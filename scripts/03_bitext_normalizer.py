# -*- coding: utf-8 -*-
"""
Phase 1: Bitext Normalizer & FLN Template Slot-Filling Engine
Generates high-precision parallel training bitext (Hindi -> Santhali Ol Chiki)
derived from the verified Grade 1-3 FLN database seeds and NIPUN Bharat pedagogy templates.
"""

import os
import sys
import json
import csv
import random
import unicodedata

sys.stdout.reconfigure(encoding='utf-8')

# Set deterministic random seed for reproducible splits
random.seed(42)

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FLN_JSON = os.path.join(BASE_DIR, "data", "processed", "fln", "fln_lexicon.json")
BITEXT_DIR = os.path.join(BASE_DIR, "data", "processed", "bitext")

def normalize_olchiki(text: str) -> str:
    """Enforces Unicode NFC, strips spurious spaces, preserves Ahd (U+1C79) and Mu TTT (U+1C78)."""
    text = unicodedata.normalize('NFC', text)
    # Santhali Mu TTT / Ahd boundary cleaning
    text = text.replace('\u1C78\u1C79', '\u1C79')
    # Normalize punctuation and extra spaces
    text = ' '.join(text.split())
    return text.strip()

def normalize_hindi(text: str) -> str:
    """Canonical Unicode NFC normalization for Hindi source text."""
    text = unicodedata.normalize('NFC', text)
    text = ' '.join(text.split())
    return text.strip()

def extract_vocab_slots(fln_records):
    """Categorizes verified single-word vocabulary seeds into slot buckets."""
    slots = {
        "objects": [],
        "fruits": [],
        "food": [],
        "vegetables": [],
        "animals": [],
        "birds": [],
        "body_parts": [],
        "colors": [],
        "numbers": [],
        "actions": []
    }
    
    for r in fln_records:
        dom = r.get("domain", "")
        raw_hi = r["source_hindi_normalized"]
        sat = r["target_olchiki_santhali"]
        
        # Clean parenthetical disambiguations for natural sentence generation (e.g. 'बस्ता (थैला)' -> 'बस्ता')
        clean_hi = raw_hi.split("(")[0].strip()
        pair = {"hi": clean_hi, "sat": sat}
        
        if dom == "vocabulary_classroom_objects":
            slots["objects"].append(pair)
        elif dom == "vocabulary_fruits_food":
            if clean_hi in ["पानी", "दूध", "भात", "चावल", "नमक", "तेल", "दाल", "रोटी", "गुड़", "शहद"]:
                slots["food"].append(pair)
            else:
                slots["fruits"].append(pair)
        elif dom == "vocabulary_vegetables":
            slots["vegetables"].append(pair)
        elif dom == "vocabulary_animals_fauna":
            if clean_hi in ["चिड़िया", "कौआ", "कबूतर", "तोता", "मोर", "मुर्गी", "मुर्गा", "बत्तख", "तितली", "मक्खी", "मधुमक्खी"]:
                slots["birds"].append(pair)
            else:
                slots["animals"].append(pair)
        elif dom == "vocabulary_body_parts":
            slots["body_parts"].append(pair)
        elif dom == "vocabulary_colors_attributes":
            if clean_hi in ["लाल", "हरा", "पीला", "नीला", "सफेद", "काला"]:
                slots["colors"].append(pair)
        elif dom == "vocabulary_numbers":
            if clean_hi in ["एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ", "दस"]:
                slots["numbers"].append(pair)
        elif dom == "vocabulary_actions":
            slots["actions"].append(pair)
            
    print("Extracted Slot Statistics:")
    for k, v in slots.items():
        print(f"  - {k:<25}: {len(v)} seeds")
    return slots

