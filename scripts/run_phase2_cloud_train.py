# pyright: reportMissingImports=false
# -*- coding: utf-8 -*-
"""
Phase 2 Cloud Training Worker (Runs on Google Colab T4 GPU VM)
Automates the full MT pipeline on the remote Colab instance:
1. Clones/pulls Vernacular_Pedagogy repo.
2. Ingests authentic AI4Bharat BPCC & IN22 datasets via HF Token.
3. Merges with verified Grade 1-3 NIPUN Bharat pedagogical bitext.
4. Fine-tunes IndicTrans2 320M Distilled with LoRA on NVIDIA T4 GPU.
5. Merges weights and quantizes to CTranslate2 INT8 format (~65 MB).
6. Packages /content/indictrans2_sat_int8_ct2.tar.gz ready for download.

COMPATIBILITY NOTE:
  This script is designed to work with the Colab 2026 pre-installed environment:
  - transformers 5.x  (NOT downgraded — we shim for IndicTrans2 compat)
  - peft 0.20+, accelerate 1.14+, datasets 4.0+, torch 2.11+
  - Python 3.13+

  We do NOT downgrade transformers because:
  1. peft/accelerate/datasets all require transformers>=5
  2. tokenizers 0.19.x has no Python 3.13 wheels (Rust build fails)
  3. Our compatibility shims handle all IndicTrans2 v5.x breakage

Note: Heavy ML dependencies are provisioned remotely in the Colab VM runtime
and intentionally omitted from the local workstation to adhere to
zero-heavy-local-compute project guardrails.
"""

import os
import sys
import subprocess
import time


def run_cmd(cmd, cwd=None, capture=False):
    """Run a shell command, print output, return exit code (non-fatal)."""
    print(f"\n[EXEC] {cmd}")
    if capture:
        res = subprocess.run(cmd, shell=True, cwd=cwd,
                             capture_output=True, text=True)
        if res.stdout:
            print(res.stdout[-2000:])
        if res.stderr:
            print(res.stderr[-2000:])
    else:
        res = subprocess.run(cmd, shell=True, cwd=cwd)
    if res.returncode != 0:
        print(f"[WARN] Command exited with code {res.returncode}")
    return res.returncode


def run_cmd_strict(cmd, cwd=None, description="", capture=False):
    """Run a shell command and abort the pipeline on failure."""
    print(f"\n[EXEC] {cmd}")
    if capture:
        res = subprocess.run(cmd, shell=True, cwd=cwd,
                             capture_output=True, text=True)
        if res.stdout:
            print(res.stdout[-2000:])
        if res.stderr:
            print(res.stderr[-2000:])
    else:
        res = subprocess.run(cmd, shell=True, cwd=cwd)
    if res.returncode != 0:
        msg = f"[FATAL] {description or 'Command'} failed with exit code {res.returncode}: {cmd}"
        print(msg)
        raise RuntimeError(msg)
    return res.returncode


# ═══════════════════════════════════════════════════════════════
# DEPENDENCY MANAGEMENT — Work WITH the Colab environment
# ═══════════════════════════════════════════════════════════════

def install_dependencies():
    """
    Smart dependency installer that works WITH Colab's pre-installed environment.

    Colab 2026 ships with: transformers 5.16+, peft 0.20+, accelerate 1.14+,
    datasets 4.0+, torch 2.11+, ctranslate2 4.8+, sentencepiece, bitsandbytes.

    Strategy:
      1. Keep ALL pre-installed packages as-is (no downgrades)
      2. Only install genuinely missing packages
      3. Use verbose output so failures are diagnosable
    """
    print("\n--- Step 1: Installing Cloud Dependencies ---")

    # Check what's already installed
    preinstalled = {}
    for pkg_name, import_name in [
        ("transformers", "transformers"),
        ("peft", "peft"),
        ("accelerate", "accelerate"),
        ("datasets", "datasets"),
        ("torch", "torch"),
        ("ctranslate2", "ctranslate2"),
        ("sentencepiece", "sentencepiece"),
        ("bitsandbytes", "bitsandbytes"),
        ("evaluate", "evaluate"),
        ("sacrebleu", "sacrebleu"),
        ("sacremoses", "sacremoses"),
        ("pandas", "pandas"),
        ("huggingface_hub", "huggingface_hub"),
    ]:
        try:
            mod = __import__(import_name)
            ver = getattr(mod, "__version__", "?")
            preinstalled[pkg_name] = ver
        except ImportError:
            preinstalled[pkg_name] = None

    print("\nPre-installed packages:")
    for pkg, ver in preinstalled.items():
        status = f"  ✓ {pkg}: {ver}" if ver else f"  ✗ {pkg}: MISSING"
        print(status)

    # Only install what's actually missing
    missing = [pkg for pkg, ver in preinstalled.items() if ver is None]

    # Always ensure indic-nlp-library (import name differs from pip name)
    try:
        import indicnlp  # type: ignore
    except ImportError:
        missing.append("indic-nlp-library")

    if missing:
        missing_str = " ".join(missing)
        print(f"\nInstalling missing packages: {missing_str}")
        run_cmd_strict(
            f"pip install {missing_str}",
            description=f"Install missing packages: {missing_str}"
        )
    else:
        print("\n[OK] All required packages are already installed!")


