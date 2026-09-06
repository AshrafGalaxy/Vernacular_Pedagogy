# -*- coding: utf-8 -*-
"""
Phase 3: Santhali Speech Corpus Ingestion & Piper TTS Audio Preprocessor
Fetches authentic Santhali (sat_Olck) speech data from AI4Bharat IndicVoices-R
and Mozilla Common Voice, standardizes audio to 16 kHz Mono 16-bit PCM WAV,
normalizes Ol Chiki transcripts, and generates LJSpeech-compliant metadata.csv.

Supported Sources:
1. AI4Bharat IndicVoices-R: https://huggingface.co/datasets/ai4bharat/indicvoices_r (sat)
2. Mozilla Common Voice Santali: https://huggingface.co/datasets/fsicoli/common_voice_19_0 (sat)
3. Local directory ingest: data/raw/common_voice_sat/
"""

import os
import sys
import csv
import re
import argparse
import unicodedata
import tarfile
from typing import List, Dict, Tuple, Optional

# Fix Windows console encoding for multi-script terminal output
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

try:
    BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
except NameError:
    BASE_DIR = os.getcwd()
ENV_FILE = os.path.join(BASE_DIR, ".env")
DEFAULT_OUTPUT_DIR = os.path.join(BASE_DIR, "data", "processed", "voice_bank")


def get_hf_token(provided_token: Optional[str] = None) -> str:
    """Retrieve Hugging Face token from parameter, env var, or .env file."""
    if provided_token:
        return provided_token.strip()
    env_token = os.environ.get("HF_TOKEN")
    if env_token:
        return env_token.strip()
    if os.path.exists(ENV_FILE):
        with open(ENV_FILE, "r", encoding="utf-8") as f:
            for line in f:
                if line.strip().startswith("HF_TOKEN="):
                    return line.strip().split("=", 1)[1].strip().strip('"').strip("'")
    return ""


def normalize_olchiki(text: str) -> str:
    """Canonical Unicode NFC normalization for Santhali Ol Chiki text."""
    text = unicodedata.normalize("NFC", text)
    # Santhali Mu TTT / Ahd boundary cleaning
    text = text.replace("\u1C78\u1C79", "\u1C79")
    # Clean whitespace and control characters
    text = " ".join(text.split())
    return text.strip()


def is_valid_olchiki_sentence(text: str, min_chars: int = 3, min_olchiki_ratio: float = 0.8) -> bool:
    """
    Validates that a sentence is predominantly composed of Ol Chiki characters (U+1C50 - U+1C7F)
    and standard punctuation, filtering out noisy Latin/Devanagari/Bengali scraps.
    """
    clean = text.strip()
    if len(clean) < min_chars:
        return False

    olchiki_count = 0
    allowed_punct = " .,!?-—:;'\"()[]/`~᱾᱿"
    total_relevant = 0

    for c in clean:
        if '\u1C50' <= c <= '\u1C7F':
            olchiki_count += 1
            total_relevant += 1
        elif c in allowed_punct:
            continue
        else:
            total_relevant += 1

    if total_relevant == 0:
        return False

    return (olchiki_count / total_relevant) >= min_olchiki_ratio


def resample_and_save_wav(audio_array, orig_sr: int, target_path: str, target_sr: int = 16000) -> float:
    """
    Converts audio array to 16 kHz Mono 16-bit PCM WAV and saves to target_path.
    Returns audio duration in seconds.
    """
    import numpy as np
    from scipy.signal import resample

    # Ensure float32 or float64 array
    audio = np.asarray(audio_array, dtype=np.float32)

    # Convert stereo / multi-channel to mono
    if audio.ndim > 1:
        if audio.shape[0] < audio.shape[1]:  # channels first
            audio = np.mean(audio, axis=0)
        else:  # channels last
            audio = np.mean(audio, axis=1)

    # Normalize audio levels
    max_val = np.max(np.abs(audio))
    if max_val > 1e-6:
        audio = audio / max_val * 0.95

    # Resample if sample rates differ
    if orig_sr != target_sr:
        num_target_samples = round(len(audio) * float(target_sr) / orig_sr)
        audio = resample(audio, num_target_samples)

    # Convert to 16-bit PCM integer values
    audio_int16 = np.clip(audio * 32767.0, -32768, 32767).astype(np.int16)

    # Write WAV file
    os.makedirs(os.path.dirname(os.path.abspath(target_path)), exist_ok=True)
    from scipy.io import wavfile
    wavfile.write(target_path, target_sr, audio_int16)

    duration = len(audio_int16) / float(target_sr)
    return duration