def generate_synthetic_bitext(fln_records):
    slots = extract_vocab_slots(fln_records)
    pairs = []
    seen = set()

    def add_pair(hi, sat):
        norm_hi = normalize_hindi(hi)
        norm_sat = normalize_olchiki(sat)
        key = (norm_hi, norm_sat)
        if key not in seen:
            seen.add(key)
            pairs.append({"source": norm_hi, "target": norm_sat})

    # 1. First add all original 368 FLN seed pairs as high-priority ground truth
    for r in fln_records:
        add_pair(r["source_hindi_normalized"], r["target_olchiki_santhali"])

    # 2. Template Category: Object Placement & Commands
    obj_templates = [
        ("बस्ते से {hi} निकालो", "ᱛᱷᱟᱹᱞᱤ ᱠᱷᱚᱱ {sat} ᱚᱰᱚᱠ ᱢᱮ"),
        ("यहाँ {hi} रखो", "ᱱᱚᱸᱰᱮ {sat} ᱫᱚᱦᱚᱭ ᱢᱮ"),
        ("मेज पर {hi} रखो", "ᱴᱮᱵᱩᱞ ᱨᱮ {sat} ᱫᱚᱦᱚᱭ ᱢᱮ"),
        ("मुझे {hi} दो", "ᱤᱧ {sat} ᱮᱢᱟᱹᱧ ᱢᱮ"),
        ("अपना {hi} दिखाओ", "ᱟᱢᱟᱜ {sat} ᱩᱫᱩᱜ ᱢᱮ"),
        ("वह {hi} लाओ", "ᱦᱟᱱᱟ {sat} ᱟᱹᱜᱩᱭ ᱢᱮ"),
        ("क्या आपके पास {hi} है?", "ᱪᱮᱫ ᱟᱢ ᱴᱷᱮᱱ {sat} ᱢᱮᱱᱟᱜ-ᱟ?"),
        ("यह {hi} किसका है?", "ᱱᱚᱣᱟ {sat} ᱫᱚ ᱚᱠᱚᱭᱟᱜ ᱠᱟᱱᱟ?"),
        ("यह एक अच्छा {hi} है", "ᱱᱚᱣᱟ ᱫᱚ ᱢᱤᱫᱴᱟᱝ ᱵᱷᱟᱹᱜᱤ {sat} ᱠᱟᱱᱟ"),
        ("साफ़ जगह पर {hi} रखो", "ᱥᱟᱯᱷᱟ ᱡᱟᱜᱟ ᱨᱮ {sat} ᱫᱚᱦᱚᱭ ᱢᱮ")
    ]
    for obj in slots["objects"]:
        for hi_tpl, sat_tpl in obj_templates:
            add_pair(hi_tpl.format(hi=obj["hi"]), sat_tpl.format(sat=obj["sat"]))

    # 3. Template Category: Fruits & Food
    fruit_templates = [
        ("यह मीठा {hi} है", "ᱱᱚᱣᱟ ᱫᱚ ᱦᱮᱲᱮᱢ {sat} ᱠᱟᱱᱟ"),
        ("ताज़ा {hi} खाओ", "ᱵᱤᱞᱤ {sat} ᱡᱚᱢ ᱢᱮ"),
        ("मुझे {hi} पसंद है", "ᱤᱧ {sat} ᱠᱩᱥᱤᱭᱟᱜ-ᱟ"),
        ("बाज़ार से {hi} लाओ", "ᱵᱟᱡᱟᱨ ᱠᱷᱚᱱ {sat} ᱟᱹᱜᱩᱭ ᱢᱮ"),
        ("पेड़ से {hi} गिरा", "ᱫᱟᱨᱮ ᱠᱷᱚᱱ {sat} ᱧᱩᱨ ᱮᱱᱟ"),
        ("क्या तुम {hi} खाओगे?", "ᱪᱮᱫ ᱟᱢ {sat}ᱢ ᱡᱚᱢᱟ?")
    ]
    for fruit in slots["fruits"]:
        for hi_tpl, sat_tpl in fruit_templates:
            add_pair(hi_tpl.format(hi=fruit["hi"]), sat_tpl.format(sat=fruit["sat"]))

    food_templates = [
        ("साफ़ {hi} पियो", "ᱥᱟᱯᱷᱟ {sat} ᱧᱩᱭ ᱢᱮ"),
        ("गरम {hi} खाओ", "ᱞᱚᱞᱚ {sat} ᱡᱚᱢ ᱢᱮ"),
        ("यहाँ {hi} रखो", "ᱱᱚᱸᱰᱮ {sat} ᱫᱚᱦᱚᱭ ᱢᱮ"),
        ("थोड़ा {hi} दो", "ᱱᱟᱥᱮ {sat} ᱮᱢᱟᱹᱧ ᱢᱮ")
    ]
    for fd in slots["food"]:
        for hi_tpl, sat_tpl in food_templates:
            add_pair(hi_tpl.format(hi=fd["hi"]), sat_tpl.format(sat=fd["sat"]))

    # 4. Template Category: Vegetables
    veg_templates = [
        ("थाली में {hi} रखो", "ᱛᱷᱟᱹᱨᱤ ᱨᱮ {sat} ᱫᱚᱦᱚᱭ ᱢᱮ"),
        ("ताज़ा {hi} खाओ", "ᱵᱤᱞᱤ {sat} ᱡᱚᱢ ᱢᱮ"),
        ("खेत से {hi} लाओ", "ᱠᱷᱮᱛ ᱠᱷᱚᱱ {sat} ᱟᱹᱜᱩᱭ ᱢᱮ"),
        ("हरी {hi} स्वास्थ्य के लिए अच्छी है", "ᱦᱟᱹᱨᱤᱭᱟᱹᱲ {sat} ᱦᱚᱲᱢᱚ ᱞᱟᱹᱜᱤᱫ ᱵᱷᱟᱹᱜᱤ ᱜᱮᱭᱟ")
    ]
    for veg in slots["vegetables"]:
        for hi_tpl, sat_tpl in veg_templates:
            add_pair(hi_tpl.format(hi=veg["hi"]), sat_tpl.format(sat=veg["sat"]))

    # 5. Template Category: Animals & Birds
    ani_templates = [
        ("मैदान में {hi} दौड़ रहा है", "ᱴᱟᱺᱰᱤ ᱨᱮ {sat} ᱫᱟᱹᱲᱮᱫᱟᱭ"),
        ("यह {hi} बहुत बड़ा है", "ᱱᱚᱣᱟ {sat} ᱫᱚ ᱟᱹᱰᱤ ᱢᱟᱨᱟᱝ ᱜᱮᱭᱟ"),
        ("क्या तुमने {hi} देखा?", "ᱪᱮᱫ ᱟᱢ {sat}ᱢ ᱧᱮᱞ ᱠᱮᱫᱮᱭᱟ?"),
        ("पेड़ के पास {hi} खड़ा है", "ᱫᱟᱨᱮ ᱥᱩᱨ ᱨᱮ {sat} ᱛᱤᱸᱜᱩ ᱟᱠᱟᱱᱟᱭ"),
        ("{hi} घास खा रहा है", "{sat} ᱜᱷᱟᱥᱮ ᱡᱚᱢᱮᱫᱟ"),
        ("{hi} पानी पी रहा है", "{sat} ᱫᱟᱜ-ᱮ ᱧᱩᱭᱮᱫᱟ")
    ]
    for ani in slots["animals"]:
        for hi_tpl, sat_tpl in ani_templates:
            add_pair(hi_tpl.format(hi=ani["hi"]), sat_tpl.format(sat=ani["sat"]))

    bird_templates = [
        ("पेड़ पर {hi} बैठी है", "ᱫᱟᱨᱮ ᱨᱮ {sat} ᱫᱩᱲᱩᱵ ᱟᱠᱟᱱᱟᱭ"),
        ("आकाश में {hi} उड़ रही है", "ᱥᱮᱨᱢᱟ ᱨᱮ {sat} ᱩᱰᱟᱹᱣᱜ ᱠᱟᱱᱟᱭ"),
        ("{hi} गाना गा रही है", "{sat} ᱥᱮᱨᱮᱧᱮᱫᱟᱭ"),
        ("सुंदर {hi} को देखो", "ᱪᱚᱨᱚᱠ {sat} ᱧᱮᱞᱮ ᱢᱮ")
    ]
    for bird in slots["birds"]:
        for hi_tpl, sat_tpl in bird_templates:
            add_pair(hi_tpl.format(hi=bird["hi"]), sat_tpl.format(sat=bird["sat"]))

    # 6. Template Category: Colors with Objects
    color_templates = [
        ("यह {obj_hi} {col_hi} रंग का है", "ᱱᱚᱣᱟ {obj_sat} ᱫᱚ {col_sat} ᱨᱚᱝ ᱠᱟᱱᱟ"),
        ("मुझे {col_hi} {obj_hi} पसंद है", "ᱤᱧ {col_sat} {obj_sat} ᱠᱩᱥᱤᱭᱟᱜ-ᱟ"),
        ("{col_hi} {obj_hi} लाओ", "{col_sat} {obj_sat} ᱟᱹᱜᱩᱭ ᱢᱮ"),
        ("{col_hi} {obj_hi} यहाँ रखो", "{col_sat} {obj_sat} ᱱᱚᱸᱰᱮ ᱫᱚᱦᱚᱭ ᱢᱮ")
    ]
    all_concrete_nouns = slots["objects"] + slots["fruits"]
    for col in slots["colors"]:
        for obj in all_concrete_nouns:
            for hi_tpl, sat_tpl in color_templates:
                add_pair(
                    hi_tpl.format(obj_hi=obj["hi"], col_hi=col["hi"]),
                    sat_tpl.format(obj_sat=obj["sat"], col_sat=col["sat"])
                )

    # 7. Template Category: Numbers & Counting
    number_templates = [
        ("यहाँ {num_hi} {noun_hi} हैं", "ᱱᱚᱸᱰᱮ {num_sat} {noun_sat} ᱢᱮᱱᱟᱜ-ᱟ"),
        ("{num_hi} {noun_hi} गिनकर बताओ", "{num_sat} {noun_sat} ᱞᱮᱠᱷᱟ ᱠᱟᱛᱮ ᱞᱟᱹᱭ ᱢᱮ"),
        ("मुझे {num_hi} {noun_hi} दो", "ᱤᱧ {num_sat} {noun_sat} ᱮᱢᱟᱹᱧ ᱢᱮ"),
        ("बस्ते में {num_hi} {noun_hi} हैं", "ᱛᱷᱟᱹᱞᱤ ᱨᱮ {num_sat} {noun_sat} ᱢᱮᱱᱟᱜ-ᱟ")
    ]
    for num in slots["numbers"]:
        for noun in slots["objects"][:8] + slots["fruits"][:6]:
            for hi_tpl, sat_tpl in number_templates:
                add_pair(
                    hi_tpl.format(noun_hi=noun["hi"], num_hi=num["hi"]),
                    sat_tpl.format(noun_sat=noun["sat"], num_sat=num["sat"])
                )

    # 8. Template Category: Body Parts & Hygiene
    body_templates = [
        ("अपने {hi} को छुओ", "ᱟᱢᱟᱜ {sat} ᱡᱚᱴᱮᱫ ᱢᱮ"),
        ("अपने {hi} को साफ़ रखो", "ᱟᱢᱟᱜ {sat} ᱥᱟᱯᱷᱟ ᱫᱚᱦᱚᱭ ᱢᱮ"),
        ("अपने {hi} को मत छुओ", "ᱟᱢᱟᱜ {sat} ᱟᱞᱚᱢ ᱡᱚᱴᱮᱫᱟ"),
        ("{hi} में दर्द है क्या?", "{sat} ᱨᱮ ᱦᱟᱹᱥᱩᱭᱮᱫ ᱢᱮᱭᱟ ᱥᱮ?")
    ]
    for b in slots["body_parts"]:
        for hi_tpl, sat_tpl in body_templates:
            add_pair(hi_tpl.format(hi=b["hi"]), sat_tpl.format(sat=b["sat"]))

    # 9. Template Category: Inclusive Cohortative Actions (Teacher + Students)
    cohort_templates = [
        ("चलो हम सब मिलकर {hi} देखें", "ᱫᱮᱞᱟᱵᱚ ᱥᱟᱱᱟᱢ ᱠᱚ ᱢᱮᱥᱟ ᱠᱟᱛᱮ {sat}ᱵᱚ ᱧᱮᱞᱟ"),
        ("चलो हम सब मिलकर {hi} गिनें", "ᱫᱮᱞᱟᱵᱚ ᱥᱟᱱᱟᱢ ᱠᱚ ᱢᱮᱥᱟ ᱠᱟᱛᱮ {sat}ᱵᱚ ᱞᱮᱠᱷᱟᱭᱟ"),
        ("चलो हम सब मिलकर {hi} के बारे में पढ़ें", "ᱫᱮᱞᱟᱵᱚ ᱥᱟᱱᱟᱢ ᱠᱚ ᱢᱮᱥᱟ ᱠᱟᱛᱮ {sat} ᱵᱟᱵᱚᱛᱵᱚ ᱯᱟᱲᱦᱟᱣᱟ")
    ]
    sample_topics = slots["objects"][:6] + slots["animals"][:6] + slots["fruits"][:4]
    for top in sample_topics:
        for hi_tpl, sat_tpl in cohort_templates:
            add_pair(hi_tpl.format(hi=top["hi"]), sat_tpl.format(sat=top["sat"]))

    print(f"\nGenerated Total Unique Synthetic Sentence Pairs: {len(pairs)}")
    return pairs

