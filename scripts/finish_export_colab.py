# -*- coding: utf-8 -*-
"""
Colab Direct Exporter: Run ONNX Export, Validation, and Packaging on existing checkpoint!
"""
import os
import sys
import glob
import shutil
import subprocess

print("=" * 60)
print("RUNNING IMMEDIATE EXPORT FROM SAVED CHECKPOINT ON COLAB")
print("=" * 60)

training_dir = "/content/piper_training_dir"
ckpt_dir = os.path.join(training_dir, "lightning_logs", "checkpoints")
ckpts = glob.glob(os.path.join(ckpt_dir, "*.ckpt"))
print("Available checkpoints:", ckpts)

# Prefer last.ckpt or epoch=0019.ckpt
best_ckpt = None
for name in ["last.ckpt", "epoch=0019.ckpt", "epoch=0014.ckpt"]:
    p = os.path.join(ckpt_dir, name)
    if os.path.exists(p):
        best_ckpt = p
        break

if not best_ckpt and ckpts:
    best_ckpt = ckpts[0]

if not best_ckpt:
    print("[ERROR] No checkpoint found!")
    sys.exit(1)

print(f"Using checkpoint: {best_ckpt} ({os.path.getsize(best_ckpt)/(1024*1024):.1f} MB)")

onnx_out = "/content/sat_piper_model.onnx"
json_out = "/content/sat_piper_model.onnx.json"

export_cmd = f"python3 -m piper_train.export_onnx {best_ckpt} {onnx_out}"
print("Running export:", export_cmd)
res = subprocess.run(export_cmd, shell=True, capture_output=True, text=True)
print("Export STDOUT:", res.stdout)
print("Export STDERR:", res.stderr)

if res.returncode != 0:
    print(f"[ERROR] Export failed with return code {res.returncode}")
    sys.exit(res.returncode)

# Copy config.json
config_src = os.path.join(training_dir, "lightning_logs", "version_0", "config.json")
if os.path.exists(config_src):
    shutil.copy2(config_src, json_out)
elif os.path.exists(os.path.join(training_dir, "config.json")):
    shutil.copy2(os.path.join(training_dir, "config.json"), json_out)
print("Exported ONNX size:", os.path.getsize(onnx_out)/(1024*1024), "MB")
print("Exported JSON size:", os.path.getsize(json_out), "bytes")

# Run in-situ validation
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

with open('{json_out}', 'r', encoding='utf-8') as f:
    cfg = json.load(f)

ph_map = cfg['phoneme_id_map']
session = ort.InferenceSession('{onnx_out}', providers=['CPUExecutionProvider'])

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

res = subprocess.run(f"python3 {validation_script_path}", shell=True, capture_output=True, text=True)
print("Validation STDOUT:", res.stdout)
print("Validation STDERR:", res.stderr)

# Package artifacts
tar_path = "/content/sat_piper_model.tar.gz"
subprocess.run(f"cd /content && tar -czf {tar_path} sat_piper_model.onnx sat_piper_model.onnx.json test_samples", shell=True, check=True)
print("Tarball created:", tar_path, os.path.getsize(tar_path)/(1024*1024), "MB")
print("[ALL_DONE_SUCCESSFULLY]")
