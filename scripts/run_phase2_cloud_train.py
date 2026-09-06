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

Note: Heavy ML dependencies (torch, transformers, peft, datasets, IndicTransToolkit)
are provisioned remotely in the Colab VM runtime and intentionally omitted from the
local workstation to adhere to zero-heavy-local-compute project guardrails.
"""

import os
import sys
import subprocess
import time


def run_cmd(cmd, cwd=None):
    """Run a shell command, print output, return exit code (non-fatal)."""
    print(f"\n[EXEC] {cmd}")
    res = subprocess.run(cmd, shell=True, cwd=cwd)
    if res.returncode != 0:
        print(f"[WARN] Command exited with code {res.returncode}")
    return res.returncode


def run_cmd_strict(cmd, cwd=None, description=""):
    """Run a shell command and abort the pipeline on failure."""
    print(f"\n[EXEC] {cmd}")
    res = subprocess.run(cmd, shell=True, cwd=cwd)
    if res.returncode != 0:
        msg = f"[FATAL] {description or 'Command'} failed with exit code {res.returncode}: {cmd}"
        print(msg)
        raise RuntimeError(msg)
    return res.returncode


def patch_indictrans_toolkit(toolkit_dir="/content/IndicTransToolkit"):
    """
    Idempotent patch for IndicTransToolkit compatibility with transformers 4.39-4.43.
    Fixes known import breakage: `from transformers.tokenization_utils import ...`
    must become `from transformers.tokenization_utils_base import ...` in newer versions.

    CRITICAL: Uses a precise regex with word-boundary anchors to prevent the
    double-suffix bug (tokenization_utils_base -> tokenization_utils_base_base).
    """
    collator_path = os.path.join(toolkit_dir, "IndicTransToolkit", "collator.py")
    if not os.path.exists(collator_path):
        print(f"[WARN] collator.py not found at {collator_path}, skipping patch.")
        return

    with open(collator_path, "r", encoding="utf-8") as f:
        original = f.read()

    # Only patch if the OLD import exists and the NEW one does NOT
    old_import = "from transformers.tokenization_utils import"
    new_import = "from transformers.tokenization_utils_base import"

    if old_import in original and new_import not in original:
        patched = original.replace(old_import, new_import)
        with open(collator_path, "w", encoding="utf-8") as f:
            f.write(patched)
        print(f"[PATCH] Fixed collator.py: tokenization_utils -> tokenization_utils_base")
    elif new_import in original:
        print(f"[PATCH] collator.py already patched (tokenization_utils_base). Skipping.")
    else:
        print(f"[PATCH] collator.py has no known import to patch. Skipping.")


def setup_transformers_compat_shims():
    """
    Install compatibility shims for modules removed in transformers v5.x:
    - transformers.onnx (removed: OnnxConfig, OnnxSeq2SeqConfigWithPast)
    - transformers.tokenization_utils.PreTrainedTokenizerBase (moved to _base)

    These shims allow IndicTrans2 remote code to load without error.
    """
    import types
    import transformers  # type: ignore

    # Shim 1: transformers.onnx (removed in v5)
    try:
        import transformers.onnx  # type: ignore
    except (ImportError, ModuleNotFoundError):
        onnx_mod = types.ModuleType("transformers.onnx")
        onnx_mod.OnnxConfig = object  # type: ignore
        onnx_mod.OnnxSeq2SeqConfigWithPast = object  # type: ignore
        sys.modules["transformers.onnx"] = onnx_mod
        print("[SHIM] Injected transformers.onnx stub module")

    # Shim 2: transformers.tokenization_utils.PreTrainedTokenizerBase
    try:
        import transformers.tokenization_utils  # type: ignore
        from transformers.tokenization_utils_base import PreTrainedTokenizerBase  # type: ignore
        if not hasattr(transformers.tokenization_utils, "PreTrainedTokenizerBase"):
            transformers.tokenization_utils.PreTrainedTokenizerBase = PreTrainedTokenizerBase
            print("[SHIM] Injected PreTrainedTokenizerBase into transformers.tokenization_utils")
    except Exception as e:
        print(f"[SHIM WARN] tokenization_utils shim failed (non-fatal): {e}")


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


def load_indic_processor():
    """
    Robustly load IndicProcessor with multiple fallback strategies.
    Returns the IndicProcessor instance configured for training.
    """
    if "/content/IndicTransToolkit" not in sys.path:
        sys.path.insert(0, "/content/IndicTransToolkit")

    # Strategy 1: Direct import
    try:
        from IndicTransToolkit import IndicProcessor  # type: ignore
        print("[INFO] IndicProcessor loaded via direct import")
        return IndicProcessor(inference=False)
    except (ImportError, ModuleNotFoundError) as e:
        print(f"[INFO] Direct import failed ({e}), trying fallback...")

    # Strategy 2: Nested module path
    try:
        from IndicTransToolkit.IndicTransToolkit import IndicProcessor  # type: ignore
        print("[INFO] IndicProcessor loaded via nested import")
        return IndicProcessor(inference=False)
    except (ImportError, ModuleNotFoundError) as e:
        print(f"[INFO] Nested import failed ({e}), trying inline patch...")

    # Strategy 3: Inline patch and retry
    try:
        patch_indictrans_toolkit("/content/IndicTransToolkit")
        # Clear cached module imports
        mods_to_clear = [k for k in sys.modules if "IndicTransToolkit" in k]
        for m in mods_to_clear:
            del sys.modules[m]
        from IndicTransToolkit import IndicProcessor  # type: ignore
        print("[INFO] IndicProcessor loaded after inline patch")
        return IndicProcessor(inference=False)
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
    # Step 1: Install Training Dependencies
    # ──────────────────────────────────────────
    print("\n--- Step 1: Installing Cloud Dependencies ---")
    run_cmd_strict(
        "pip install -q 'transformers>=4.39.0,<4.44.0' datasets evaluate sacrebleu "
        "peft bitsandbytes accelerate sentencepiece ctranslate2 huggingface_hub "
        "sacremoses indic-nlp-library pandas",
        description="Dependency installation"
    )

    # Clone IndicTransToolkit (idempotent)
    toolkit_dir = "/content/IndicTransToolkit"
    if not os.path.exists(toolkit_dir):
        run_cmd_strict(
            f"git clone https://github.com/VarunGumma/IndicTransToolkit.git {toolkit_dir}",
            description="IndicTransToolkit clone"
        )

    # Apply idempotent Python-based patch (NOT sed — avoids double-suffix bug)
    patch_indictrans_toolkit(toolkit_dir)

    # Install IndicTransToolkit as editable package
    run_cmd_strict(
        f"pip install -q -e {toolkit_dir}",
        description="IndicTransToolkit install"
    )

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

    # Install compatibility shims before importing model code
    setup_transformers_compat_shims()

    # Authenticate with HF Hub for gated model access
    authenticate_huggingface(hf_token)

    # Load IndicProcessor with robust fallback chain
    ip = load_indic_processor()

    from transformers import AutoModelForSeq2SeqLM, AutoTokenizer, Seq2SeqTrainingArguments, Seq2SeqTrainer  # type: ignore
    from peft import LoraConfig, get_peft_model, TaskType, PeftModel  # type: ignore
    from datasets import Dataset  # type: ignore
    import pandas as pd  # type: ignore

    model_name = "ai4bharat/indictrans2-indic-indic-dist-320M"
    src_lang = "hin_Deva"
    tgt_lang = "sat_Olck"

    auth_token = hf_token if hf_token else None
    print(f"Fetching gated model with auth token: {'Yes' if auth_token else 'No'}")
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
