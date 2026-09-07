# -*- coding: utf-8 -*-
"""
Phase 1: Ingest Authentic Educational Parallel Bitext (Hindi -> Santhali Ol Chiki)
from AIKosh NCERT Education Repositories:
- Repository: https://huggingface.co/datasets/coild-aikosh/Education
- Repository: https://huggingface.co/datasets/coild-aikosh/Education_v2

Extracts parallel sentence pairs, normalizes Hindi (Devanagari NFC) and Santhali (Ol Chiki NFC),
and exports to data/processed/bitext/aikosh_hin_sat.tsv for merging into training splits.
"""

import os
import sys
import csv
import json
import urllib.request
import urllib.error
import unicodedata

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BITEXT_DIR = os.path.join(BASE_DIR, "data", "processed", "bitext")
AIKOSH_OUTPUT = os.path.join(BITEXT_DIR, "aikosh_hin_sat.tsv")
os.makedirs(BITEXT_DIR, exist_ok=True)

ENV_FILE = os.path.join(BASE_DIR, ".env")

def get_hf_token():
    token = os.environ.get("HF_TOKEN", "")
    if token:
        return token.strip()
    if os.path.exists(ENV_FILE):
        with open(ENV_FILE, "r", encoding="utf-8") as f:
            for line in f:
                if line.strip().startswith("HF_TOKEN="):
                    return line.strip().split("=", 1)[1].strip().strip('"').strip("'")
    return ""

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
    """Ensures text contains valid Ol Chiki script characters."""
    allowed_punct = " .,!?-—:;'\"()[]/`~|।0123456789"
    chars = [c for c in text.strip() if c not in allowed_punct and not c.isspace()]
    if not chars:
        return False
    olchiki_chars = [c for c in chars if '\u1C50' <= c <= '\u1C7F']
    return (len(olchiki_chars) / len(chars)) >= 0.70

def fetch_hf_file(repo_id: str, file_path: str, token: str):
    """Download text file content from Hugging Face repository."""
    url = f"https://huggingface.co/datasets/{repo_id}/raw/main/{file_path}"
    headers = {"User-Agent": "Mozilla/5.0"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        if e.code == 403 or e.code == 401:
            print(f"[ACCESS REQUIRED] HTTP {e.code} for {repo_id}/{file_path}")
            return None
        print(f"[WARN] HTTP {e.code} fetching {file_path}: {e}")
        return None
    except Exception as e:
        print(f"[WARN] Error fetching {file_path}: {e}")
        return None

def parse_aikosh_lines(content: str):
    """Parses parallel lines from tab-delimited, pipe-delimited, or parallel format."""
    pairs = []
    lines = content.splitlines()
    for line in lines:
        line = line.strip()
        if not line:
            continue
        
        # Try tab separation
        if "\t" in line:
            parts = line.split("\t")
            if len(parts) >= 2:
                pairs.append((parts[0].strip(), parts[1].strip()))
                continue
        
        # Try pipe separation
        if "|" in line:
            parts = line.split("|")
            if len(parts) >= 2:
                pairs.append((parts[0].strip(), parts[1].strip()))
                continue

    return pairs

def main():
    print("=" * 65)
    print("Ingesting AIKosh Education & Education_v2 Parallel Datasets")
    print("=" * 65)

    token = get_hf_token()
    print(f"Hugging Face Token loaded: {'Yes (length ' + str(len(token)) + ')' if token else 'No'}")

    all_pairs = []
    seen = set()
    access_issues = []

    def add_clean_pair(hi: str, sat: str):
        # Determine which is Hindi and which is Santhali
        hi_cand, sat_cand = hi, sat
        # If first column has Ol Chiki and second has Devanagari, swap them
        if any('\u1C50' <= c <= '\u1C7F' for c in hi) and any('\u0900' <= c <= '\u097F' for c in sat):
            hi_cand, sat_cand = sat, hi

        norm_hi = normalize_hindi(hi_cand)
        norm_sat = normalize_olchiki(sat_cand)

        if norm_hi and norm_sat and is_pure_olchiki(norm_sat) and len(norm_hi) > 2:
            key = (norm_hi, norm_sat)
            if key not in seen:
                seen.add(key)
                all_pairs.append({"source": norm_hi, "target": norm_sat})

    # Dataset 1: coild-aikosh/Education_v2
    print("\n--- [1/2] Ingesting coild-aikosh/Education_v2 ---")
    v2_files = ["HIN-SAT/Source_Reviewed/EDU/combined.txt"]
    for vf in v2_files:
        content = fetch_hf_file("coild-aikosh/Education_v2", vf, token)
        if content is None:
            access_issues.append("coild-aikosh/Education_v2")
            break
        else:
            parsed = parse_aikosh_lines(content)
            print(f"  Parsed {len(parsed)} candidate lines from {vf}")
            for hi, sat in parsed:
                add_clean_pair(hi, sat)

    # Dataset 2: coild-aikosh/Education
    print("\n--- [2/2] Ingesting coild-aikosh/Education ---")
    v1_files = [
        "EDU/HIN-SAT/Social_Studies_history_part1_hin2sat_translation/translation_text/source_translated/source_translated_merged.txt",
        "EDU/HIN-SAT/Geography_part1_hin-sat_translation/translation_text/source_translated/source_translated_merged.txt",
        "EDU/HIN-SAT/Physics_part1_hin-sat_translation/translation_text/source_translated/source_translated_merged.txt",
        "EDU/HIN-SAT/EDU_NCERT_CHEM_part1_hin-sat_translation/translation_text/source_translated/source_translated_merged.txt",
        "EDU/HIN-SAT/EDU_NCERT_PHY_part1_hin-sat_translation/translation_text/source_translated/source_translated_merged.txt"
    ]
    for vf in v1_files:
        content = fetch_hf_file("coild-aikosh/Education", vf, token)
        if content is None:
            access_issues.append("coild-aikosh/Education")
            break
        else:
            parsed = parse_aikosh_lines(content)
            print(f"  Parsed {len(parsed)} candidate lines from {vf}")
            for hi, sat in parsed:
                add_clean_pair(hi, sat)

    if all_pairs:
        with open(AIKOSH_OUTPUT, "w", encoding="utf-8", newline="") as f:
            writer = csv.writer(f, delimiter="\t")
            writer.writerow(["source", "target"])
            for p in all_pairs:
                writer.writerow([p["source"], p["target"]])
        print(f"\n[SUCCESS] Extracted and verified {len(all_pairs)} educational pairs to:")
        print(f"  {AIKOSH_OUTPUT}")
    else:
        print("\n[NOTICE] Zero pairs could be extracted automatically due to gated access requirements.")

    if access_issues:
        print("\n" + "=" * 65)
        print("ACTION REQUIRED FOR GATED DATASETS:")
        print("The following datasets are gated and require manual user authorization on Hugging Face:")
        for repo in sorted(set(access_issues)):
            print(f"  -> https://huggingface.co/datasets/{repo}")
        print("\nTo grant access with your logged-in account (Ashraf01k):")
        print("1. Open the links above in your browser.")
        print("2. Click 'Agree and access repository' (or submit terms).")
        print("3. Re-run: python scripts/01_fetch_aikosh_education.py")
        print("=" * 65)

if __name__ == "__main__":
    main()
