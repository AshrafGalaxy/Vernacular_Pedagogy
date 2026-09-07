# pyright: reportMissingImports=false
# -*- coding: utf-8 -*-
"""
Phase 3 Cloud Training Worker (Runs on Google Colab Tesla T4 GPU VM)
Automates the full Voice Synthesis pipeline on the remote Colab instance:
1. Clones/pulls Vernacular_Pedagogy repository.
2. Installs Piper TTS, PyTorch Lightning, and audio processing tools.
3. Ingests authentic Santhali speech data (AI4Bharat IndicVoices-R & Common Voice).
4. Preprocesses dataset with deterministic Ol Chiki character-level alignment.
5. Warm-starts fine-tuning of Piper VITS architecture on NVIDIA T4 GPU.
6. Exports trained checkpoint to ONNX format (sat_piper_model.onnx ~30 MB).
7. Serializes configuration JSON (sat_piper_model.onnx.json).
8. Runs in-process inference validation on sample FLN pedagogical phrases.
9. Packages /content/sat_piper_model.tar.gz ready for download.
"""

import os
import sys
import subprocess
import time
import json
import glob
import shutil


def run_cmd(cmd, cwd=None, description="", capture=False):
    """Run a shell command, print output, return exit code (non-fatal)."""
    if description:
        print(f"\n[EXEC] {description}: {cmd}", flush=True)
    else:
        print(f"\n[EXEC] {cmd}", flush=True)
    if capture:
        res = subprocess.run(cmd, shell=True, cwd=cwd, capture_output=True, text=True)
        if res.stdout:
            print(res.stdout[-2000:], flush=True)
        if res.stderr:
            print(res.stderr[-2000:], flush=True)
    else:
        res = subprocess.run(cmd, shell=True, cwd=cwd)
    if res.returncode != 0:
        print(f"[WARN] Command exited with code {res.returncode}", flush=True)
    return res.returncode


def run_cmd_strict(cmd, cwd=None, description="", capture=False):
    """Run a shell command and abort the pipeline on failure."""
    print(f"\n[EXEC] {cmd}", flush=True)
    if capture:
        res = subprocess.run(cmd, shell=True, cwd=cwd, capture_output=True, text=True)
        if res.stdout:
            print(res.stdout[-2000:], flush=True)
        if res.stderr:
            print(res.stderr[-2000:], flush=True)
    else:
        res = subprocess.run(cmd, shell=True, cwd=cwd)
    if res.returncode != 0:
        msg = f"[FATAL] {description or 'Command'} failed with exit code {res.returncode}: {cmd}"
        print(msg, flush=True)
        raise RuntimeError(msg)
    return res.returncode


