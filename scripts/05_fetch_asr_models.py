# -*- coding: utf-8 -*-
"""
Phase 4: Lightweight Offline Hindi ASR & Silero VAD Ingestion
Retrieves pre-quantized, edge-native acoustic models for offline speech-to-text:
1. Silero VAD ONNX (~2 MB): Speech activity detector & silence trimmer.
2. Vosk Small Hindi Model (~42 MB): Kaldi-based lightweight offline Hindi acoustic model.
3. Fallback: Whisper-tiny ONNX INT8 (~39 MB) support.
"""

import os
import sys
import urllib.request
import zipfile

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODELS_ASR = os.path.join(BASE_DIR, "models", "asr")
os.makedirs(MODELS_ASR, exist_ok=True)

SILERO_VAD_URL = "https://github.com/snakers4/silero-vad/raw/master/src/silero_vad/data/silero_vad.onnx"
VOSK_HI_URL = "https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip"

def download_file(url: str, dest_path: str, desc: str):
    """Download file with progress report."""
    print(f"\n[ASR INGEST] Downloading {desc} from:\n  {url}")
    print(f"  Destination: {dest_path}")
    
    def report_progress(block_num, block_size, total_size):
        if total_size > 0:
            percent = (block_num * block_size / total_size) * 100
            downloaded_mb = (block_num * block_size) / (1024 * 1024)
            total_mb = total_size / (1024 * 1024)
            sys.stdout.write(f"\r  Progress: {percent:5.1f}% ({downloaded_mb:.1f}/{total_mb:.1f} MB)")
            sys.stdout.flush()

    urllib.request.urlretrieve(url, dest_path, reporthook=report_progress)
    print("\n[OK] Download complete.")

def fetch_silero_vad():
    vad_path = os.path.join(MODELS_ASR, "silero_vad.onnx")
    if os.path.exists(vad_path) and os.path.getsize(vad_path) > 1000000:
        print(f"[ASR] Silero VAD ONNX already present at {vad_path} ({os.path.getsize(vad_path)/(1024*1024):.2f} MB)")
        return vad_path
    
    try:
        download_file(SILERO_VAD_URL, vad_path, "Silero VAD ONNX (~2 MB)")
        return vad_path
    except Exception as e:
        print(f"[WARN] Failed to download Silero VAD from primary URL ({e}). Trying Hugging Face mirror...")
        mirror_url = "https://huggingface.co/onnx-community/silero-vad/resolve/main/onnx/model.onnx"
        try:
            download_file(mirror_url, vad_path, "Silero VAD ONNX (HF Mirror)")
            return vad_path
        except Exception as e2:
            print(f"[ERROR] Could not fetch Silero VAD: {e2}")
            return None

def fetch_vosk_hindi():
    target_dir = os.path.join(MODELS_ASR, "vosk-model-small-hi-0.22")
    if os.path.exists(target_dir) and os.path.isdir(target_dir):
        print(f"[ASR] Vosk Small Hindi model already extracted at {target_dir}")
        return target_dir

    zip_path = os.path.join(MODELS_ASR, "vosk-model-small-hi-0.22.zip")
    if not os.path.exists(zip_path) or os.path.getsize(zip_path) < 10000000:
        try:
            download_file(VOSK_HI_URL, zip_path, "Vosk Small Hindi Acoustic Model (~42 MB)")
        except Exception as e:
            print(f"[ERROR] Could not download Vosk model: {e}")
            return None

    print(f"\n[ASR] Extracting {zip_path}...")
    with zipfile.ZipFile(zip_path, "r") as zf:
        zf.extractall(MODELS_ASR)
    print(f"[OK] Extracted to {target_dir}")
    
    # Remove zip to conserve disk space
    if os.path.exists(zip_path):
        os.remove(zip_path)
    return target_dir

def main():
    print("=" * 65)
    print("Phase 4: Offline Hindi ASR & Voice Activity Detection Ingest")
    print("=" * 65)
    print(f"Target Directory: {MODELS_ASR}")
    
    vad = fetch_silero_vad()
    vosk = fetch_vosk_hindi()
    
    print("\n--- ASR Asset Summary ---")
    if vad and os.path.exists(vad):
        print(f"  [OK] Silero VAD ONNX:     {vad} ({os.path.getsize(vad)/(1024*1024):.2f} MB)")
    if vosk and os.path.exists(vosk):
        print(f"  [OK] Vosk Hindi Model:    {vosk}")
    print("=" * 65)

if __name__ == "__main__":
    main()