# ═══════════════════════════════════════════════════════════════
# INDICTRANSTOOLKIT COMPATIBILITY
# ═══════════════════════════════════════════════════════════════

def patch_indictrans_toolkit(toolkit_dir="/content/IndicTransToolkit"):
    """
    Comprehensive, idempotent patch for IndicTransToolkit compatibility
    with transformers 5.x on Colab 2026.

    Fixes ALL known import breakages in collator.py:
      1. `from transformers.tokenization_utils import` →
         `from transformers.tokenization_utils_base import`
      2. `from transformers.tokenization_utils_base_base import` →
         `from transformers.tokenization_utils_base import`
         (repairs the double-suffix bug from previous sed corruption)
      3. `from transformers.data.data_collator import pad_without_fast_tokenizer_warning`
         (may have been moved/removed in v5)

    CRITICAL: This function is fully idempotent — running it multiple times
    on already-patched files produces no changes.
    """
    collator_path = os.path.join(toolkit_dir, "IndicTransToolkit", "collator.py")
    if not os.path.exists(collator_path):
        print(f"[WARN] collator.py not found at {collator_path}, skipping patch.")
        return

    with open(collator_path, "r", encoding="utf-8") as f:
        content = f.read()

    original_content = content
    patches_applied = []

    # Patch 1: Fix the _base_base double-suffix corruption from previous sed runs
    bad_import = "from transformers.tokenization_utils_base_base import"
    correct_import = "from transformers.tokenization_utils_base import"
    if bad_import in content:
        content = content.replace(bad_import, correct_import)
        patches_applied.append("Fixed _base_base double-suffix corruption")

    # Patch 2: Fix the original tokenization_utils → tokenization_utils_base
    old_import = "from transformers.tokenization_utils import"
    if old_import in content and correct_import not in content:
        content = content.replace(old_import, correct_import)
        patches_applied.append("Fixed tokenization_utils → tokenization_utils_base")

    # Patch 3: Handle pad_without_fast_tokenizer_warning (moved/removed in v5)
    pad_import = "from transformers.data.data_collator import pad_without_fast_tokenizer_warning"
    if pad_import in content:
        # Check if the function still exists
        try:
            from transformers.data.data_collator import pad_without_fast_tokenizer_warning  # type: ignore
            # Function exists, no patch needed
        except ImportError:
            # Function was removed/moved — provide a no-op fallback
            replacement = (
                "try:\n"
                "        from transformers.data.data_collator import pad_without_fast_tokenizer_warning\n"
                "    except ImportError:\n"
                "        # Removed in transformers v5.x — use inline fallback\n"
                "        def pad_without_fast_tokenizer_warning(tokenizer, *args, **kwargs):\n"
                "            return tokenizer.pad(*args, **kwargs)"
            )
            content = content.replace(
                "    " + pad_import,
                "    " + replacement
            )
            patches_applied.append("Added pad_without_fast_tokenizer_warning fallback")

    # Write back only if changes were made
    if content != original_content:
        with open(collator_path, "w", encoding="utf-8") as f:
            f.write(content)
        for p in patches_applied:
            print(f"[PATCH] {p}")
    else:
        print("[PATCH] collator.py is already correctly patched. No changes needed.")