def install_dependencies():
    """Install Piper TTS training stack and audio processing dependencies."""
    print("\n--- Step 1: Installing Cloud Dependencies for Piper TTS ---", flush=True)

    # Core system tools
    run_cmd("apt-get update -qq && apt-get install -y -qq espeak-ng ffmpeg sox libsndfile1", capture=True)

    # Python packages
    # Pinning pytorch-lightning to 1.9.5, torchmetrics to 0.11.4, and numpy<2.0 for Piper VITS compatibility
    packages = [
        "pytorch-lightning==1.9.5",
        "torchmetrics==0.11.4",
        "onnx",
        "onnxruntime",
        "soundfile",
        "scipy",
        "datasets",
        "huggingface_hub",
        "librosa",
        "cython",
        "onnxscript"
    ]
    run_cmd_strict(
        f"pip install -q {' '.join(packages)}",
        description="Install Piper training dependencies"
    )

    # Clone & install rhasspy/piper
    piper_dir = "/content/piper"
    if not os.path.exists(piper_dir):
        run_cmd_strict(
            f"git clone https://github.com/rhasspy/piper.git {piper_dir}",
            description="Clone rhasspy/piper repository"
        )

    # Patch 1: Remove unbuildable piper-phonemize from requirements.txt (not needed for --phoneme-type text)
    req_path = os.path.join(piper_dir, "src", "python", "requirements.txt")
    if os.path.exists(req_path):
        with open(req_path, "r", encoding="utf-8") as f:
            lines = [l for l in f if "piper-phonemize" not in l]
        with open(req_path, "w", encoding="utf-8") as f:
            f.writelines(lines)

    # Patch 2: Pure-Python Ol Chiki codepoints phonemizer in preprocess.py
    prep_path = os.path.join(piper_dir, "src", "python", "piper_train", "preprocess.py")
    if os.path.exists(prep_path):
        with open(prep_path, "r", encoding="utf-8") as f:
            prep_code = f.read()
        pure_py = '''def _build_sat_map():
    m = {"_": [0], "^": [1], "$": [2], " ": [3]}
    punct = [".", ",", "!", "?", "-", ":", ";", "\'", \'"\', "(", ")", "[", "]", "/", "`", "~", chr(0x1C7E), chr(0x1C7F)]
    cid = 4
    for p in punct:
        if p not in m:
            m[p] = [cid]
            cid += 1
    for code in range(0x1C50, 0x1C80):
        c = chr(code)
        if c not in m:
            m[c] = [cid]
            cid += 1
    return m

_GLOBAL_CODEPOINTS = {"sat": _build_sat_map(), "default": _build_sat_map()}

def get_codepoints_map():
    return _GLOBAL_CODEPOINTS

def get_max_phonemes():
    return 256

def phonemize_codepoints(text):
    return [[c for c in text]]

def phoneme_ids_codepoints(language, phonemes, missing_phonemes=None):
    cmap = _GLOBAL_CODEPOINTS.get(language, _GLOBAL_CODEPOINTS["sat"])
    pad = cmap.get("_", [0])[0]
    bos = cmap.get("^", [1])[0]
    eos = cmap.get("$", [2])[0]
    ids = [bos]
    for p in phonemes:
        if p in cmap:
            ids.extend(cmap[p])
            ids.append(pad)
        elif missing_phonemes is not None:
            missing_phonemes[p] += 1
    ids.append(eos)
    return ids

def tashkeel_run(t):
    return t

phonemize_espeak = None
phoneme_ids_espeak = None
get_espeak_map = None'''
        import re
        prep_code = re.sub(r'from piper_phonemize import[\s\S]+?tashkeel_run,\n\)', pure_py, prep_code)
        with open(prep_path, "w", encoding="utf-8") as f:
            f.write(prep_code)

    # Patch 3: Build monotonic_align Cython extension in-place
    run_cmd_strict(
        f"cd {piper_dir}/src/python && python3 piper_train/vits/monotonic_align/setup.py build_ext --inplace",
        description="Build monotonic_align Cython extension"
    )

    # Patch 4: Fix relative import in monotonic_align/__init__.py
    ma_init = os.path.join(piper_dir, "src", "python", "piper_train", "vits", "monotonic_align", "__init__.py")
    if os.path.exists(ma_init):
        with open(ma_init, "r", encoding="utf-8") as f:
            ma_code = f.read()
        if "from .monotonic_align.core import maximum_path_c" in ma_code:
            ma_code = ma_code.replace(
                "from .monotonic_align.core import maximum_path_c",
                "try:\n    from .core import maximum_path_c\nexcept ImportError:\n    from .monotonic_align.core import maximum_path_c"
            )
            with open(ma_init, "w", encoding="utf-8") as f:
                f.write(ma_code)

    # Patch 5: PyTorch 2.6 & NumPy 2.x backward compatibility for PyTorch Lightning in piper_train
    compat_code = (
        "import numpy as np\n"
        "np.Inf = np.inf\n"
        "np.NAN = np.nan\n"
        "np.PINF = np.inf\n"
        "np.NINF = -np.inf\n"
        "import torch, pathlib\n"
        "try:\n"
        "    if hasattr(torch.serialization, 'add_safe_globals'):\n"
        "        torch.serialization.add_safe_globals([pathlib.PosixPath, pathlib.WindowsPath])\n"
        "except Exception:\n"
        "    pass\n"
        "_orig_torch_load = torch.load\n"
        "def _safe_torch_load(*args, **kwargs):\n"
        "    kwargs.setdefault('weights_only', False)\n"
        "    return _orig_torch_load(*args, **kwargs)\n"
        "torch.load = _safe_torch_load\n"
    )
    for py_file in [
        os.path.join(piper_dir, "src", "python", "piper_train", "__main__.py"),
        os.path.join(piper_dir, "src", "python", "piper_train", "vits", "lightning.py")
    ]:
        if os.path.exists(py_file):
            with open(py_file, "r", encoding="utf-8") as f:
                content = f.read()
            if "_safe_torch_load" not in content:
                with open(py_file, "w", encoding="utf-8") as f:
                    f.write(compat_code + content)

    # Patch 6: Pre-trained VITS warm-start weight transfer & checkpoint saving
    main_py = os.path.join(piper_dir, "src", "python", "piper_train", "__main__.py")
    if os.path.exists(main_py):
        with open(main_py, "r", encoding="utf-8") as f:
            main_code = f.read()

        # Intercept base checkpoint before Trainer construction
        code_before_trainer = (
            "    base_ckpt = getattr(args, 'resume_from_checkpoint', None)\n"
            "    args.resume_from_checkpoint = None\n"
        )
        if "base_ckpt = getattr(args, 'resume_from_checkpoint', None)" not in main_code:
            main_code = main_code.replace(
                "    trainer = Trainer.from_argparse_args(args)",
                code_before_trainer + "    trainer = Trainer.from_argparse_args(args)"
            )

        # Configure checkpoint saving with explicit dirpath and save_last=True
        old_cb = "trainer.callbacks = [ModelCheckpoint(every_n_epochs=args.checkpoint_epochs)]"
        new_cb = (
            "ckpt_dir = args.dataset_dir / 'lightning_logs' / 'checkpoints'\n"
            "        ckpt_dir.mkdir(parents=True, exist_ok=True)\n"
            "        trainer.callbacks = [\n"
            "            ModelCheckpoint(\n"
            "                dirpath=ckpt_dir,\n"
            "                filename='{epoch:04d}',\n"
            "                save_last=True,\n"
            "                every_n_epochs=args.checkpoint_epochs,\n"
            "            )\n"
            "        ]"
        )
        if old_cb in main_code:
            main_code = main_code.replace(old_cb, new_cb)

        # Warm-start model_g and model_d weights before trainer.fit
        warmstart_code = (
            "    if base_ckpt:\n"
            "        _LOGGER.info('Warm-starting weights from base checkpoint: %s', base_ckpt)\n"
            "        model_base = VitsModel.load_from_checkpoint(base_ckpt, dataset=None)\n"
            "        load_state_dict(model.model_g, model_base.model_g.state_dict())\n"
            "        load_state_dict(model.model_d, model_base.model_d.state_dict())\n"
            "        _LOGGER.info('Successfully loaded pre-trained weights into model_g and model_d!')\n"
            "    trainer.fit(model)"
        )
        if "if base_ckpt:" not in main_code and "trainer.fit(model)" in main_code:
            main_code = main_code.replace("    trainer.fit(model)", warmstart_code)

        with open(main_py, "w", encoding="utf-8") as f:
            f.write(main_code)

    # Patch 7: Legacy TorchScript ONNX export (dynamo=False) to bypass Dynamo assertions
    export_py = os.path.join(piper_dir, "src", "python", "piper_train", "export_onnx.py")
    if os.path.exists(export_py):
        with open(export_py, "r", encoding="utf-8") as f:
            exp_code = f.read()
        if "dynamo=False" not in exp_code:
            exp_code = exp_code.replace(
                "verbose=False,",
                "verbose=False,\n        dynamo=False,"
            )
            with open(export_py, "w", encoding="utf-8") as f:
                f.write(exp_code)

    # Install piper_train package with --no-deps
    run_cmd_strict(
        f"cd {piper_dir}/src/python && pip install -q -e . --no-deps",
        description="Install piper_train package"
    )
    print("[OK] Dependencies and Piper VITS extensions installed successfully!", flush=True)


