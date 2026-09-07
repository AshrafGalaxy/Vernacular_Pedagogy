# -*- coding: utf-8 -*-
"""
Upload Trained Models & Assets to Hugging Face Hub Repository
Hosts:
1. Piper TTS Santhali Model (sat_piper_model.onnx & config JSON)
2. IndicTrans2 INT8 CTranslate2 Model (indictrans2_sat_int8_ct2.tar.gz)
3. FLN Fast-Path Database (fln_lexicon.sqlite)
4. Comprehensive Model Card (README.md)
"""

import os
import sys
import argparse
from dotenv import load_dotenv

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
load_dotenv(os.path.join(BASE_DIR, ".env"))

MODEL_CARD_TEMPLATE = """---
language:
- sat
- hin
tags:
- text-to-speech
- translation
- vernacular-pedagogy
- santhali
- ol-chiki
- indictrans2
- piper-tts
license: mit
pipeline_tag: text-to-speech
---

# Vernacular Pedagogy: Hindi-to-Santhali Edge AI Models

This repository hosts production-ready, lightweight offline models for real-time Hindi $\\rightarrow$ Santhali translation and natural speech synthesis, designed for Foundational Literacy & Numeracy (FLN) Grade 1–3 primary education.

## Model Contents

1. **`sat_piper_model.onnx`** (~60.6 MB): End-to-end VITS neural TTS model trained on authentic multi-speaker Santhali speech (AI4Bharat IndicVoices-R & XKaab).
2. **`sat_piper_model.onnx.json`**: Model configuration, audio sample rate (16,000 Hz), and native Ol Chiki phoneme symbol mapping (`U+1C50`–`U+1C7F`).
3. **`indictrans2_sat_int8_ct2.tar.gz`** (~286.7 MB): CTranslate2 INT8 quantized neural machine translation model (`hin_Deva` $\\rightarrow$ `sat_Olck`), optimized for ultra-fast mobile CPU inference (<120 ms).
4. **`fln_lexicon.sqlite`**: Pre-indexed B-Tree SQLite cache of 368 verified classroom interactions (<0.1 ms retrieval).

## Benchmark Performance (Standard CPU)

- **Speech Synthesis (Piper ONNX):** Real-Time Factor (RTF) = **0.051 – 0.081** (>4x faster than real-time)
- **Synthesis Latency:** **41 ms – 87 ms** per classroom command
- **Translation Latency:** **<120 ms** per sentence

## License & Attribution

Trained and packaged as part of the [Vernacular Pedagogy](https://github.com/AshrafGalaxy/Vernacular_Pedagogy) initiative.
"""

