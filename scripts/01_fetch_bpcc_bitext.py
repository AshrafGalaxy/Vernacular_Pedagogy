# -*- coding: utf-8 -*-
"""
Phase 1: AI4Bharat BPCC & IN22 Human Parallel Bitext Ingestion
Fetches authentic human-translated parallel corpora from AI4Bharat repositories
specified in Plan.md:
- Repository: https://huggingface.co/datasets/ai4bharat/BPCC (BPCC-Human, sat_Olck <-> hin_Deva)
- Benchmark:  https://huggingface.co/datasets/ai4bharat/IN22-Conv (sat_Olck <-> hin_Deva)
- Benchmark:  https://huggingface.co/datasets/ai4bharat/IN22-Gen (sat_Olck <-> hin_Deva)

Normalizes text and exports to data/processed/bitext/bpcc_hin_sat.tsv
"""

import os
import sys
import csv
import argparse
import unicodedata

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

def normalize_olchiki(text: str) -> str:
    """Canonical Unicode NFC normalization for Santhali Ol Chiki."""
    text = unicodedata.normalize("NFC", text)
    text = text.replace("\u1C78\u1C79", "\u1C79")
    text = " ".join(text.split())
    return text.strip()

def normalize_hindi(text: str) -> str:
    """Canonical Unicode NFC normalization for Hindi source."""
    text = unicodedata.normalize("NFC", text)
    text = " ".join(text.split())
    return text.strip()

def is_pure_olchiki(text: str) -> bool:
    """Ensures Santhali text strictly contains Ol Chiki script characters and punctuation."""
    allowed_punct = " .,!?-—:;'\"()[]/`~"
    chars = [c for c in text.strip() if c not in allowed_punct and not c.isspace()]
    if not chars:
        return False
    olchiki_chars = [c for c in chars if '\u1C50' <= c <= '\u1C7F']
    return (len(olchiki_chars) / len(chars)) >= 0.85

def download_and_extract_bpcc(hf_token: str = None, output_tsv: str = "data/processed/bitext/bpcc_hin_sat.tsv"):
    """
    Downloads and extracts authentic parallel pairs for hin_Deva <-> sat_Olck.
    Designed to run seamlessly in Cloud Colab or environments with Hugging Face access.
    """
    try:
        from datasets import load_dataset
    except ImportError:
        print("[ERROR] 'datasets' library is required to pull from Hugging Face Hub.")
        print("Please run in Cloud Colab or install: pip install datasets huggingface_hub")
        return False

    os.makedirs(os.path.dirname(os.path.abspath(output_tsv)), exist_ok=True)
    extracted_pairs = []
    seen = set()

    def add_pair(hi, sat, source_tag):
        norm_hi = normalize_hindi(hi)
        norm_sat = normalize_olchiki(sat)
        if norm_hi and norm_sat and is_pure_olchiki(norm_sat):
            key = (norm_hi, norm_sat)
            if key not in seen:
                seen.add(key)
                extracted_pairs.append({"source": norm_hi, "target": norm_sat, "provenance": source_tag})

    print("Step 1: Attempting to pull AI4Bharat IN22-Conv & IN22-Gen benchmarks (Public)...")
    for bench_name in ["ai4bharat/IN22-Conv", "ai4bharat/IN22-Gen"]:
        try:
            print(f"  Fetching {bench_name} for sat_Olck...")
            ds = load_dataset(bench_name, "sat_Olck", split="test", token=hf_token)
            # IN22 provides parallel sentences across all 22 languages
            # Fetch matching Hindi and Santhali sentences
            ds_hin = load_dataset(bench_name, "hin_Deva", split="test", token=hf_token)
            for h_row, s_row in zip(ds_hin, ds):
                hi_text = h_row.get("sentence", "")
                sat_text = s_row.get("sentence", "")
                add_pair(hi_text, sat_text, bench_name)
            print(f"  Successfully extracted pairs from {bench_name}. Current total: {len(extracted_pairs)}")
        except Exception as e:
            print(f"  [Note] Could not fetch {bench_name}: {e}")

    print("\nStep 2: Attempting to pull AI4Bharat BPCC-Human (Gated dataset from Plan.md)...")
    try:
        # BPCC contains human gold standard subsets for Indic language pairs
        ds_bpcc = load_dataset("ai4bharat/BPCC", "sat_Olck-hin_Deva", split="train", token=hf_token)
        for row in ds_bpcc:
            hi_text = row.get("hin_Deva", "") or row.get("source", "")
            sat_text = row.get("sat_Olck", "") or row.get("target", "")
            add_pair(hi_text, sat_text, "BPCC-Human")
        print(f"  Successfully extracted BPCC pairs. Total authentic pairs: {len(extracted_pairs)}")
    except Exception as e:
        print(f"  [Note] BPCC requires Hugging Face authentication token with accepted terms: {e}")
        print("  To access BPCC-Human:")
        print("  1. Visit https://huggingface.co/datasets/ai4bharat/BPCC and click 'Access Repository'")
        print("  2. Pass your token: python scripts/01_fetch_bpcc_bitext.py --hf-token <YOUR_TOKEN>")

    if extracted_pairs:
        with open(output_tsv, "w", encoding="utf-8", newline="") as f:
            writer = csv.writer(f, delimiter="\t")
            writer.writerow(["source", "target", "provenance"])
            for row in extracted_pairs:
                writer.writerow([row["source"], row["target"], row["provenance"]])
        print(f"\n[SUCCESS] Exported {len(extracted_pairs)} authentic human bitext pairs to {output_tsv}")
        return True
    else:
        print("\n[WARNING] No pairs extracted. Run this script in Google Colab where Hugging Face access is unrestricted.")
        return False

def load_env_token():
    """Attempts to read HF_TOKEN from environment or local .env file."""
    token = os.environ.get("HF_TOKEN")
    if token:
        return token.strip()
    env_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".env")
    if os.path.exists(env_path):
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                if line.strip().startswith("HF_TOKEN="):
                    return line.strip().split("=", 1)[1].strip().strip('"').strip("'")
    return None

def main():
    parser = argparse.ArgumentParser(description="Ingest AI4Bharat BPCC & IN22 parallel bitext")
    parser.add_argument("--hf-token", type=str, default=None, help="Hugging Face User Access Token for gated BPCC")
    parser.add_argument("--output", type=str, default="data/processed/bitext/bpcc_hin_sat.tsv", help="Output TSV path")
    args = parser.parse_args()

    token = args.hf_token or load_env_token()
    download_and_extract_bpcc(hf_token=token, output_tsv=args.output)

if __name__ == "__main__":
    main()