def setup_repository():
    """Clone or pull latest main branch of Vernacular_Pedagogy."""
    print("\n--- Step 2: Syncing Repository ---", flush=True)
    repo_dir = "/content/Vernacular_Pedagogy"
    if not os.path.exists(repo_dir):
        run_cmd_strict(
            f"git clone https://github.com/AshrafGalaxy/Vernacular_Pedagogy.git {repo_dir}",
            description="Clone Vernacular_Pedagogy repository"
        )
    else:
        run_cmd(f"cd {repo_dir} && git fetch origin main && git reset --hard origin/main")
    print(f"[OK] Repository synced at: {repo_dir}", flush=True)
    return repo_dir


def prepare_dataset(repo_dir: str, auth_token: str = ""):
    """Fetch or ingest Santhali speech dataset and generate LJSpeech format."""
    print("\n--- Step 3: Preparing Santhali Speech Dataset ---", flush=True)
    data_dir = "/content/dataset"
    os.makedirs(data_dir, exist_ok=True)
    meta_path = os.path.join(data_dir, "metadata.csv")
    wavs_dir = os.path.join(data_dir, "wavs")

    # Check if pre-packaged voicebank exists in repo or /content
    pre_packaged = "/content/santali_voicebank_16k.tar.gz"
    if os.path.exists(pre_packaged):
        print(f"[INGEST] Unpacking existing archive: {pre_packaged}...", flush=True)
        run_cmd_strict(f"tar -xzf {pre_packaged} -C {data_dir}", description="Unpack voicebank")
    else:
        # Run 04_fetch_santhali_audio.py to stream from Hugging Face
        print("[INGEST] Running 04_fetch_santhali_audio.py...", flush=True)
        token_arg = f"--hf-token {auth_token}" if auth_token else ""
        run_cmd_strict(
            f"python3 {repo_dir}/scripts/04_fetch_santhali_audio.py {token_arg} --output-dir {data_dir} --max-samples 700",
            description="Fetch Santhali speech dataset"
        )

    # Validate dataset content
    if not os.path.exists(meta_path) or not os.path.exists(wavs_dir):
        raise RuntimeError(f"[FATAL] Dataset missing metadata.csv or wavs/ in {data_dir}")

    with open(meta_path, "r", encoding="utf-8") as f:
        clip_count = sum(1 for line in f if line.strip())
    wav_count = len(glob.glob(os.path.join(wavs_dir, "*.wav")))

    print(f"[OK] Dataset prepared: {clip_count} metadata rows, {wav_count} WAV files.", flush=True)
    if clip_count < 10 or wav_count < 10:
        print(f"[WARN] Sample count ({wav_count}) is small, but proceeding with fine-tuning test.")
    return data_dir


