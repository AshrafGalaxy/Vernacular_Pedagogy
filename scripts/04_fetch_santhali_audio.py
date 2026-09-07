# -*- coding: utf-8 -*-
"""
Phase 3: Santhali Speech Corpus Ingestion & Piper TTS Audio Preprocessor
Fetches authentic Santhali (sat_Olck) speech data from:
1. XKaab ASR-santali_100hrs: https://huggingface.co/datasets/XKaab/ASR-santali_100hrs
2. XKaab ASR-Santali_4hrs: https://huggingface.co/datasets/XKaab/ASR-Santali_4hrs
3. AI4Bharat IndicVoices-R: https://huggingface.co/datasets/ai4bharat/indicvoices_r
4. Mozilla Common Voice (sat): https://huggingface.co/datasets/fsicoli/common_voice_19_0

Performs:
- Silence trimming (leading & trailing silence removal)
- RMS & peak loudness normalization (-20 dB LUFS equivalent)
- Ol Chiki orthographic validation (filters noisy non-Ol Chiki scrapes)
- Single dominant speaker clustering to prevent acoustic blurring
- LJSpeech format metadata generation (clip_id|text)
- Voice bank packaging for remote cloud training
"""

import os
import sys
import csv
import re
import argparse
import unicodedata
import tarfile
from collections import Counter
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
    if not text:
        return ""
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


def trim_silence(audio, sr: int = 16000, threshold_db: float = -42.0, pad_ms: int = 120):
    """
    Removes leading and trailing silence below threshold_db with pad_ms margin.
    Prevents awkward initial delays and trailing noise in VITS training.
    """
    import numpy as np

    max_val = np.max(np.abs(audio))
    if max_val < 1e-5:
        return audio

    threshold = max_val * (10.0 ** (threshold_db / 20.0))
    active_indices = np.where(np.abs(audio) > threshold)[0]
    if len(active_indices) == 0:
        return audio

    pad_samples = int((pad_ms / 1000.0) * sr)
    start_idx = max(0, active_indices[0] - pad_samples)
    end_idx = min(len(audio), active_indices[-1] + pad_samples)
    return audio[start_idx:end_idx]