def fetch_from_huggingface(
    dataset_name: str,
    config: Optional[str] = None,
    split: str = "train",
    hf_token: Optional[str] = None,
    output_dir: str = DEFAULT_OUTPUT_DIR,
    max_samples: Optional[int] = None
) -> Tuple[int, float]:
    """
    Streams or downloads speech dataset from Hugging Face, validates Ol Chiki transcripts,
    transcodes audio to 16 kHz Mono WAV, and saves metadata.
    """
    try:
        from datasets import load_dataset
    except ImportError:
        print("[ERROR] 'datasets' library is required to pull from Hugging Face Hub.")
        print("Please install in cloud/venv: pip install datasets huggingface_hub scipy")
        return 0, 0.0

    wavs_dir = os.path.join(output_dir, "wavs")
    os.makedirs(wavs_dir, exist_ok=True)
    meta_path = os.path.join(output_dir, "metadata.csv")

    token = get_hf_token(hf_token)
    target_cap = max_samples if max_samples is not None else 600
    print(f"\n[FETCH] Connecting to Hugging Face dataset: '{dataset_name}' (config={config}, split={split}, cap={target_cap})...")

    try:
        if config:
            ds = load_dataset(dataset_name, config, split=split, token=token or None, streaming=True)
        else:
            ds = load_dataset(dataset_name, split=split, token=token or None, streaming=True)
        is_streaming = True
        print(f"[FETCH] Connected in streaming mode for {dataset_name}.")
    except Exception as e_stream:
        print(f"[WARN] Streaming mode failed ({e_stream}), trying standard download...")
        try:
            if config:
                ds = load_dataset(dataset_name, config, split=split, token=token or None)
            else:
                ds = load_dataset(dataset_name, split=split, token=token or None)
            is_streaming = False
            print(f"[FETCH] Loaded {len(ds)} raw samples from {dataset_name}.")
        except Exception as e:
            print(f"[WARN] Failed to load {dataset_name} ({config}): {e}")
            return 0, 0.0

    saved_count = 0
    total_duration = 0.0
    records = []

    for i, item in enumerate(ds):
        if target_cap and saved_count >= target_cap:
            break

        # Extract transcript (support normalized, transcription, sentence, text)
        raw_text = item.get("normalized") or item.get("transcription") or item.get("sentence") or item.get("text") or ""
        norm_text = normalize_olchiki(raw_text)

        if not is_valid_olchiki_sentence(norm_text):
            continue

        # Extract audio (support standard dict or torchcodec AudioDecoder)
        audio_array = None
        orig_sr = 16000
        if "audio" in item and isinstance(item["audio"], dict):
            audio_array = item["audio"].get("array")
            orig_sr = item["audio"].get("sampling_rate", 16000)
        elif "audio_filepath" in item and hasattr(item["audio_filepath"], "get_all_samples"):
            samples_obj = item["audio_filepath"].get_all_samples()
            audio_tensor = samples_obj.data.squeeze().cpu().numpy()
            audio_array = audio_tensor
            orig_sr = samples_obj.sample_rate

        if audio_array is None or len(audio_array) == 0:
            continue

        clip_id = f"sat_{dataset_name.split('/')[-1]}_{i:06d}"
        wav_path = os.path.join(wavs_dir, f"{clip_id}.wav")

        try:
            dur = resample_and_save_wav(audio_array, orig_sr, wav_path, target_sr=16000)
            if 0.5 <= dur <= 15.0:  # Valid Piper training duration window
                records.append((clip_id, norm_text))
                total_duration += dur
                saved_count += 1
                if saved_count % 50 == 0:
                    print(f"  Processed {saved_count}/{target_cap} clips ({total_duration/60:.1f} minutes)...")
        except Exception as err:
            if os.path.exists(wav_path):
                os.remove(wav_path)
            continue

    # Write metadata.csv in LJSpeech format
    mode = "a" if os.path.exists(meta_path) else "w"
    with open(meta_path, mode, encoding="utf-8", newline="") as f:
        writer = csv.writer(f, delimiter="|")
        for clip_id, text in records:
            writer.writerow([clip_id, text])

    print(f"[OK] Ingested {saved_count} clips from {dataset_name} (Total: {total_duration/3600:.2f} hours).")
    return saved_count, total_duration