def setup_indictrans_toolkit(toolkit_dir="/content/IndicTransToolkit"):
    """
    Full setup of IndicTransToolkit: clone, patch, install.
    Handles fresh installs AND recovery from previously corrupted state.
    """
    print("\n--- Setting up IndicTransToolkit ---")

    # Clone if not present
    if not os.path.exists(toolkit_dir):
        run_cmd_strict(
            f"git clone https://github.com/VarunGumma/IndicTransToolkit.git {toolkit_dir}",
            description="IndicTransToolkit clone"
        )
    else:
        print(f"[OK] IndicTransToolkit already cloned at {toolkit_dir}")
        # Reset to clean state if previously corrupted
        collator_path = os.path.join(toolkit_dir, "IndicTransToolkit", "collator.py")
        if os.path.exists(collator_path):
            with open(collator_path, "r") as f:
                content = f.read()
            if "tokenization_utils_base_base" in content:
                print("[RECOVERY] Detected corrupted collator.py — resetting from git...")
                run_cmd(f"cd {toolkit_dir} && git checkout -- IndicTransToolkit/collator.py")

    # Apply comprehensive idempotent patches
    patch_indictrans_toolkit(toolkit_dir)

    # Install as editable package
    run_cmd_strict(
        f"pip install -q -e {toolkit_dir}",
        description="IndicTransToolkit install"
    )


# ═══════════════════════════════════════════════════════════════
# TRANSFORMERS v5.x COMPATIBILITY SHIMS
# ═══════════════════════════════════════════════════════════════

def setup_transformers_compat_shims():
    """
    Install compatibility shims for IndicTrans2 model remote code
    that uses APIs removed/moved in transformers v5.x:

    Shim 1: transformers.onnx (removed in v5)
      - The model's configuration_indictrans.py imports OnnxConfig and
        OnnxSeq2SeqConfigWithPast from transformers.onnx
    Shim 2: transformers.onnx.utils (removed in v5)
      - compute_effective_axis_dimension utility
    Shim 3: transformers.tokenization_utils.PreTrainedTokenizerBase
      - Moved to transformers.tokenization_utils_base in newer versions
    """
    import types
    import transformers  # type: ignore

    transformers_version = getattr(transformers, "__version__", "0.0.0")
    major_ver = int(transformers_version.split(".")[0])
    print(f"[INFO] transformers version: {transformers_version} (major={major_ver})")

    if major_ver >= 5:
        print("[INFO] Applying transformers v5.x compatibility shims...")

        # Shim 1: transformers.onnx module
        if "transformers.onnx" not in sys.modules:
            try:
                import transformers.onnx  # type: ignore
            except (ImportError, ModuleNotFoundError):
                onnx_mod = types.ModuleType("transformers.onnx")
                onnx_mod.__package__ = "transformers.onnx"
                onnx_mod.OnnxConfig = object  # type: ignore
                onnx_mod.OnnxSeq2SeqConfigWithPast = object  # type: ignore
                sys.modules["transformers.onnx"] = onnx_mod
                print("  [SHIM] transformers.onnx stub injected")

        # Shim 2: transformers.onnx.utils
        if "transformers.onnx.utils" not in sys.modules:
            onnx_utils_mod = types.ModuleType("transformers.onnx.utils")
            onnx_utils_mod.__package__ = "transformers.onnx"
            onnx_utils_mod.compute_effective_axis_dimension = lambda *a, **kw: 0  # type: ignore
            sys.modules["transformers.onnx.utils"] = onnx_utils_mod
            print("  [SHIM] transformers.onnx.utils stub injected")

        # Shim 3: tokenization_utils.PreTrainedTokenizerBase
        try:
            import transformers.tokenization_utils  # type: ignore
            from transformers.tokenization_utils_base import PreTrainedTokenizerBase  # type: ignore
            if not hasattr(transformers.tokenization_utils, "PreTrainedTokenizerBase"):
                transformers.tokenization_utils.PreTrainedTokenizerBase = PreTrainedTokenizerBase
                print("  [SHIM] PreTrainedTokenizerBase bridged to tokenization_utils")
        except Exception as e:
            print(f"  [SHIM WARN] tokenization_utils bridge failed (non-fatal): {e}")

    else:
        print("[INFO] transformers v4.x detected — no shims needed.")


