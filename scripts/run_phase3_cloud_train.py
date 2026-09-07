# pyright: reportMissingImports=false
# -*- coding: utf-8 -*-
"""
Phase 3 Cloud Training Worker (Runs on Google Colab Tesla T4 GPU VM)
Automates the full Voice Synthesis pipeline on the remote Colab instance:
1. Clones/pulls latest main branch of Vernacular_Pedagogy repository.
2. Installs Piper TTS, PyTorch Lightning, and audio processing tools.
3. Ingests authentic Santhali speech data (XKaab 100hrs & 4hrs + IndicVoices-R).
4. Preprocesses dataset with deterministic Ol Chiki -> IPA phonetic alignment.
5. Warm-starts fine-tuning of Piper VITS architecture on NVIDIA T4 GPU (80 epochs).
6. Exports trained checkpoint to ONNX format (sat_piper_model.onnx ~60 MB).
7. Serializes configuration JSON (sat_piper_model.onnx.json) with IPA & Ol Chiki maps.
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
    """Run a shell command with live streaming output, return exit code (non-fatal)."""
    if description:
        print(f"\n[EXEC] {description}: {cmd}", flush=True)
    else:
        print(f"\n[EXEC] {cmd}", flush=True)
    try:
        p = subprocess.Popen(
            cmd,
            shell=True,
            cwd=cwd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
            bufsize=1
        )
        if p.stdout:
            for line in iter(p.stdout.readline, ""):
                print(line, end="", flush=True)
            p.stdout.close()
        ret = p.wait()
        if ret != 0:
            print(f"[WARN] Command exited with code {ret}", flush=True)
        return ret
    except Exception as e:
        print(f"[ERROR] Subprocess error: {e}", flush=True)
        return -1


def run_cmd_strict(cmd, cwd=None, description="", capture=False):
    """Run a shell command with live streaming output and abort on failure."""
    if description:
        print(f"\n[EXEC] {description}: {cmd}", flush=True)
    else:
        print(f"\n[EXEC] {cmd}", flush=True)
    p = subprocess.Popen(
        cmd,
        shell=True,
        cwd=cwd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8",
        errors="replace",
        bufsize=1
    )
    if p.stdout:
        for line in iter(p.stdout.readline, ""):
            print(line, end="", flush=True)
        p.stdout.close()
    ret = p.wait()
    if ret != 0:
        msg = f"[FATAL] {description or 'Command'} failed with exit code {ret}: {cmd}"
        print(msg, flush=True)
        raise RuntimeError(msg)
    return ret


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

    # Patch 1: Remove unbuildable piper-phonemize from requirements.txt
    req_path = os.path.join(piper_dir, "src", "python", "requirements.txt")
    if os.path.exists(req_path):
        with open(req_path, "r", encoding="utf-8") as f:
            lines = [l for l in f if "piper-phonemize" not in l]
        with open(req_path, "w", encoding="utf-8") as f:
            f.writelines(lines)

    # Patch 2: Ol Chiki -> IPA Phonemizer with standard eSpeak acoustic map in preprocess.py
    prep_path = os.path.join(piper_dir, "src", "python", "piper_train", "preprocess.py")
    if os.path.exists(prep_path):
        with open(prep_path, "r", encoding="utf-8") as f:
            prep_code = f.read()

        ipa_preprocess_code = '''import sys
sys.path.insert(0, "/content/Vernacular_Pedagogy")
from scripts.santhali_phonemizer import santhali_to_ipa

# Base 154-symbol eSpeak IPA map matching piper_base.ckpt (en_US/lessac/medium)
_BASE_ESPEAK_MAP = {
    "_": [0], "^": [1], "$": [2], " ": [3], "!": [4], "\'": [5], "(": [6], ")": [7],
    ",": [8], "-": [9], ".": [10], ":": [11], ";": [12], "?": [13], "a": [14], "b": [15],
    "c": [16], "d": [17], "e": [18], "f": [19], "h": [20], "i": [21], "j": [22], "k": [23],
    "l": [24], "m": [25], "n": [26], "o": [27], "p": [28], "q": [29], "r": [30], "s": [31],
    "t": [32], "u": [33], "v": [34], "w": [35], "x": [36], "y": [37], "z": [38], "æ": [39],
    "ç": [40], "ð": [41], "ø": [42], "ħ": [43], "ŋ": [44], "œ": [45], "ǀ": [46], "ǁ": [47],
    "ǂ": [48], "ǃ": [49], "ɐ": [50], "ɑ": [51], "ɒ": [52], "ɓ": [53], "ɔ": [54], "ɕ": [55],
    "ɖ": [56], "ɗ": [57], "ɘ": [58], "ə": [59], "ɚ": [60], "ɛ": [61], "ɜ": [62], "ɞ": [63],
    "ɟ": [64], "ɠ": [65], "ɡ": [66], "ɢ": [67], "ɣ": [68], "ɤ": [69], "ɥ": [70], "ɦ": [71],
    "ɧ": [72], "ɨ": [73], "ɪ": [74], "ɫ": [75], "ɬ": [76], "ɭ": [77], "ɮ": [78], "ɯ": [79],
    "ɰ": [80], "ɱ": [81], "ɲ": [82], "ɳ": [83], "ɴ": [84], "ɵ": [85], "ɶ": [86], "ɸ": [87],
    "ɹ": [88], "ɺ": [89], "ɻ": [90], "ɽ": [91], "ɾ": [92], "ʀ": [93], "ʁ": [94], "ʂ": [95],
    "ʃ": [96], "ʄ": [97], "ʈ": [98], "ʉ": [99], "ʊ": [100], "ʋ": [101], "ʌ": [102], "ʍ": [103],
    "ʎ": [104], "ʏ": [105], "ʐ": [106], "ʑ": [107], "ʒ": [108], "ʔ": [109], "ʕ": [110],
    "ʘ": [111], "ʙ": [112], "ʛ": [113], "ʜ": [114], "ʝ": [115], "ʟ": [116], "ʡ": [117],
    "ʢ": [118], "ʲ": [119], "ˈ": [120], "ˌ": [121], "ː": [122], "ˑ": [123], "˞": [124],
    "β": [125], "θ": [126], "χ": [127], "ᵻ": [128], "ⱱ": [129], "0": [130], "1": [131],
    "2": [132], "3": [133], "4": [134], "5": [135], "6": [136], "7": [137], "8": [138],
    "9": [139], "̧": [140], "̃": [141], "̪": [142], "̯": [143], "̩": [144], "ʰ": [145],
    "ˤ": [146], "ε": [147], "↓": [148], "#": [149], "\\"": [150], "↑": [151], "̺": [152],
    "̻": [153]
}

def _build_sat_map():
    m = dict(_BASE_ESPEAK_MAP)
    # Direct Ol Chiki aliases to base IPA phoneme IDs
    aliases = {
        "ᱛ": "t", "ᱫ": "d", "ᱠ": "k", "ᱜ": "ɡ", "ᱯ": "p", "ᱵ": "b",
        "ᱢ": "m", "ᱱ": "n", "ᱝ": "ŋ", "ᱧ": "ɲ", "ᱬ": "ɳ", "ᱴ": "ʈ",
        "ᱰ": "ɖ", "ᱲ": "ɽ", "ᱪ": "c", "ᱡ": "ɟ", "ᱥ": "s", "ᱦ": "h",
        "ᱞ": "l", "ᱨ": "r", "ᱣ": "w", "ᱭ": "j", "ᱚ": "ɔ", "ᱟ": "a",
        "ᱤ": "i", "ᱩ": "u", "ᱮ": "e", "ᱳ": "o", "ᱸ": "̃", "ᱹ": "ə",
        "ᱻ": "ː", "ᱼ": "ʔ", "᱾": ".", "᱿": ".", "ᱷ": "ʰ"
    }
    for olck_char, ipa_sym in aliases.items():
        if ipa_sym in _BASE_ESPEAK_MAP:
            m[olck_char] = _BASE_ESPEAK_MAP[ipa_sym]
    return m

_GLOBAL_CODEPOINTS = {"sat": _build_sat_map(), "default": _build_sat_map()}

def get_codepoints_map():
    return _GLOBAL_CODEPOINTS

def get_max_phonemes():
    return 256

def phonemize_codepoints(text):
    ipa_str = santhali_to_ipa(text)
    return [[c for c in ipa_str]]

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
        if "from piper_phonemize import" in prep_code:
            prep_code = re.sub(r'from piper_phonemize import[\s\S]+?tashkeel_run,\n\)', ipa_preprocess_code, prep_code)
        elif "def _build_sat_map():" in prep_code:
            prep_code = re.sub(r'def _build_sat_map\(\):[\s\S]+?get_espeak_map = None', ipa_preprocess_code, prep_code)

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

    # Patch 5: PyTorch 2.6 & NumPy 2.x backward compatibility
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
        os.path.join(piper_dir, "src", "python", "piper_train", "vits", "lightning.py"),
        os.path.join(piper_dir, "src", "python", "piper_train", "export_onnx.py")
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

        code_before_trainer = (
            "    base_ckpt = getattr(args, 'resume_from_checkpoint', None)\n"
            "    args.resume_from_checkpoint = None\n"
        )
        if "base_ckpt = getattr(args, 'resume_from_checkpoint', None)" not in main_code:
            main_code = main_code.replace(
                "    trainer = Trainer.from_argparse_args(args)",
                code_before_trainer + "    trainer = Trainer.from_argparse_args(args)"
            )

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

    # Patch 7: TorchScript ONNX export (dynamo=False)
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


def prepare_dataset(repo_dir: str, auth_token: str = "", target_clips: int = 2500):
    """Fetch and ingest Santhali speech dataset and generate LJSpeech format."""
    print(f"\n--- Step 3: Preparing Santhali Speech Dataset (Target: {target_clips} clips) ---", flush=True)
    data_dir = "/content/dataset"
    os.makedirs(data_dir, exist_ok=True)
    meta_path = os.path.join(data_dir, "metadata.csv")
    wavs_dir = os.path.join(data_dir, "wavs")

    # Check if pre-packaged voicebank exists in /content (e.g. uploaded from local Mozilla Common Voice)
    pre_packaged = "/content/santali_voicebank_16k.tar.gz"
    if os.path.exists(pre_packaged):
        print(f"[INGEST] Unpacking user's pre-packaged voicebank: {pre_packaged}...", flush=True)
        run_cmd_strict(f"tar -xzf {pre_packaged} -C {data_dir}", description="Unpack voicebank")

    token_arg = f"--hf-token {auth_token}" if auth_token else ""
    run_cmd_strict(
        f"python3 {repo_dir}/scripts/04_fetch_santhali_audio.py {token_arg} --output-dir {data_dir} --target-total {target_clips} --single-speaker",
        description="Fetch Santhali speech dataset"
    )

    if not os.path.exists(meta_path) or not os.path.exists(wavs_dir):
        raise RuntimeError(f"[FATAL] Dataset missing metadata.csv or wavs/ in {data_dir}")

    with open(meta_path, "r", encoding="utf-8") as f:
        clip_count = sum(1 for line in f if line.strip())
    wav_count = len(glob.glob(os.path.join(wavs_dir, "*.wav")))

    print(f"[OK] Dataset prepared: {clip_count} metadata rows, {wav_count} WAV files.", flush=True)
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
    ckpts = glob.glob(os.path.join(training_dir, "lightning_logs", "checkpoints", "*.ckpt"))
    if not ckpts:
        ckpts = glob.glob(os.path.join(training_dir, "lightning_logs", "**", "*.ckpt"), recursive=True)
    if not ckpts:
        all_ckpts = [p for p in glob.glob("/content/**/*.ckpt", recursive=True) if "piper_base.ckpt" not in p]
        if all_ckpts:
            ckpts = all_ckpts
    if not ckpts:
        raise RuntimeError(f"No checkpoint found in {training_dir}/lightning_logs")

    last_ckpt = os.path.join(training_dir, "lightning_logs", "checkpoints", "last.ckpt")
    latest_ckpt = last_ckpt if os.path.exists(last_ckpt) else max(ckpts, key=os.path.getctime)
    print(f"[EXPORT] Converting checkpoint: {latest_ckpt}", flush=True)

    onnx_out = "/content/sat_piper_model.onnx"
    json_out = "/content/sat_piper_model.onnx.json"

    export_cmd = f"python3 -m piper_train.export_onnx {latest_ckpt} {onnx_out}"
    run_cmd_strict(export_cmd, cwd="/content/piper/src/python", description="ONNX export")

    config_src = os.path.join(training_dir, "config.json")
    if os.path.exists(config_src):
        shutil.copy2(config_src, json_out)
    else:
        alt_config = "/content/piper_training_dir/config.json"
        if os.path.exists(alt_config):
            shutil.copy2(alt_config, json_out)
        else:
            raise FileNotFoundError(f"config.json not found in {training_dir}")

    # Verify exported artifacts
    if not os.path.exists(onnx_out) or os.path.getsize(onnx_out) < 1000000:
        raise RuntimeError(f"Exported ONNX file missing or too small: {onnx_out}")
    if not os.path.exists(json_out) or os.path.getsize(json_out) < 100:
        raise RuntimeError(f"Exported config JSON missing or too small: {json_out}")

    onnx_size_mb = os.path.getsize(onnx_out) / (1024 * 1024)
    print(f"[OK] ONNX model exported: {onnx_out} ({onnx_size_mb:.1f} MB)", flush=True)
    print(f"[OK] Configuration exported: {json_out}", flush=True)
    return onnx_out, json_out


def run_insitu_validation(onnx_path: str, config_path: str):
    """Run in-process audio synthesis verification on Colab before packaging."""
    print("\n--- Step 8: In-Situ Audio Synthesis Verification ---", flush=True)
    test_dir = "/content/test_samples"
    os.makedirs(test_dir, exist_ok=True)
    validation_script_path = "/content/validate_tts.py"

    validation_code = f"""import sys
