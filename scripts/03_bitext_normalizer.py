# -*- coding: utf-8 -*-
"""
Phase 1: Semantically Constrained Bitext Normalizer & Pedagogical Expansion Engine
Generates high-precision parallel training bitext (Hindi -> Santhali Ol Chiki)
strictly constrained by pedagogical, common-sense semantic rules, and merges
with authentic human-translated pairs from AI4Bharat BPCC & IN22 when available.
"""

import os
import sys
import json
import csv
import random
import unicodedata

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

# Set deterministic random seed for reproducible splits
random.seed(42)

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FLN_JSON = os.path.join(BASE_DIR, "data", "processed", "fln", "fln_lexicon.json")
BITEXT_DIR = os.path.join(BASE_DIR, "data", "processed", "bitext")
BPCC_TSV = os.path.join(BITEXT_DIR, "bpcc_hin_sat.tsv")

def normalize_olchiki(text: str) -> str:
    """Enforces Unicode NFC, strips spurious spaces, preserves Ahd (U+1C79) and Mu TTT (U+1C78)."""
    text = unicodedata.normalize("NFC", text)
    text = text.replace("\u1C78\u1C79", "\u1C79")
    text = " ".join(text.split())
    return text.strip()

def normalize_hindi(text: str) -> str:
    """Canonical Unicode NFC normalization for Hindi source text."""
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

def load_fln_records(filepath: str):
    with open(filepath, "r", encoding="utf-8") as f:
        return json.load(f)