def patch_remote_tokenizer(model_name, auth_token=None):
    """
    Patch the IndicTrans2 remote tokenizer code for transformers v5.x compatibility.

    ROOT CAUSE: In transformers v5.x, PreTrainedTokenizerBase.__setattr__() requires
    `_special_tokens_map` to be initialized (done by super().__init__()). But the
    IndicTrans2 tokenizer sets `self.unk_token = ...` BEFORE calling super().__init__(),
    causing: AttributeError: IndicTransTokenizer has no attribute _special_tokens_map

    FIX: Rewrite the __init__ method to store special tokens in temporary variables
    first, then set them properly AFTER super().__init__() is called.
    """
    import transformers  # type: ignore
    major_ver = int(getattr(transformers, "__version__", "0").split(".")[0])
    if major_ver < 5:
        return  # Only needed for v5+

    # First, trigger the download of remote code by attempting a config load
    # This ensures the tokenizer file is cached locally
    try:
        from transformers import AutoConfig  # type: ignore
        AutoConfig.from_pretrained(model_name, trust_remote_code=True, token=auth_token)
    except Exception:
        pass  # Config load may fail but files should still be cached

    # Find the cached tokenizer file
    cache_base = os.path.expanduser("~/.cache/huggingface/modules/transformers_modules")
    tokenizer_path = None
    for root, dirs, files in os.walk(cache_base):
        for f in files:
            if f == "tokenization_indictrans.py":
                tokenizer_path = os.path.join(root, f)
                break
        if tokenizer_path:
            break

    if not tokenizer_path:
        print("[PATCH] Remote tokenizer not yet cached — will retry after first load attempt.")
        return

    with open(tokenizer_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Check if already patched (look for our marker)
    if "# PATCHED_FOR_TRANSFORMERS_V5" in content:
        print("[PATCH] Remote tokenizer already patched for v5.x. Skipping.")
        return

    # Check if the problematic pattern exists
    if "self.unk_token = (" not in content or "super().__init__(" not in content:
        print("[PATCH] Remote tokenizer has unexpected structure. Skipping auto-patch.")
        return

    # Strategy: Replace the __init__ body to move super().__init__() BEFORE
    # any self.XXX_token = ... assignments.
    #
    # The fix: store special token strings in local variables, call super().__init__()
    # first (which initializes _special_tokens_map), then set attributes.

    old_init_body = '''        self.src_vocab_fp = src_vocab_fp
        self.tgt_vocab_fp = tgt_vocab_fp
        self.src_spm_fp = src_spm_fp
        self.tgt_spm_fp = tgt_spm_fp

        # Store token content directly instead of accessing .content
        self.unk_token = (
            hasattr(unk_token, "content") and unk_token.content or unk_token
        )
        self.pad_token = (
            hasattr(pad_token, "content") and pad_token.content or pad_token
        )
        self.eos_token = (
            hasattr(eos_token, "content") and eos_token.content or eos_token
        )
        self.bos_token = (
            hasattr(bos_token, "content") and bos_token.content or bos_token
        )

        # Load vocabularies
        self.src_encoder = self._load_json(self.src_vocab_fp)
        self.tgt_encoder = self._load_json(self.tgt_vocab_fp)

        # Validate tokens
        if self.unk_token not in self.src_encoder:
            raise KeyError("<unk> token must be in vocab")
        if self.pad_token not in self.src_encoder:
            raise KeyError("<pad> token must be in vocab")

        # Pre-compute reverse mappings
        self.src_decoder = {v: k for k, v in self.src_encoder.items()}
        self.tgt_decoder = {v: k for k, v in self.tgt_encoder.items()}

        # Load SPM models
        self.src_spm = self._load_spm(self.src_spm_fp)
        self.tgt_spm = self._load_spm(self.tgt_spm_fp)

        # Initialize current settings
        self._switch_to_input_mode()

        # Cache token IDs
        self.unk_token_id = self.src_encoder[self.unk_token]
        self.pad_token_id = self.src_encoder[self.pad_token]
        self.eos_token_id = self.src_encoder[self.eos_token]
        self.bos_token_id = self.src_encoder[self.bos_token]

        super().__init__(
            src_vocab_file=self.src_vocab_fp,
            tgt_vocab_file=self.tgt_vocab_fp,
            do_lower_case=do_lower_case,
            unk_token=unk_token,
            bos_token=bos_token,
            eos_token=eos_token,
            pad_token=pad_token,
            **kwargs,
        )'''

    new_init_body = '''        # PATCHED_FOR_TRANSFORMERS_V5: Reordered __init__ to call super().__init__()
        # BEFORE setting special token attributes, which is required by
        # transformers v5.x (PreTrainedTokenizerBase.__setattr__ needs
        # _special_tokens_map to be initialized first).
        self.src_vocab_fp = src_vocab_fp
        self.tgt_vocab_fp = tgt_vocab_fp
        self.src_spm_fp = src_spm_fp
        self.tgt_spm_fp = tgt_spm_fp

        # Resolve token strings from AddedToken objects if needed
        _unk = hasattr(unk_token, "content") and unk_token.content or (unk_token if isinstance(unk_token, str) else str(unk_token))
        _pad = hasattr(pad_token, "content") and pad_token.content or (pad_token if isinstance(pad_token, str) else str(pad_token))
        _eos = hasattr(eos_token, "content") and eos_token.content or (eos_token if isinstance(eos_token, str) else str(eos_token))
        _bos = hasattr(bos_token, "content") and bos_token.content or (bos_token if isinstance(bos_token, str) else str(bos_token))

        # Load vocabularies (needed before super().__init__ for token ID lookups)
        self.src_encoder = self._load_json(self.src_vocab_fp)
        self.tgt_encoder = self._load_json(self.tgt_vocab_fp)

        # Validate tokens
        if _unk not in self.src_encoder:
            raise KeyError("<unk> token must be in vocab")
        if _pad not in self.src_encoder:
            raise KeyError("<pad> token must be in vocab")

        # Pre-compute reverse mappings
        self.src_decoder = {v: k for k, v in self.src_encoder.items()}
        self.tgt_decoder = {v: k for k, v in self.tgt_encoder.items()}

        # Load SPM models
        self.src_spm = self._load_spm(self.src_spm_fp)
        self.tgt_spm = self._load_spm(self.tgt_spm_fp)

        # Initialize current settings
        self._switch_to_input_mode()

        # Call super().__init__() FIRST — this initializes _special_tokens_map
        super().__init__(
            src_vocab_file=self.src_vocab_fp,
            tgt_vocab_file=self.tgt_vocab_fp,
            do_lower_case=do_lower_case,
            unk_token=_unk,
            bos_token=_bos,
            eos_token=_eos,
            pad_token=_pad,
            **kwargs,
        )

        # Cache token IDs (after super().__init__ sets up token infrastructure)
        self.unk_token_id = self.src_encoder.get(_unk, 0)
        self.pad_token_id = self.src_encoder.get(_pad, 1)
        self.eos_token_id = self.src_encoder.get(_eos, 2)
        self.bos_token_id = self.src_encoder.get(_bos, 0)'''

    if old_init_body in content:
        content = content.replace(old_init_body, new_init_body)
        with open(tokenizer_path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"[PATCH] Remote tokenizer patched for transformers v5.x: {tokenizer_path}")
    else:
        print("[PATCH WARN] Could not find exact __init__ pattern in remote tokenizer.")
        print("  Attempting fallback: monkey-patching __setattr__...")
        # Fallback: inject a compatibility wrapper at import time
        _inject_tokenizer_compat_shim()


def _inject_tokenizer_compat_shim():
    """
    Fallback shim: If we can't patch the source file, monkey-patch
    PreTrainedTokenizerBase.__setattr__ to gracefully handle missing
    _special_tokens_map during __init__.
    """
    from transformers.tokenization_utils_base import PreTrainedTokenizerBase  # type: ignore

    original_setattr = PreTrainedTokenizerBase.__setattr__

    def safe_setattr(self, key, value):
        try:
            original_setattr(self, key, value)
        except AttributeError as e:
            if "_special_tokens_map" in str(e):
                # Initialize _special_tokens_map if it doesn't exist yet
                object.__setattr__(self, "_special_tokens_map", {})
                original_setattr(self, key, value)
            else:
                raise

    PreTrainedTokenizerBase.__setattr__ = safe_setattr
    print("  [SHIM] Injected safe __setattr__ for PreTrainedTokenizerBase")


# ═══════════════════════════════════════════════════════════════
# AUTHENTICATION & DATA LOADING
# ═══════════════════════════════════════════════════════════════

def get_hf_token():
    """Retrieve HF token from environment or persisted file."""
    token = os.environ.get("HF_TOKEN", "")
    if token:
        return token.strip()
    token_file = "/content/.hf_token"
    if os.path.exists(token_file):
        try:
            with open(token_file, "r") as f:
                token = f.read().strip()
                if token:
                    return token
        except Exception:
            pass
    return ""


def authenticate_huggingface(hf_token):
    """Authenticate with Hugging Face Hub using the provided token."""
    if not hf_token:
        print("[WARN] No HF_TOKEN provided. Gated model access may fail.")
        return
    try:
        from huggingface_hub import login  # type: ignore
        login(token=hf_token, add_to_git_credential=False)
        print("[INFO] Authenticated with Hugging Face Hub successfully!")
    except Exception as e:
        print(f"[WARN] Hugging Face Hub login warning: {e}")


def load_indic_processor():
    """
    Robustly load IndicProcessor with multiple fallback strategies.
    Returns the IndicProcessor instance configured for training.
    """
    toolkit_dir = "/content/IndicTransToolkit"
    if toolkit_dir not in sys.path:
        sys.path.insert(0, toolkit_dir)

    # Strategy 1: Direct import (works when pip install -e succeeded)
    try:
        from IndicTransToolkit import IndicProcessor  # type: ignore
        ip = IndicProcessor(inference=False)
        print("[OK] IndicProcessor loaded via direct import")
        return ip
    except (ImportError, ModuleNotFoundError) as e:
        print(f"[INFO] Direct import failed ({e}), trying fallback...")

    # Strategy 2: Clear stale module cache, re-patch, retry
    print("[INFO] Clearing module cache and re-patching...")
    mods_to_clear = [k for k in list(sys.modules.keys()) if "IndicTransToolkit" in k]
    for m in mods_to_clear:
        del sys.modules[m]

    patch_indictrans_toolkit(toolkit_dir)

    try:
        from IndicTransToolkit import IndicProcessor  # type: ignore
        ip = IndicProcessor(inference=False)
        print("[OK] IndicProcessor loaded after re-patching")
        return ip
    except (ImportError, ModuleNotFoundError) as e:
        print(f"[INFO] Re-patched import failed ({e}), trying nested path...")

    # Strategy 3: Nested module path
    try:
        from IndicTransToolkit.IndicTransToolkit import IndicProcessor  # type: ignore
        ip = IndicProcessor(inference=False)
        print("[OK] IndicProcessor loaded via nested import")
        return ip
    except Exception as e:
        raise RuntimeError(
            f"[FATAL] Cannot import IndicProcessor after all strategies. "
            f"Check IndicTransToolkit installation and transformers version. Error: {e}"
        )


def validate_tsv_file(path, min_rows=5):
    """Validate that a TSV file exists and has the expected minimum rows."""
    if not os.path.exists(path):
        raise FileNotFoundError(
            f"[FATAL] Required dataset file not found: {path}\n"
            f"Ensure 01_fetch_bpcc_bitext.py and 03_bitext_normalizer.py ran successfully."
        )
    import pandas as pd  # type: ignore
    df = pd.read_csv(path, sep="\t", nrows=min_rows + 1)
    if len(df) < min_rows:
        raise ValueError(
            f"[FATAL] Dataset file {path} has only {len(df)} rows (minimum {min_rows} expected)."
        )
    required_cols = {"source", "target"}
    if not required_cols.issubset(set(df.columns)):
        raise ValueError(
            f"[FATAL] Dataset file {path} missing required columns. "
            f"Expected {required_cols}, got {set(df.columns)}."
        )
    print(f"[OK] Validated {path}: columns={list(df.columns)}, rows>={min_rows}")
    return True


# ═══════════════════════════════════════════════════════════════
# MAIN PIPELINE
# ═══════════════════════════════════════════════════════════════

def main():
    print("=" * 60)
    print("PHASE 2: IndicTrans2 LoRA Cloud Training & CTranslate2 INT8")
    print("=" * 60)

    # ──────────────────────────────────────────
    # Check GPU
    # ──────────────────────────────────────────
    import torch  # type: ignore
    print(f"CUDA Available: {torch.cuda.is_available()}")
    if torch.cuda.is_available():
        print(f"GPU Device:     {torch.cuda.get_device_name(0)}")
        print(f"Total Memory:   {torch.cuda.get_device_properties(0).total_memory / (1024**3):.2f} GB")
    else:
        print("[WARNING] CUDA not detected. Training will be extremely slow on CPU.")

    # ──────────────────────────────────────────
    # Step 1: Smart Dependency Installation
    # ──────────────────────────────────────────
    install_dependencies()

    # ──────────────────────────────────────────
    # Step 1b: Setup IndicTransToolkit
    # ──────────────────────────────────────────
    setup_indictrans_toolkit()

    # ──────────────────────────────────────────
    # Step 1c: Install Compatibility Shims (BEFORE any model imports)
    # ──────────────────────────────────────────
    setup_transformers_compat_shims()

    # ──────────────────────────────────────────
    # Step 2: Clone/Update Repository
    # ──────────────────────────────────────────
    print("\n--- Step 2: Syncing Repository ---")
    repo_dir = "/content/Vernacular_Pedagogy"
    if not os.path.exists(repo_dir):
        run_cmd_strict(
            f"git clone https://github.com/AshrafGalaxy/Vernacular_Pedagogy.git {repo_dir}",
            description="Repository clone"
        )
    else:
        run_cmd("git pull origin main", cwd=repo_dir)

    # ──────────────────────────────────────────
    # Step 3: Ingest Authentic Bitext Datasets
    # ──────────────────────────────────────────
    print("\n--- Step 3: Fetching Authentic AI4Bharat BPCC & IN22 Data ---")
    hf_token = get_hf_token()
    token_arg = f"--hf-token {hf_token}" if hf_token else ""
    # Data ingestion is best-effort (may fail if BPCC terms not accepted)
    run_cmd(f"python scripts/01_fetch_bpcc_bitext.py {token_arg}", cwd=repo_dir)
    # Normalizer is critical — it generates train/val splits
    run_cmd_strict(
        "python scripts/03_bitext_normalizer.py",
        cwd=repo_dir,
        description="Bitext normalization & train/val split generation"
    )

    # ──────────────────────────────────────────
    # Step 4: Load Models & Tokenizer
    # ──────────────────────────────────────────
    print("\n--- Step 4: Loading IndicTrans2 Base Model ---")

    # Authenticate with HF Hub for gated model access
    authenticate_huggingface(hf_token)

    # Load IndicProcessor with robust fallback chain
    ip = load_indic_processor()

    from transformers import AutoModelForSeq2SeqLM, AutoTokenizer  # type: ignore
    from transformers import Seq2SeqTrainingArguments, Seq2SeqTrainer  # type: ignore
    from peft import LoraConfig, get_peft_model, TaskType, PeftModel  # type: ignore
    from datasets import Dataset  # type: ignore
    import pandas as pd  # type: ignore

    model_name = "ai4bharat/indictrans2-indic-indic-dist-320M"
    src_lang = "hin_Deva"
    tgt_lang = "sat_Olck"

    auth_token = hf_token if hf_token else None
    print(f"Fetching gated model with auth token: {'Yes' if auth_token else 'No'}")

    # Patch remote tokenizer for transformers v5.x compatibility
    patch_remote_tokenizer(model_name, auth_token=auth_token)

    # Load tokenizer with retry: if first attempt fails due to cached stale
    # code, clear module cache and retry after patching
    try:
        tokenizer = AutoTokenizer.from_pretrained(model_name, trust_remote_code=True, token=auth_token)
    except (AttributeError, TypeError) as e:
        print(f"[WARN] Tokenizer load failed ({e}), applying fallback shim and retrying...")
        _inject_tokenizer_compat_shim()
        # Clear any cached modules from the failed attempt
        mods_to_clear = [k for k in list(sys.modules.keys())
                         if "indictrans" in k.lower() or "tokenization_indictrans" in k.lower()]
        for m in mods_to_clear:
            del sys.modules[m]
        tokenizer = AutoTokenizer.from_pretrained(model_name, trust_remote_code=True, token=auth_token)

    base_model = AutoModelForSeq2SeqLM.from_pretrained(
        model_name,
        trust_remote_code=True,
        torch_dtype=torch.float16,
        device_map="auto",
        token=auth_token
    )
    print(f"[OK] Model loaded: {model_name}")

    # ──────────────────────────────────────────
    # Step 5: Setup LoRA
    # ──────────────────────────────────────────
    print("\n--- Step 5: Configuring LoRA Adapter ---")
    lora_config = LoraConfig(
        task_type=TaskType.SEQ_2_SEQ_LM,
        r=16,
        lora_alpha=32,
        lora_dropout=0.05,
        target_modules=["q_proj", "v_proj", "k_proj", "out_proj"],
        bias="none"
    )
    peft_model = get_peft_model(base_model, lora_config)
    peft_model.print_trainable_parameters()

    # ──────────────────────────────────────────
    # Step 6: Load & Tokenize Datasets
    # ──────────────────────────────────────────
    print("\n--- Step 6: Tokenizing Bitext ---")
    train_tsv = os.path.join(repo_dir, "data", "processed", "bitext", "train.tsv")
    val_tsv = os.path.join(repo_dir, "data", "processed", "bitext", "val.tsv")

    # Validate dataset files before proceeding
    validate_tsv_file(train_tsv, min_rows=10)
    validate_tsv_file(val_tsv, min_rows=2)

    def load_ds(path):
        df = pd.read_csv(path, sep="\t")
        # Drop any rows with NaN values in source/target columns
        df = df.dropna(subset=["source", "target"])
        src = df["source"].astype(str).tolist()
        tgt = df["target"].astype(str).tolist()
        if not src:
            raise ValueError(f"[FATAL] No valid rows found in {path} after cleaning.")
        prepped = ip.preprocess_batch(src, src_lang=src_lang, tgt_lang=tgt_lang)
        return Dataset.from_dict({"source": prepped, "target": tgt})

    train_ds = load_ds(train_tsv)
    val_ds = load_ds(val_tsv)

    def tokenize_fn(examples):
        inputs = tokenizer(examples["source"], max_length=128, truncation=True, padding="max_length")
        labels = tokenizer(text_target=examples["target"], max_length=128, truncation=True, padding="max_length")
        labels["input_ids"] = [
            [(l if l != tokenizer.pad_token_id else -100) for l in label]
            for label in labels["input_ids"]
        ]
        inputs["labels"] = labels["input_ids"]
        return inputs

    tok_train = train_ds.map(tokenize_fn, batched=True, remove_columns=["source", "target"])
    tok_val = val_ds.map(tokenize_fn, batched=True, remove_columns=["source", "target"])
    print(f"Samples: Train={len(tok_train)}, Val={len(tok_val)}")

    # ──────────────────────────────────────────
    # Step 7: Fine-Tune Model
    # ──────────────────────────────────────────
    print("\n--- Step 7: Starting GPU LoRA Fine-Tuning ---")
    out_lora = "/content/indictrans2_sat_lora"

    # Handle API differences between transformers versions
    # v5.x uses eval_strategy, v4.x uses evaluation_strategy
    try:
        training_args = Seq2SeqTrainingArguments(
            output_dir=out_lora,
            per_device_train_batch_size=8,
            per_device_eval_batch_size=8,
            gradient_accumulation_steps=2,
            learning_rate=3e-4,
            num_train_epochs=3,
            fp16=torch.cuda.is_available(),
            eval_strategy="epoch",
            save_strategy="epoch",
            save_total_limit=1,
            logging_steps=20,
            report_to="none"
        )
    except TypeError:
        # Older transformers versions use evaluation_strategy
        training_args = Seq2SeqTrainingArguments(
            output_dir=out_lora,
            per_device_train_batch_size=8,
            per_device_eval_batch_size=8,
            gradient_accumulation_steps=2,
            learning_rate=3e-4,
            num_train_epochs=3,
            fp16=torch.cuda.is_available(),
            evaluation_strategy="epoch",
            save_strategy="epoch",
            save_total_limit=1,
            logging_steps=20,
            report_to="none"
        )

    trainer = Seq2SeqTrainer(
        model=peft_model,
        args=training_args,
        train_dataset=tok_train,
        eval_dataset=tok_val,
        tokenizer=tokenizer
    )

    t0 = time.time()
    trainer.train()
    elapsed_min = (time.time() - t0) / 60
    print(f"Training completed in {elapsed_min:.1f} minutes!")
    lora_final_path = "/content/indictrans2_sat_lora_final"
    trainer.save_model(lora_final_path)
    print(f"[OK] LoRA adapter saved to {lora_final_path}")

    # ──────────────────────────────────────────
    # Step 8: Merge LoRA Weights
    # ──────────────────────────────────────────
    print("\n--- Step 8: Merging LoRA Weights with Base Model ---")
    raw_base = AutoModelForSeq2SeqLM.from_pretrained(model_name, trust_remote_code=True, token=auth_token)
    merged = PeftModel.from_pretrained(raw_base, lora_final_path)
    merged = merged.merge_and_unload()
    merged_path = "/content/indictrans2_sat_merged"
    merged.save_pretrained(merged_path)
    tokenizer.save_pretrained(merged_path)
    print(f"[OK] Merged model saved to: {merged_path}")

    # ──────────────────────────────────────────
    # Step 9: Quantize to CTranslate2 INT8
    # ──────────────────────────────────────────
    print("\n--- Step 9: Quantizing to CTranslate2 INT8 ---")
    ct2_path = "/content/indictrans2_sat_int8_ct2"
    run_cmd_strict(
        f"python -m ctranslate2.converters.transformers "
        f"--model {merged_path} --output_dir {ct2_path} "
        f"--quantization int8 --trust_remote_code --low_cpu_mem_usage",
        description="CTranslate2 INT8 quantization"
    )

    # ──────────────────────────────────────────
    # Step 10: Package Model Artifact
    # ──────────────────────────────────────────
    print("\n--- Step 10: Packaging INT8 Model Artifact ---")
    tar_path = "/content/indictrans2_sat_int8_ct2.tar.gz"
    run_cmd_strict(
        f"cd /content && tar -czf {tar_path} indictrans2_sat_int8_ct2/",
        description="Model packaging"
    )
    if os.path.exists(tar_path):
        size_mb = os.path.getsize(tar_path) / (1024 * 1024)
        print(f"\n{'=' * 60}")
        print(f"[PHASE 2 SUCCESS] Final INT8 Model Package Ready!")
        print(f"  Path: {tar_path}")
        print(f"  Size: {size_mb:.1f} MB")
        print(f"  Training Time: {elapsed_min:.1f} minutes")
        print(f"{'=' * 60}")
    else:
        raise RuntimeError(f"[FATAL] Expected package not found at {tar_path}")


if __name__ == "__main__":
    main()