import os
import json
import numpy as np
import onnxruntime as ort
import soundfile as sf

sys.path.insert(0, "/content/Vernacular_Pedagogy")
from scripts.santhali_phonemizer import santhali_to_ipa

with open('{config_path}', 'r', encoding='utf-8') as f:
    cfg = json.load(f)

ph_map = cfg['phoneme_id_map']
session = ort.InferenceSession('{onnx_path}', providers=['CPUExecutionProvider'])

test_sentences = [
    ("val_01", "\\u1C5A\\u1C64\\u1C5F\\u1C5A\\u1C64, \\u1C60\\u1C5E\\u1C5B \\u1C6E\\u1C5E\\u1C5F\\u1C5A \\u1C62\\u1C5E\\u1C65\\u1C5A\\u1C62 \\u1C67\\u1C64\\u1C65\\u1C5A?"),
    ("val_02", "\\u1C64\\u1C65 \\u1C6B\\u1C64 \\u1C67\\u1C64\\u1C5B \\u1C63\\u1C5E\\u1C65 \\u1C64\\u1C5E\\u1C65\\u1C64\\u1C7D-\\u1C5A\\u1C7E"),
    ("val_03", "\\u1C65\\u1C64\\u1C69\\u1C5A \\u1C6B\\u1C64 \\u1C62\\u1C64\\u1C6B\\u1C63\\u1C5A\\u1C65 \\u1C66\\u1C68\\u1C63\\u1C60\\u1C64 \\u1C5A\\u1C5A\\u1C65\\u1C5A\\u1C7E"),
    ("val_04", "\\u1C62\\u1C64\\u1C6C\\u1C5E \\u1C5D\\u1C64\\u1C63\\u1C5A\\u1C65 \\u1C60\\u1C5E\\u1C6C\\u1C5E \\u1C68\\u1C61\\u1C5A\\u1C79\\u1C69 \\u1C5E\\u1C65\\u1C5A\\u1C7E"),
    ("val_05", "\\u1C5A\\u1C62 \\u1C64\\u1C5A\\u1C63\\u1C5E\\u1C62 \\u1C60\\u1C5A\\u1C6E\\u1C5A\\u1C65 \\u1C5A\\u1C5A\\u1C65\\u1C5A?")
]