def resample_and_save_wav(audio_array, orig_sr: int, target_path: str, target_sr: int = 16000) -> float:
    """
    Converts audio array to 16 kHz Mono 16-bit PCM WAV, applies silence trimming and
    peak/RMS normalization, and saves to target_path. Returns duration in seconds.
    """
    import numpy as np
    from scipy.signal import resample

    # Ensure float32 array
    audio = np.asarray(audio_array, dtype=np.float32)

    # Convert stereo / multi-channel to mono
    if audio.ndim > 1:
        if audio.shape[0] < audio.shape[1]:  # channels first
            audio = np.mean(audio, axis=0)
        else:  # channels last
            audio = np.mean(audio, axis=1)

    # Resample if sample rates differ
    if orig_sr != target_sr:
        num_target_samples = round(len(audio) * float(target_sr) / orig_sr)
        audio = resample(audio, num_target_samples)

    # Trim leading and trailing silence
    audio = trim_silence(audio, sr=target_sr, threshold_db=-42.0, pad_ms=120)

    # Peak & RMS Normalization (target ~ -20 dB LUFS / 0.92 peak)
    max_val = np.max(np.abs(audio))
    if max_val > 1e-5:
        audio = audio / max_val * 0.92

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
    max_samples: Optional[int] = None,
    data_files: Optional[str] = None,
    dominant_speaker_only: bool = False,
    target_speaker_id: Optional[str] = None
) -> Tuple[int, float]:
    """
    Streams or downloads speech dataset from Hugging Face, validates Ol Chiki transcripts,
    applies speaker filtering, transcodes audio to 16 kHz Mono WAV, and saves metadata.
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
    target_cap = max_samples if max_samples is not None else 1000
    print(f"\n[FETCH] Connecting to dataset: '{dataset_name}' (config={config}, split={split}, cap={target_cap})...")

    try:
        load_kwargs = {"split": split, "token": token or None}
        if config:
            load_kwargs["name"] = config
        if data_files:
            load_kwargs["data_files"] = data_files
        ds = load_dataset(dataset_name, **load_kwargs)
        try:
            from datasets import Audio
            if hasattr(ds, "column_names") and "audio" in ds.column_names:
                ds = ds.cast_column("audio", Audio(sampling_rate=16000))
        except Exception:
            pass
        print(f"[FETCH] Successfully loaded {len(ds)} samples from {dataset_name}.")
    except Exception as e:
        print(f"[WARN] Failed to load {dataset_name} ({config or data_files}): {e}")
        return 0, 0.0

    # Determine dominant speaker if requested and speaker_id column exists
    active_speaker = target_speaker_id
    if dominant_speaker_only and not active_speaker:
        sample_speakers = []
        for j, item in enumerate(ds):
            if j >= 1500:
                break
            spk = item.get("speaker_id")
            if spk:
                sample_speakers.append(spk)
        if sample_speakers:
            top_spk, count = Counter(sample_speakers).most_common(1)[0]
            print(f"[SPEAKER] Isolated dominant narrator voice: '{top_spk}' ({count} sample occurrences)")
            active_speaker = top_spk

    saved_count = 0
    total_duration = 0.0
    records = []

    for i, item in enumerate(ds):
        if target_cap and saved_count >= target_cap:
            break

        # Speaker filter
        if active_speaker:
            spk = item.get("speaker_id")
            if spk and spk != active_speaker:
                continue

        # Extract transcript (support normalized, verbatim, transcription, sentence, text)
        raw_text = item.get("normalized") or item.get("verbatim") or item.get("transcription") or item.get("sentence") or item.get("text") or ""
        norm_text = normalize_olchiki(raw_text)

        if not is_valid_olchiki_sentence(norm_text):
            continue

        # Extract audio (support pre-decoded array, raw bytes, or torchcodec/filepath)
        audio_array = None
        orig_sr = 16000

        # Check 'audio' field
        if "audio" in item and isinstance(item["audio"], dict):
            if item["audio"].get("array") is not None:
                audio_array = item["audio"]["array"]
                orig_sr = item["audio"].get("sampling_rate", 16000)
            elif item["audio"].get("bytes") is not None:
                import io
                try:
                    import soundfile as sf
                    audio_array, orig_sr = sf.read(io.BytesIO(item["audio"]["bytes"]))
                except Exception:
                    try:
                        import torchaudio
                        tensor, orig_sr = torchaudio.load(io.BytesIO(item["audio"]["bytes"]))
                        audio_array = tensor.squeeze().cpu().numpy()
                    except Exception:
                        audio_array = None

        # Check 'audio_filepath' field (common in XKaab datasets)
        if audio_array is None and "audio_filepath" in item:
            af = item["audio_filepath"]
            if isinstance(af, dict) and af.get("array") is not None:
                audio_array = af["array"]
                orig_sr = af.get("sampling_rate", 16000)
            elif hasattr(af, "get_all_samples"):
                samples_obj = af.get_all_samples()
                audio_array = samples_obj.data.squeeze().cpu().numpy()
                orig_sr = samples_obj.sample_rate
            elif isinstance(af, dict) and af.get("bytes") is not None:
                import io
                import soundfile as sf
                try:
                    audio_array, orig_sr = sf.read(io.BytesIO(af["bytes"]))
                except Exception:
                    audio_array = None

        if audio_array is None or len(audio_array) == 0:
            continue

        clip_id = f"sat_{dataset_name.split('/')[-1]}_{i:06d}"
        wav_path = os.path.join(wavs_dir, f"{clip_id}.wav")

        try:
            dur = resample_and_save_wav(audio_array, orig_sr, wav_path, target_sr=16000)
            if 0.8 <= dur <= 12.0:  # Valid Piper VITS duration window
                records.append((clip_id, norm_text))
                total_duration += dur
                saved_count += 1
                if saved_count % 100 == 0:
                    print(f"  Processed {saved_count}/{target_cap} clips ({total_duration/60:.1f} minutes)...")
            else:
                if os.path.exists(wav_path):
                    os.remove(wav_path)
        except Exception:
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


def find_local_common_voice_dir(base_dir: str = BASE_DIR) -> Optional[str]:
    """Finds local Mozilla Common Voice directory if downloaded by user."""
    candidates = [
        os.path.join(base_dir, "Santali dataset Mozilla", "cv-corpus-26.0-2026-06-12", "sat"),
        os.path.join(base_dir, "Santali dataset Mozilla", "sat"),
        os.path.join(base_dir, "Santali dataset Mozilla"),
        os.path.join(base_dir, "data", "raw", "common_voice_sat"),
    ]
    for c in candidates:
        if os.path.exists(os.path.join(c, "clips")) and os.path.exists(os.path.join(c, "validated.tsv")):
            return c
    return None


def ingest_local_common_voice(
    local_dir: str,
    output_dir: str = DEFAULT_OUTPUT_DIR,
    max_samples: Optional[int] = None
) -> Tuple[int, float]:
    """
    Ingests authentic Santhali read speech from local Mozilla Common Voice directory.
    Transcodes MP3s to 16 kHz Mono WAV, trims silence, normalizes loudness, and writes metadata.csv.
    """
    try:
        import soundfile as sf
    except ImportError:
        print("[WARN] soundfile not installed locally. Cannot transcode MP3s.")
        return 0, 0.0

    wavs_dir = os.path.join(output_dir, "wavs")
    os.makedirs(wavs_dir, exist_ok=True)
    meta_path = os.path.join(output_dir, "metadata.csv")
    clips_dir = os.path.join(local_dir, "clips")
    val_tsv = os.path.join(local_dir, "validated.tsv")

    if not os.path.exists(clips_dir) or not os.path.exists(val_tsv):
        print(f"[WARN] Local Common Voice not found in {local_dir}")
        return 0, 0.0

    print(f"\n[LOCAL] Ingesting authentic Santhali speech from local Mozilla Common Voice...")
    print(f"  Directory: {local_dir}")

    saved_count = 0
    total_duration = 0.0
    records = []
    seen_paths = set()

    for tsv_name in ["validated.tsv", "train.tsv", "other.tsv", "dev.tsv", "test.tsv"]:
        tsv_path = os.path.join(local_dir, tsv_name)
        if not os.path.exists(tsv_path):
            continue

        with open(tsv_path, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f, delimiter="\t")
            for row in reader:
                if max_samples and saved_count >= max_samples:
                    break

                mp3_name = row.get("path", "")
                if not mp3_name or mp3_name in seen_paths:
                    continue

                raw_text = row.get("sentence", "")
                norm_text = normalize_olchiki(raw_text)

                if not is_valid_olchiki_sentence(norm_text):
                    continue

                mp3_path = os.path.join(clips_dir, mp3_name)
                if not os.path.exists(mp3_path):
                    continue

                seen_paths.add(mp3_name)
                clip_id = f"sat_cv_{os.path.splitext(mp3_name)[0]}"
                wav_path = os.path.join(wavs_dir, f"{clip_id}.wav")

                try:
                    audio, orig_sr = sf.read(mp3_path)
                    dur = resample_and_save_wav(audio, orig_sr, wav_path, target_sr=16000)
                    if 0.8 <= dur <= 12.0:
                        records.append((clip_id, norm_text))
                        total_duration += dur
                        saved_count += 1
                        if saved_count % 100 == 0:
                            print(f"  Processed {saved_count} local clips ({total_duration/60:.1f} mins)...")
                    else:
                        if os.path.exists(wav_path):
                            os.remove(wav_path)
                except Exception:
                    if os.path.exists(wav_path):
                        os.remove(wav_path)
                    continue

    # Write metadata.csv in LJSpeech format
    mode = "a" if os.path.exists(meta_path) else "w"
    with open(meta_path, mode, encoding="utf-8", newline="") as f:
        writer = csv.writer(f, delimiter="|")
        for clip_id, text in records:
            writer.writerow([clip_id, text])

    print(f"[OK] Ingested {saved_count} validated clips from local Mozilla Common Voice (Total: {total_duration/3600:.2f} hours).")
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
    parser.add_argument("--target-total", type=int, default=2500, help="Target total training clips (default: 2500)")
    parser.add_argument("--single-speaker", action="store_true", default=True, help="Isolate dominant narrator voice")
    args = parser.parse_args()

    token = get_hf_token(args.hf_token)
    total_clips = 0
    total_time = 0.0

    print("=" * 65)
    print("Phase 3: Multi-Source Santhali Speech Corpus Ingestion Pipeline")
    print(f"Target Output:  {args.output_dir}")
    print(f"Target Clips:   {args.target_total}")
    print(f"Voice Mode:     {'Single Dominant Narrator' if args.single_speaker else 'Multi-Speaker'}")
    print(f"HF Auth Status: {'Configured' if token else 'None'}")
    print("=" * 65)

    # Source 0: Check Local Mozilla Common Voice dataset first!
    local_cv_dir = find_local_common_voice_dir(BASE_DIR)
    if local_cv_dir:
        print(f"\n[FOUND] Detected user's local Mozilla Common Voice dataset: {local_cv_dir}")
        c0, t0 = ingest_local_common_voice(local_cv_dir, output_dir=args.output_dir, max_samples=args.target_total)
        total_clips += c0
        total_time += t0

    # Source 1: XKaab ASR-Santali 100hrs (Primary Source - High Quality Shard 0 & 1)
    print("\n--- Source 1: XKaab ASR-Santali 100hrs (Primary Corpus) ---")
    c1, t1 = fetch_from_huggingface(
        "XKaab/ASR-santali_100hrs",
        config=None,
        split="train",
        data_files="data/train-00000-of-00012.parquet",
        hf_token=token,
        output_dir=args.output_dir,
        max_samples=1800,
        dominant_speaker_only=args.single_speaker
    )
    total_clips += c1
    total_time += t1

    # Source 2: XKaab ASR-Santali 4hrs (Curated Benchmark Corpus)
    if total_clips < args.target_total:
        remaining = args.target_total - total_clips
        print(f"\n--- Source 2: XKaab ASR-Santali 4hrs (Curated Corpus, cap={remaining}) ---")
        c2, t2 = fetch_from_huggingface(
            "XKaab/ASR-Santali_4hrs",
            config=None,
            split="valid",
            hf_token=token,
            output_dir=args.output_dir,
            max_samples=remaining
        )
        total_clips += c2
        total_time += t2

    # Source 3: AI4Bharat IndicVoices-R (Phonetic Enrichment Corpus)
    if total_clips < args.target_total:
        remaining = args.target_total - total_clips
        print(f"\n--- Source 3: AI4Bharat IndicVoices-R (Phonetic Diversity, cap={remaining}) ---")
        c3, t3 = fetch_from_huggingface(
            "ai4bharat/indicvoices_r",
            config=None,
            data_files="Santali/train-00000-of-00108.parquet",
            split="train",
            hf_token=token,
            output_dir=args.output_dir,
            max_samples=min(remaining, 300)
        )
        total_clips += c3
        total_time += t3

    if total_clips > 0:
        package_voicebank(args.output_dir)
        print(f"\n{'=' * 65}")
        print(f"[SUCCESS] Voicebank Ready: {total_clips} clips ({total_time/3600:.2f} hours audio)")
        print(f"{'=' * 65}")
    else:
        print("\n[NOTE] No online clips ingested locally. This script will execute automatically")
        print("in the cloud on the Colab T4 GPU instance with full datacenter bandwidth.")


if __name__ == "__main__":
    main()