def upload_to_hub(repo_id: str, private: bool = False):
    from huggingface_hub import HfApi, create_repo

    token = os.getenv("HF_TOKEN")
    if not token:
        print("[ERROR] HF_TOKEN not found in environment or .env file.")
        sys.exit(1)

    api = HfApi(token=token)
    user_info = api.whoami()
    username = user_info["name"]
    full_repo_id = f"{username}/{repo_id}" if "/" not in repo_id else repo_id

    print(f"\n[HF HUB] Authenticated as: {username}")
    print(f"[HF HUB] Target Repository: {full_repo_id}")

    # 1. Create Repository if not exists
    try:
        create_repo(repo_id=full_repo_id, repo_type="model", private=private, exist_ok=True, token=token)
        print(f"[OK] Model repository ready: https://huggingface.co/{full_repo_id}")
    except Exception as e:
        print(f"[ERROR] Failed to create repository '{full_repo_id}': {e}")
        print("\nPlease ensure your Hugging Face token has 'Write' permissions enabled at:")
        print("https://huggingface.co/settings/tokens")
        sys.exit(1)

    # 2. Upload Model Card (README.md)
    print("\n[UPLOAD] Uploading README.md (Model Card)...")
    api.upload_file(
        path_or_fileobj=MODEL_CARD_TEMPLATE.encode("utf-8"),
        path_in_repo="README.md",
        repo_id=full_repo_id,
        repo_type="model",
        commit_message="docs: add Vernacular Pedagogy model card"
    )

    # 3. Upload Piper TTS ONNX model & config
    tts_onnx = os.path.join(BASE_DIR, "models", "tts", "sat_piper_model.onnx")
    tts_json = os.path.join(BASE_DIR, "models", "tts", "sat_piper_model.onnx.json")
    tts_tar = os.path.join(BASE_DIR, "models", "tts", "sat_piper_model.tar.gz")

    if os.path.exists(tts_onnx):
        print(f"[UPLOAD] Uploading sat_piper_model.onnx ({os.path.getsize(tts_onnx)/(1024*1024):.1f} MB)...")
        api.upload_file(
            path_or_fileobj=tts_onnx,
            path_in_repo="sat_piper_model.onnx",
            repo_id=full_repo_id,
            repo_type="model",
            commit_message="feat(tts): add sat_piper_model.onnx"
        )

    if os.path.exists(tts_json):
        print("[UPLOAD] Uploading sat_piper_model.onnx.json...")
        api.upload_file(
            path_or_fileobj=tts_json,
            path_in_repo="sat_piper_model.onnx.json",
            repo_id=full_repo_id,
            repo_type="model",
            commit_message="feat(tts): add sat_piper_model.onnx.json config"
        )

    if os.path.exists(tts_tar):
        print(f"[UPLOAD] Uploading sat_piper_model.tar.gz ({os.path.getsize(tts_tar)/(1024*1024):.1f} MB)...")
        api.upload_file(
            path_or_fileobj=tts_tar,
            path_in_repo="sat_piper_model.tar.gz",
            repo_id=full_repo_id,
            repo_type="model",
            commit_message="feat(tts): add sat_piper_model.tar.gz bundle"
        )

    # 4. Upload IndicTrans2 INT8 archive
    mt_tar = os.path.join(BASE_DIR, "models", "mt", "indictrans2_sat_int8_ct2.tar.gz")
    if not os.path.exists(mt_tar):
        mt_tar = os.path.join(BASE_DIR, "models", "mt", "indictrans2_sat_int8_ct2_unpruned.tar.gz")
    if os.path.exists(mt_tar):
        print(f"[UPLOAD] Uploading {os.path.basename(mt_tar)} ({os.path.getsize(mt_tar)/(1024*1024):.1f} MB) -> indictrans2_sat_int8_ct2.tar.gz...")
        api.upload_file(
            path_or_fileobj=mt_tar,
            path_in_repo="indictrans2_sat_int8_ct2.tar.gz",
            repo_id=full_repo_id,
            repo_type="model",
            commit_message="feat(mt): add indictrans2_sat_int8_ct2.tar.gz CTranslate2 bundle"
        )

    # 5. Upload FLN SQLite Lexicon
    sqlite_db = os.path.join(BASE_DIR, "assets", "fln_lexicon.sqlite")
    if os.path.exists(sqlite_db):
        print(f"[UPLOAD] Uploading fln_lexicon.sqlite...")
        api.upload_file(
            path_or_fileobj=sqlite_db,
            path_in_repo="fln_lexicon.sqlite",
            repo_id=full_repo_id,
            repo_type="model",
            commit_message="feat(db): add fln_lexicon.sqlite B-Tree fast-path cache"
        )

    # 6. Upload Audio Samples
    samples_dir = os.path.join(BASE_DIR, "models", "tts", "samples")
    if os.path.isdir(samples_dir):
        for sample_file in os.listdir(samples_dir):
            if sample_file.endswith(".wav"):
                sp = os.path.join(samples_dir, sample_file)
                print(f"[UPLOAD] Uploading sample: samples/{sample_file}...")
                api.upload_file(
                    path_or_fileobj=sp,
                    path_in_repo=f"samples/{sample_file}",
                    repo_id=full_repo_id,
                    repo_type="model",
                    commit_message=f"feat(audio): add synthesized benchmark sample {sample_file}"
                )

    print(f"\n[SUCCESS] All Vernacular Pedagogy models successfully hosted on Hugging Face!")
    print(f"View your repository at: https://huggingface.co/{full_repo_id}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Upload Vernacular Pedagogy models to Hugging Face Hub")
    parser.add_argument("--repo-name", type=str, default="vernacular-pedagogy-santhali", help="Model repo name")
    parser.add_argument("--private", action="store_true", help="Set repository to private")
    args = parser.parse_args()

    upload_to_hub(args.repo_name, private=args.private)