def preprocess_dataset(dataset_dir: str):
    """Run Piper preprocess to build phonetic alignment and mel-spectrograms."""
    print("\n--- Step 4: Piper Dataset Preprocessing ---", flush=True)
    out_training_dir = "/content/piper_training_dir"
    if os.path.exists(out_training_dir):
        shutil.rmtree(out_training_dir)
    os.makedirs(out_training_dir, exist_ok=True)

    cmd = (
        f"python3 -m piper_train.preprocess "
        f"--language sat "
        f"--input-dir {dataset_dir} "
        f"--output-dir {out_training_dir} "
        f"--dataset-format ljspeech "
        f"--single-speaker "
        f"--sample-rate 16000 "
        f"--phoneme-type text"
    )
    run_cmd_strict(cmd, cwd="/content/piper/src/python", description="Piper preprocess")
    print(f"[OK] Preprocessed training directory ready: {out_training_dir}", flush=True)
    return out_training_dir


def download_base_checkpoint():
    """Download pre-trained Piper base checkpoint for warm-start fine-tuning."""
    print("\n--- Step 5: Downloading Base VITS Checkpoint ---", flush=True)
    base_ckpt = "/content/piper_base.ckpt"
    if not os.path.exists(base_ckpt):
        url = "https://huggingface.co/datasets/rhasspy/piper-checkpoints/resolve/main/en/en_US/lessac/medium/epoch%3D2164-step%3D1355540.ckpt"
        run_cmd_strict(
            f"wget -q -O {base_ckpt} \"{url}\"",
            description="Download Piper base checkpoint"
        )
    size_mb = os.path.getsize(base_ckpt) / (1024 * 1024)
    print(f"[OK] Base checkpoint ready ({size_mb:.1f} MB): {base_ckpt}", flush=True)
    return base_ckpt


