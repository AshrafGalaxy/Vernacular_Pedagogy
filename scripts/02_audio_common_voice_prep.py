# -*- coding: utf-8 -*-
"""
Phase 1: Mozilla Common Voice Santali Audio Preprocessor & Piper TTS Formatter
Prepares Mozilla Common Voice Santali (v26.0 / sat_Olck) for Piper TTS (VITS) training.

Functions:
1. Parses validated.tsv / train.tsv from Common Voice release.
2. Filters out downvoted, malformed, or non-Ol-Chiki transcripts.
3. Normalizes Ol Chiki text (NFC, Ahd/Mu TTT preservation).
4. Generates Piper/LJSpeech-compliant metadata.csv (clip_id|transcript).
5. Provides batch FFmpeg transcoding commands to standard 16kHz Mono 16-bit PCM WAV.
"""

import os
import sys
import csv
import argparse
import unicodedata
from typing import List, Dict, Tuple

# Fix Windows console encoding for multi-script terminal output
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

def normalize_olchiki(text: str) -> str:
    """Canonical normalization for Santhali Ol Chiki text."""
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
    allowed_punct = " .,!?-—:;'\"()[]/`~"
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

def process_common_voice_tsv(
    tsv_path: str,
    output_metadata_path: str,
    wav_clips_dir: str = "wavs",
    require_upvotes: bool = True
) -> Tuple[int, int, int]:
    """
    Parses a Common Voice TSV file, filters high-quality verified records,
    and writes out metadata.csv formatted for Piper TTS:
    <clip_basename_without_ext>|<normalized_transcript>
    """
    if not os.path.exists(tsv_path):
        raise FileNotFoundError(f"Input TSV file not found: {tsv_path}")

    total_rows = 0
    accepted_rows = 0
    rejected_rows = 0
    output_rows = []

    with open(tsv_path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f, delimiter="\t")
        for row in reader:
            total_rows += 1
            raw_path = row.get("path", "")
            raw_sentence = row.get("sentence", "")
            
            try:
                up_votes = int(row.get("up_votes", 0) or 0)
                down_votes = int(row.get("down_votes", 0) or 0)
            except ValueError:
                up_votes, down_votes = 0, 0

            # Quality Filter: upvotes must equal or exceed downvotes
            if require_upvotes and (down_votes > up_votes):
                rejected_rows += 1
                continue

            norm_sentence = normalize_olchiki(raw_sentence)
            if not is_valid_olchiki_sentence(norm_sentence):
                rejected_rows += 1
                continue

            # Standardize clip identifier (strip extension like .mp3)
            base_name = os.path.splitext(os.path.basename(raw_path))[0]
            output_rows.append((base_name, norm_sentence))
            accepted_rows += 1

    os.makedirs(os.path.dirname(os.path.abspath(output_metadata_path)), exist_ok=True)
    with open(output_metadata_path, "w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f, delimiter="|", quoting=csv.QUOTE_MINIMAL)
        for base_name, transcript in output_rows:
            writer.writerow([base_name, transcript])

    print(f"Common Voice Processing Summary:")
    print(f"  - Total records evaluated: {total_rows}")
    print(f"  - Accepted records:        {accepted_rows} ({(accepted_rows/total_rows*100) if total_rows else 0:.1f}%)")
    print(f"  - Rejected records:        {rejected_rows}")
    print(f"  - Formatted Piper metadata: {output_metadata_path}")
    return total_rows, accepted_rows, rejected_rows

def generate_ffmpeg_transcode_command(
    mp3_dir: str,
    wav_dir: str,
    sample_rate: int = 16000
) -> str:
    """Generates cross-platform FFmpeg commands to transcode clips to 16kHz mono 16-bit PCM WAV."""
    bash_cmd = (
        f'mkdir -p "{wav_dir}" && \\\n'
        f'for f in "{mp3_dir}"/*.mp3; do \\\n'
        f'  [ -f "$f" ] || continue; \\\n'
        f'  ffmpeg -y -v error -i "$f" -acodec pcm_s16le -ac 1 -ar {sample_rate} "{wav_dir}/$(basename "${{f%.*}}").wav"; \\\n'
        f'done'
    )
    return bash_cmd

def main():
    parser = argparse.ArgumentParser(description="Prepare Mozilla Common Voice Santali for Piper TTS")
    parser.add_argument("--tsv", type=str, default="data/raw/common_voice_sat/validated.tsv", help="Path to validated.tsv")
    parser.add_argument("--output", type=str, default="data/processed/voice_bank/metadata.csv", help="Output metadata.csv path")
    parser.add_argument("--allow-unvoted", action="store_true", help="Allow unvoted clips without filtering by upvotes")
    args = parser.parse_args()

    if os.path.exists(args.tsv):
        process_common_voice_tsv(
            tsv_path=args.tsv,
            output_metadata_path=args.output,
            require_upvotes=not args.allow_unvoted
        )
    else:
        print(f"Notice: TSV path '{args.tsv}' does not exist locally.")
        print("This script is ready to run in the Cloud Colab environment where Common Voice Santali is ingested.")
        print("\nExample FFmpeg Transcoding command for Cloud Colab:")
        print(generate_ffmpeg_transcode_command("clips", "data/processed/voice_bank/wavs"))

if __name__ == "__main__":
    main()