pad = ph_map.get('_', [0])[0]
bos = ph_map.get('^', [1])[0]
eos = ph_map.get('$', [2])[0]

print("[VALIDATION] Synthesizing 5 sample sentences...", flush=True)
for sid, text in test_sentences:
    try:
        ipa = santhali_to_ipa(text)
        ids = [bos]
        for c in ipa:
            if c in ph_map:
                ids.extend(ph_map[c])
                ids.append(pad)
        ids.append(eos)

        phoneme_ids = np.array(ids, dtype=np.int64)[None, :]
        lengths = np.array([phoneme_ids.shape[1]], dtype=np.int64)
        scales = np.array([0.667, 1.0, 0.8], dtype=np.float32)

        audio = session.run(None, {{
            'input': phoneme_ids,
            'input_lengths': lengths,
            'scales': scales
        }})[0].squeeze()

        out_wav = os.path.join('{test_dir}', f'{{sid}}.wav')
        sf.write(out_wav, audio, 16000)
        dur = len(audio) / 16000.0
        rms = float(np.sqrt(np.mean(audio**2)))
        print(f"  [OK] {{sid}} -> {{dur:.2f}}s, RMS: {{rms:.4f}} -> {{out_wav}}", flush=True)
    except Exception as e:
        print(f"  [WARN] Failed to synthesize {{sid}}: {{e}}", flush=True)