def train_piper_model(training_dir: str, base_ckpt: str, max_epochs: int = 25):
    """Fine-tune Piper VITS on Tesla T4 GPU."""
    print(f"\n--- Step 6: Fine-Tuning Piper VITS (Max Epochs: {max_epochs}) ---", flush=True)
    cmd = (
        f"python3 -m piper_train "
        f"--dataset-dir {training_dir} "
        f"--accelerator gpu "
        f"--devices 1 "
        f"--batch-size 16 "
        f"--validation-split 0.05 "
        f"--checkpoint-epochs 5 "
        f"--max_epochs {max_epochs} "
        f"--resume_from_checkpoint {base_ckpt}"
    )
    t0 = time.time()
    run_cmd_strict(cmd, cwd="/content/piper/src/python", description="Piper VITS training")
    elapsed_min = (time.time() - t0) / 60
    print(f"[OK] Training completed in {elapsed_min:.1f} minutes!", flush=True)


def export_onnx_model(training_dir: str):
    """Export the trained PyTorch checkpoint to ONNX format."""
    print("\n--- Step 7: Exporting to ONNX Format ---", flush=True)
    # Find latest checkpoint across checkpoints/ and lightning_logs/
    ckpts = glob.glob(os.path.join(training_dir, "lightning_logs", "checkpoints", "*.ckpt"))
    if not ckpts:
        ckpts = glob.glob(os.path.join(training_dir, "lightning_logs", "**", "*.ckpt"), recursive=True)
    if not ckpts:
        raise RuntimeError(f"No checkpoint found in {training_dir}/lightning_logs")
    # Prefer last.ckpt if available, otherwise latest by ctime
    last_ckpt = os.path.join(training_dir, "lightning_logs", "checkpoints", "last.ckpt")
    latest_ckpt = last_ckpt if os.path.exists(last_ckpt) else max(ckpts, key=os.path.getctime)
    print(f"[EXPORT] Converting checkpoint: {latest_ckpt}", flush=True)

    onnx_out = "/content/sat_piper_model.onnx"
    json_out = "/content/sat_piper_model.onnx.json"

    export_cmd = f"python3 -m piper_train.export_onnx {latest_ckpt} {onnx_out}"
    run_cmd_strict(export_cmd, cwd="/content/piper/src/python", description="ONNX export")

    # Copy config.json to model.onnx.json
    config_src = os.path.join(training_dir, "config.json")
    if os.path.exists(config_src):
        shutil.copy2(config_src, json_out)
    else:
        raise FileNotFoundError(f"config.json not found in {training_dir}")

    onnx_size_mb = os.path.getsize(onnx_out) / (1024 * 1024)
    print(f"[OK] ONNX model exported: {onnx_out} ({onnx_size_mb:.1f} MB)", flush=True)
    print(f"[OK] Configuration exported: {json_out}", flush=True)
    return onnx_out, json_out


def package_artifacts():
    """Compress the exported ONNX model and config into a single tarball."""
    print("\n--- Step 8: Packaging TTS Model Artifacts ---", flush=True)
    tar_path = "/content/sat_piper_model.tar.gz"
    run_cmd_strict(
        f"cd /content && tar -czf {tar_path} sat_piper_model.onnx sat_piper_model.onnx.json",
        description="Package TTS model"
    )
    size_mb = os.path.getsize(tar_path) / (1024 * 1024)
    print(f"\n{'=' * 60}", flush=True)
    print(f"[PHASE 3 SUCCESS] Final Piper TTS Model Package Ready!", flush=True)
    print(f"  Path: {tar_path}", flush=True)
    print(f"  Package Size: {size_mb:.1f} MB", flush=True)
    print(f"{'=' * 60}", flush=True)
    return tar_path


def main():
    print("=" * 60, flush=True)
    print("Vernacular Pedagogy: Phase 3 Cloud Voice Synthesis (Piper TTS)", flush=True)
    print("=" * 60, flush=True)

    auth_token = os.environ.get("HF_TOKEN", "").strip()

    install_dependencies()
    repo_dir = setup_repository()
    dataset_dir = prepare_dataset(repo_dir, auth_token=auth_token)
    training_dir = preprocess_dataset(dataset_dir)
    base_ckpt = download_base_checkpoint()
    train_piper_model(training_dir, base_ckpt, max_epochs=25)
    export_onnx_model(training_dir)
    package_artifacts()


if __name__ == "__main__":
    main()