def export_train_val_splits(pairs, output_dir=BITEXT_DIR, train_ratio=0.9):
    os.makedirs(output_dir, exist_ok=True)
    
    # Shuffle randomly
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
    
    print(f"\nExported Bitext Splits:")
    print(f"  - Full Corpus: {full_tsv} ({len(pairs)} records)")
    print(f"  - Train Split: {train_tsv} ({len(train_data)} records - {train_ratio*100:.0f}%)")
    print(f"  - Val Split:   {val_tsv} ({len(val_data)} records - {(1-train_ratio)*100:.0f}%)")
    
    # Verify Ol Chiki integrity on generated dataset
    invalid_count = 0
    for idx, p in enumerate(pairs):
        target = p["target"]
        invalids = [c for c in target if not ('\u1C50' <= c <= '\u1C7F' or c.isspace() or c in ".,!?-—:;'\"()[]/`")]
        if invalids:
            print(f"[FLAGGED] Row {idx}: invalid characters {invalids} in '{target}'")
            invalid_count += 1
            
    if invalid_count == 0:
        print("\nVerification PASSED: 100% of generated Ol Chiki targets are compliant with Unicode U+1C50-U+1C7F!")
    else:
        print(f"\nWARNING: {invalid_count} records had invalid characters!")

if __name__ == "__main__":
    with open(FLN_JSON, "r", encoding="utf-8") as f:
        records = json.load(f)
        
    pairs = generate_synthetic_bitext(records)
    export_train_val_splits(pairs)