def package_voicebank(output_dir: str = DEFAULT_OUTPUT_DIR) -> str:
    """Packages the processed WAVs and metadata.csv into a tar.gz archive for Colab training."""
    tar_path = os.path.join(output_dir, "santali_voicebank_16k.tar.gz")
    wavs_dir = os.path.join(output_dir, "wavs")
    meta_path = os.path.join(output_dir, "metadata.csv")

    if not os.path.exists(meta_path) or not os.path.exists(wavs_dir):
        print(f"[WARN] Cannot package: missing {meta_path} or {wavs_dir}")
        return ""

    print(f"\n[PACKAGE] Compressing voicebank into: {tar_path}...")
    with tarfile.open(tar_path, "w:gz") as tar:
        tar.add(meta_path, arcname="metadata.csv")
        tar.add(wavs_dir, arcname="wavs")

    size_mb = os.path.getsize(tar_path) / (1024 * 1024)
    print(f"[OK] Voicebank archive created: {size_mb:.1f} MB")
    return tar_path


def main():
    parser = argparse.ArgumentParser(description="Fetch and preprocess Santhali speech data for Piper TTS")
    parser.add_argument("--hf-token", type=str, default=None, help="Hugging Face User Access Token")
    parser.add_argument("--output-dir", type=str, default=DEFAULT_OUTPUT_DIR, help="Output directory for voicebank")
    parser.add_argument("--max-samples", type=int, default=600, help="Optional maximum sample cap (default: 600)")
    args = parser.parse_args()

    token = get_hf_token(args.hf_token)
    total_clips = 0
    total_time = 0.0

    print("=" * 60)
    print("Phase 3: Santhali Voice Bank Fetcher & Preprocessor")
    print(f"Target Output: {args.output_dir}")
    print(f"Hugging Face Auth: {'Detected' if token else 'None'}")
    print("=" * 60)

    # Source 1: AI4Bharat IndicVoices-R (Studio-quality Indian Vernacular Speech)
    print("\nAttempting Source 1: AI4Bharat IndicVoices-R ('ai4bharat/indicvoices_r', Santali)...")
    c1, t1 = fetch_from_huggingface("ai4bharat/indicvoices_r", config="Santali", split="train", hf_token=token, output_dir=args.output_dir, max_samples=500)
    total_clips += c1
    total_time += t1

    # Source 2: XKaab Santhali Speech Corpus (Acoustic Diversity)
    print("\nAttempting Source 2: XKaab Santali Speech Corpus ('XKaab/ASR-Santali_4hrs', valid)...")
    c2, t2 = fetch_from_huggingface("XKaab/ASR-Santali_4hrs", config=None, split="valid", hf_token=token, output_dir=args.output_dir, max_samples=200)
    total_clips += c2
    total_time += t2

    if total_clips > 0:
        package_voicebank(args.output_dir)
        print(f"\n[SUCCESS] Completed Santhali Voice Bank: {total_clips} clips, {total_time/3600:.2f} hours audio.")
    else:
        print("\n[NOTE] No online clips ingested. If running locally without 'datasets' installed,")
        print("this script will execute automatically in the cloud on the Colab T4 GPU instance.")


if __name__ == "__main__":
    main()