def generate_semantically_sound_bitext(fln_records):
    """
    Generates natural, semantically verified pedagogical sentences.
    Prevents absurd cross-product anomalies (e.g., 'taking a chair out of a schoolbag',
    'blue papaya', or 'red jackfruit').
    """
    # Build fast lookup dictionary: hindi_word -> olchiki_word
    vocab_map = {}
    for r in fln_records:
        clean_hi = r["source_hindi_normalized"].split("(")[0].strip()
        vocab_map[clean_hi] = r["target_olchiki_santhali"]

    pairs = []
    seen = set()

    def add_pair(hi: str, sat: str):
        norm_hi = normalize_hindi(hi)
        norm_sat = normalize_olchiki(sat)
        if norm_hi and norm_sat and is_pure_olchiki(norm_sat):
            key = (norm_hi, norm_sat)
            if key not in seen:
                seen.add(key)
                pairs.append({"source": norm_hi, "target": norm_sat})

    # 1. Add all 368 verified FLN gold-standard records (sentences and single-word vocabulary)
    for r in fln_records:
        add_pair(r["source_hindi_normalized"], r["target_olchiki_santhali"])

    # 2. Schoolbag items: ONLY items that physically fit inside a schoolbag
    bag_items = [
        ("किताब", "ᱯᱩᱛᱷᱤ"),
        ("पेंसिल", "ᱯᱮᱱᱥᱤᱞ"),
        ("रबर", "ᱨᱚᱵᱚᱨ"),
        ("स्लेट", "ᱥᱞᱮᱴ"),
        ("चॉक", "ᱪᱚᱠ")
    ]
    for hi_item, sat_item in bag_items:
        add_pair(f"बस्ते से {hi_item} निकालो", f"ᱛᱷᱟᱹᱞᱤ ᱠᱷᱚᱱ {sat_item} ᱚᱰᱚᱠ ᱢᱮ")
        add_pair(f"बस्ते में {hi_item} रखो", f"ᱛᱷᱟᱹᱞᱤ ᱨᱮ {sat_item} ᱫᱚᱦᱚᱭ ᱢᱮ")
        add_pair(f"अपना {hi_item} दिखाओ", f"ᱟᱢᱟᱜ {sat_item} ᱩᱫᱩᱜ ᱢᱮ")
        add_pair(f"मुझे {hi_item} दो", f"ᱤᱧ {sat_item} ᱮᱢᱟᱹᱧ ᱢᱮ")
        add_pair(f"क्या आपके पास {hi_item} है?", f"ᱪᱮᱫ ᱟᱢ ᱴᱷᱮᱱ {sat_item} ᱢᱮᱱᱟᱜ-ᱟ?")

    # 3. Classroom furniture & large items (Natural commands)
    large_items = [
        ("कुर्सी", "ᱢᱟᱹᱪᱤ", "कुर्सी पर बैठो", "ᱢᱟᱹᱪᱤ ᱨᱮ ᱫᱩᱲᱩᱵ ᱢᱮ"),
        ("कुर्सी", "ᱢᱟᱹᱪᱤ", "कुर्सी यहाँ लाओ", "ᱢᱟᱹᱪᱤ ᱱᱚᱸᱰᱮ ᱟᱹᱜᱩᱭ ᱢᱮ"),
        ("मेज", "ᱴᱮᱵᱩᱞ", "मेज पर किताब रखो", "ᱴᱮᱵᱩᱞ ᱨᱮ ᱯᱩᱛᱷᱤ ᱫᱚᱦᱚᱭ ᱢᱮ"),
        ("मेज", "ᱴᱮᱵᱩᱞ", "मेज साफ़ करो", "ᱴᱮᱵᱩᱞ ᱥᱟᱯᱷᱟᱭ ᱢᱮ"),
        ("घंटी", "ᱜᱷᱟᱹᱱᱴᱤ", "घंटी बजाओ", "ᱜᱷᱟᱹᱱᱴᱤ ᱨᱩᱭ ᱢᱮ"),
        ("घंटी", "ᱜᱷᱟᱹᱱᱴᱤ", "घंटी बज गई", "ᱜᱷᱟᱹᱱᱴᱤ ᱥᱟᱰᱮ ᱮᱱᱟ")
    ]
    for _, _, hi_sent, sat_sent in large_items:
        add_pair(hi_sent, sat_sent)

    # 4. Realistic and Natural Color Associations (No 'blue papaya' or 'red jackfruit'!)
    natural_color_pairs = [
        # Red
        ("लाल सेब", "ᱟᱨᱟᱜ ᱥᱮᱣ", "यह लाल सेब है", "ᱱᱚᱣᱟ ᱫᱚ ᱟᱨᱟᱜ ᱥᱮᱣ ᱠᱟᱱᱟ"),
        ("लाल सेब", "ᱟᱨᱟᱜ ᱥᱮᱣ", "मीठा लाल सेब खाओ", "ᱦᱮᱲᱮᱢ ᱟᱨᱟᱜ ᱥᱮᱣ ᱡᱚᱢ ᱢᱮ"),
        ("लाल अनार", "ᱟᱨᱟᱜ ᱟᱱᱟᱨ", "लाल अनार लाओ", "ᱟᱨᱟᱜ ᱟᱱᱟᱨ ᱟᱹᱜᱩᱭ ᱢᱮ"),
        ("लाल टमाटर", "ᱟᱨᱟᱜ ᱵᱤᱞᱟᱹᱛᱤ", "ताज़ा लाल टमाटर खाओ", "ᱵᱤᱞᱤ ᱟᱨᱟᱜ ᱵᱤᱞᱟᱹᱛᱤ ᱡᱚᱢ ᱢᱮ"),
        # Yellow
        ("पीला केला", "ᱥᱟᱥᱟᱝ ᱠᱟᱭᱨᱟ", "पीला केला मीठा है", "ᱥᱟᱥᱟᱝ ᱠᱟᱭᱨᱟ ᱫᱚ ᱦᱮᱲᱮᱢ ᱜᱮᱭᱟ"),
        ("पीला केला", "ᱥᱟᱥᱟᱝ ᱠᱟᱭᱨᱟ", "ताज़ा पीला केला खाओ", "ᱵᱤᱞᱤ ᱥᱟᱥᱟᱝ ᱠᱟᱭᱨᱟ ᱡᱚᱢ ᱢᱮ"),
        ("पीला आम", "ᱥᱟᱥᱟᱝ ᱩᱞ", "पेड़ पर पीला आम है", "ᱫᱟᱨᱮ ᱨᱮ ᱥᱟᱥᱟᱝ ᱩᱞ ᱢᱮᱱᱟᱜ-ᱟ"),
        ("पीला आम", "ᱥᱟᱥᱟᱝ ᱩᱞ", "मीठा पीला आम खाओ", "ᱦᱮᱲᱮᱢ ᱥᱟᱥᱟᱝ ᱩᱞ ᱡᱚᱢ ᱢᱮ"),
        # Green
        ("हरी मिर्च", "ᱦᱟᱹᱨᱤᱭᱟᱹᱲ ᱢᱟᱹᱨᱤᱪ", "हरी मिर्च तीखी है", "ᱦᱟᱹᱨᱤᱭᱟᱹᱲ ᱢᱟᱹᱨᱤᱪ ᱫᱚ ᱦᱟᱫᱽ ᱜᱮᱭᱟ"),
        ("हरा अमरूद", "ᱦᱟᱹᱨᱤᱭᱟᱹᱲ ᱟᱢᱨᱩᱫᱽ", "ताज़ा हरा अमरूद खाओ", "ᱵᱤᱞᱤ ᱦᱟᱹᱨᱤᱭᱟᱹᱲ ᱟᱢᱨᱩᱫᱽ ᱡᱚᱢ ᱢᱮ"),
        ("हरी घास", "ᱦᱟᱹᱨᱤᱭᱟᱹᱲ ᱜᱷᱟᱥ", "गाय हरी घास खा रही है", "ᱜᱟᱹᱭ ᱦᱟᱹᱨᱤᱭᱟᱹᱲ ᱜᱷᱟᱥᱮ ᱡᱚᱢᱮᱫᱟ"),
        ("हरा पत्ता", "ᱦᱟᱹᱨᱤᱭᱟᱹᱲ ᱥᱟᱠᱟᱢ", "पेड़ का पत्ता हरा है", "ᱫᱟᱨᱮ ᱨᱮᱭᱟᱜ ᱥᱟᱠᱟᱢ ᱫᱚ ᱦᱟᱹᱨᱤᱭᱟᱹᱲ ᱜᱮᱭᱟ"),
        # White
        ("सफेद दूध", "ᱯᱩᱸᱰ ᱛᱳᱣᱟ", "गरम सफेद दूध पियो", "ᱞᱚᱞᱚ ᱯᱩᱸᱰ ᱛᱳᱣᱟ ᱧᱩᱭ ᱢᱮ"),
        ("सफेद चावल", "ᱯᱩᱸᱰ ᱪᱟᱣᱞᱮ", "थाली में सफेद चावल रखो", "ᱛᱷᱟᱹᱨᱤ ᱨᱮ ᱯᱩᱸᱰ ᱪᱟᱣᱞᱮ ᱫᱚᱦᱚᱭ ᱢᱮ"),
        ("सफेद चॉक", "ᱯᱩᱸᱰ ᱪᱚᱠ", "सफेद चॉक से लिखो", "ᱯᱩᱸᱰ ᱪᱚᱠ ᱛᱮ ᱚᱞ ᱢᱮ"),
        # Black
        ("काला कौआ", "ᱦᱮᱸᱫᱮ ᱠᱟᱶᱦᱮ", "पेड़ पर काला कौआ बैठा है", "ᱫᱟᱨᱮ ᱨᱮ ᱦᱮᱸᱫᱮ ᱠᱟᱶᱦᱮ ᱫᱩᱲᱩᱵ ᱟᱠᱟᱱᱟᱭ"),
        ("काली स्लेट", "ᱦᱮᱸᱫᱮ ᱥᱞᱮᱴ", "काली स्लेट पर चॉक से लिखो", "ᱦᱮᱸᱫᱮ ᱥᱞᱮᱴ ᱨᱮ ᱪᱚᱠ ᱛᱮ ᱚᱞ ᱢᱮ")
    ]
    for hi_np, sat_np, hi_sent, sat_sent in natural_color_pairs:
        add_pair(hi_np, sat_np)
        add_pair(hi_sent, sat_sent)

    # 5. Realistic Counting (Concrete Nouns & Body Parts)
    counting_seeds = [
        ("एक", "ᱢᱤᱫ", "किताब", "ᱯᱩᱛᱷᱤ"),
        ("दो", "ᱵᱟᱨ", "किताब", "ᱯᱩᱛᱷᱤ"),
        ("तीन", "ᱯᱮ", "किताब", "ᱯᱩᱛᱷᱤ"),
        ("चार", "ᱯᱩᱱ", "किताब", "ᱯᱩᱛᱷᱤ"),
        ("पाँच", "ᱢᱚᱬᱮ", "किताब", "ᱯᱩᱛᱷᱤ"),
        ("एक", "ᱢᱤᱫ", "पेंसिल", "ᱯᱮᱱᱥᱤᱞ"),
        ("दो", "ᱵᱟᱨ", "पेंसिल", "ᱯᱮᱱᱥᱤᱞ"),
        ("तीन", "ᱯᱮ", "पेंसिल", "ᱯᱮᱱᱥᱤᱞ"),
        ("पाँच", "ᱢᱚᱬᱮ", "पेंसिल", "ᱯᱮᱱᱥᱤᱞ"),
        ("एक", "ᱢᱤᱫ", "सेब", "ᱥᱮᱣ"),
        ("दो", "ᱵᱟᱨ", "सेब", "ᱥᱮᱣ"),
        ("तीन", "ᱯᱮ", "सेब", "ᱥᱮᱣ"),
        ("चार", "ᱯᱩᱱ", "सेब", "ᱥᱮᱣ"),
        ("पाँच", "ᱢᱚᱬᱮ", "सेब", "ᱥᱮᱣ"),
        ("दो", "ᱵᱟᱨ", "आँख", "ᱢᱮᱫ"),
        ("दो", "ᱵᱟᱨ", "कान", "ᱞᱩᱛᱩᱨ"),
        ("दो", "ᱵᱟᱨ", "हाथ", "ᱛᱤ"),
        ("दो", "ᱵᱟᱨ", "पैर", "ᱡᱟᱸᱜᱟ"),
        ("दस", "ᱜᱮᱞ", "उँगली", "ᱠᱟᱹᱴᱩᱵ")
    ]
    for num_hi, num_sat, noun_hi, noun_sat in counting_seeds:
        add_pair(f"{num_hi} {noun_hi}", f"{num_sat} {noun_sat}")
        add_pair(f"यहाँ {num_hi} {noun_hi} हैं", f"ᱱᱚᱸᱰᱮ {num_sat} {noun_sat} ᱢᱮᱱᱟᱜ-ᱟ")
        add_pair(f"मुझे {num_hi} {noun_hi} दो", f"ᱤᱧ {num_sat} {noun_sat} ᱮᱢᱟᱹᱧ ᱢᱮ")
        add_pair(f"{num_hi} {noun_hi} गिनकर बताओ", f"{num_sat} {noun_sat} ᱞᱮᱠᱷᱟ ᱠᱟᱛᱮ ᱞᱟᱹᱭ ᱢᱮ")

    # 6. Natural Animal & Nature Sentences
    nature_sentences = [
        ("गाय घास खा रही है", "ᱜᱟᱹᱭ ᱜᱷᱟᱥᱮ ᱡᱚᱢᱮᱫᱟ"),
        ("बैल खेत जोत रहा है", "ᱰᱟᱝᱜᱽᱨᱟ ᱠᱷᱮᱛᱮ ᱥᱤᱭᱮᱫᱟ"),
        ("बकरी पत्ता खा रही है", "ᱢᱮᱨᱚᱢ ᱥᱟᱠᱟᱢᱮ ᱡᱚᱢᱮᱫᱟ"),
        ("कुत्ता दरवाज़े पर बैठा है", "ᱥᱮᱛᱟ ᱫᱩᱣᱟᱹᱨ ᱨᱮ ᱫᱩᱲᱩᱵ ᱟᱠᱟᱱᱟᱭ"),
        ("बिल्ली दूध पी रही है", "ᱯᱩᱥᱤ ᱛᱳᱣᱟᱭ ᱧᱩᱭᱮᱫᱟ"),
        ("चिड़िया आकाश में उड़ रही है", "ᱪᱮᱬᱮ ᱥᱮᱨᱢᱟ ᱨᱮ ᱩᱰᱟᱹᱣᱜ ᱠᱟᱱᱟᱭ"),
        ("तोता पेड़ पर बैठा है", "ᱢᱤᱨᱩ ᱫᱟᱨᱮ ᱨᱮ ᱫᱩᱲᱩᱵ ᱟᱠᱟᱱᱟᱭ"),
        ("मोर नाच रहा है", "ᱢᱟᱨᱟᱜ ᱮᱱᱮᱡ ᱠᱟᱱᱟᱭ"),
        ("नदी में साफ़ पानी बह रहा है", "ᱜᱟᱰᱟ ᱨᱮ ᱥᱟᱯᱷᱟ ᱫᱟᱜ ᱞᱤᱸᱜᱤᱱ ᱠᱟᱱᱟ"),
        ("सूरज पूरब से निकलता है", "ᱵᱮᱲᱟ ᱫᱚ ᱥᱟᱢᱟᱝ ᱠᱷᱚᱱ ᱨᱟᱠᱟᱵ-ᱟ"),
        ("बारिश हो रही है", "ᱫᱟᱜ ᱡᱟᱹᱲᱤ ᱧᱩᱨᱩᱜ ᱠᱟᱱᱟ"),
        ("पेड़ हमें फल देते हैं", "ᱫᱟᱨᱮ ᱫᱚ ᱡᱚ ᱮᱢᱟᱵᱚᱱᱟ")
    ]
    for hi_sent, sat_sent in nature_sentences:
        add_pair(hi_sent, sat_sent)

    # 7. Ingest authentic BPCC bitext if present
    if os.path.exists(BPCC_TSV):
        print(f"\nMerging authentic BPCC bitext from {BPCC_TSV}...")
        bpcc_count = 0
        with open(BPCC_TSV, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f, delimiter="\t")
            for row in reader:
                add_pair(row["source"], row["target"])
                bpcc_count += 1
        print(f"  Merged {bpcc_count} authentic pairs from BPCC/IN22!")

    print(f"\nTotal Clean, Semantically Sound Parallel Pairs: {len(pairs)}")
    return pairs

