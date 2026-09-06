import csv
import json
import os
import sys
import unicodedata

sys.stdout.reconfigure(encoding='utf-8')

def verify_and_clean_santhali_fln(json_path: str):
    """
    Validates Santhali FLN records:
    1. Canonical Unicode normalization (NFC)
    2. Strict Ol Chiki Unicode range verification (U+1C50 - U+1C7F)
    3. Lean schema enforcement (id, domain, nipun_target_grade, source_hindi_normalized, target_olchiki_santhali, phonetic_deva_santhali)
    """
    with open(json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
        
    clean_records = []
    seen_ids = set()
    seen_hindi = set()
    
    for item in data:
        rec_id = item.get('id', '').strip()
        if rec_id in seen_ids:
            print(f"[DUPLICATE ID] Skipped {rec_id}")
            continue
        seen_ids.add(rec_id)
        
        hindi_norm = unicodedata.normalize('NFC', item.get('source_hindi_normalized', '')).strip()
        olchiki_norm = unicodedata.normalize('NFC', item.get('target_olchiki_santhali', '')).strip()
        deva_norm = unicodedata.normalize('NFC', item.get('phonetic_deva_santhali', '')).strip()
        
        # Verify Ol Chiki characters
        invalid_chars = [
            c for c in olchiki_norm 
            if not ('\u1C50' <= c <= '\u1C7F' or c.isspace() or c in ".,!?-—:;'\"()[]/`")
        ]
        if invalid_chars:
            print(f"[INVALID OL CHIKI] {rec_id}: {invalid_chars} in '{olchiki_norm}'")
            continue
            
        clean_records.append({
            "id": rec_id,
            "domain": item.get("domain", "general").strip(),
            "nipun_target_grade": item.get("nipun_target_grade", "Grade 1").strip(),
            "source_hindi_normalized": hindi_norm,
            "target_olchiki_santhali": olchiki_norm,
            "phonetic_deva_santhali": deva_norm
        })
        
    print(f"Verified {len(clean_records)} / {len(data)} Santhali FLN records successfully.")
    return clean_records

def export_lean_fln(json_path: str, tsv_path: str):
    records = verify_and_clean_santhali_fln(json_path)
    
    # Write back clean JSON
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(records, f, ensure_ascii=False, indent=2)
        
    # Write clean TSV
    fieldnames = ['id', 'domain', 'nipun_target_grade', 'source_hindi_normalized', 'target_olchiki_santhali', 'phonetic_deva_santhali']
    with open(tsv_path, 'w', encoding='utf-8', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, delimiter='\t')
        writer.writeheader()
        writer.writerows(records)
        
    print(f"Export complete:\n  JSON: {json_path}\n  TSV:  {tsv_path}")

if __name__ == '__main__':
    base_dir = os.path.dirname(os.path.dirname(__file__))
    json_p = os.path.join(base_dir, 'data', 'processed', 'fln', 'fln_lexicon.json')
    tsv_p = os.path.join(base_dir, 'data', 'processed', 'fln', 'fln_lexicon.tsv')
    export_lean_fln(json_p, tsv_p)