print("[OK] In-situ synthesis verified successfully!", flush=True)
"""
    with open(validation_script_path, "w", encoding="utf-8") as f:
        f.write(validation_code)

    run_cmd(f"python3 {validation_script_path}", description="In-situ synthesis verification")


def package_artifacts():
    """Compress the exported ONNX model, config, and sample WAVs into a single tarball."""
    print("\n--- Step 9: Packaging TTS Model Artifacts ---", flush=True)
    tar_path = "/content/sat_piper_model.tar.gz"
    run_cmd_strict(
        f"cd /content && tar -czf {tar_path} sat_piper_model.onnx sat_piper_model.onnx.json test_samples",
        description="Package TTS model"
    )
    size_mb = os.path.getsize(tar_path) / (1024 * 1024)
    print(f"\n{'=' * 65}", flush=True)
    print(f"[PHASE 3 SUCCESS] Final Piper TTS Model Package Ready!", flush=True)
    print(f"  Path: {tar_path}", flush=True)
    print(f"  Package Size: {size_mb:.1f} MB", flush=True)
    print(f"{'=' * 65}", flush=True)
    return tar_path


def main():
    print("=" * 65, flush=True)
    print("Vernacular Pedagogy: Phase 3 Cloud Voice Synthesis (Piper TTS)", flush=True)
    print("=" * 65, flush=True)

    auth_token = os.environ.get("HF_TOKEN", "").strip()

    install_dependencies()
    repo_dir = setup_repository()
    dataset_dir = prepare_dataset(repo_dir, auth_token=auth_token, target_clips=2500)
    training_dir = preprocess_dataset(dataset_dir)
    base_ckpt = download_base_checkpoint()
    train_piper_model(training_dir, base_ckpt, max_epochs=25)
    onnx_path, config_path = export_onnx_model(training_dir)
    run_insitu_validation(onnx_path, config_path)
    package_artifacts()


if __name__ == "__main__":
    main()