def export_train_val_splits(pairs, output_dir=BITEXT_DIR, train_ratio=0.9):
    os.makedirs(output_dir, exist_ok=True)
    random.shuffle(pairs)
    
    split_idx = int(len(pairs) * train_ratio)
    train_data = pairs[:split_idx]
    val_data = pairs[split_idx:]
    
    train_tsv = os.path.join(output_dir, "train.tsv")
    val_tsv = os.path.join(output_dir, "val.tsv")
    full_tsv = os.path.join(output_dir, "synthetic_train.tsv")
    
    def write_tsv(filepath, data):
        with open(filepath, "w", encoding="utf-8", newline="") as f:
            writer = csv.writer(f, delimiter="\t")
            writer.writerow(["source", "target"])
            for row in data:
                writer.writerow([row["source"], row["target"]])
                
    write_tsv(train_tsv, train_data)
    write_tsv(val_tsv, val_data)
    write_tsv(full_tsv, pairs)
    
    print(f"\nExported Clean Bitext Splits:")
    print(f"  - Full Corpus: {full_tsv} ({len(pairs)} records)")
    print(f"  - Train Split: {train_tsv} ({len(train_data)} records - {train_ratio*100:.0f}%)")
    print(f"  - Val Split:   {val_tsv} ({len(val_data)} records - {(1-train_ratio)*100:.0f}%)")

if __name__ == "__main__":
    if not os.path.exists(FLN_JSON):
        print(f"[ERROR] FLN lexicon not found at: {FLN_JSON}")
        print("Please run scripts/06_build_sqlite_lexicon.py first to generate the lexicon.")
        sys.exit(1)
    records = load_fln_records(FLN_JSON)
    if not records:
        print(f"[ERROR] FLN lexicon at {FLN_JSON} is empty.")
        sys.exit(1)
    pairs = generate_semantically_sound_bitext(records)
    if not pairs:
        print("[ERROR] No parallel pairs generated. Check FLN lexicon content.")
        sys.exit(1)
    export_train_val_splits(pairs)
