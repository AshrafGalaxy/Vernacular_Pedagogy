import csv
import json
import os
import re
import sys
import unicodedata

sys.stdout.reconfigure(encoding='utf-8')

def load_and_clean_raw_fln(raw_path: str):
    with open(raw_path, 'r', encoding='utf-8') as f:
        content = f.read().strip()
    
    # Handle multiple JSON arrays pasted consecutively: [ ... ] \n [ ... ] -> [ ... , ... ]
    merged = re.sub(r'\]\s*\[', ',', content)
    records = json.loads(merged)
    
    clean_records = []
    seen_ids = set()
    
    for item in records:
        rec_id = item.get('id', '').strip()
        if rec_id in seen_ids:
            print(f"Warning: Duplicate ID '{rec_id}' skipped.")
            continue
        seen_ids.add(rec_id)
        
        cleaned = {}
        for k, v in item.items():
            if isinstance(v, str):
                cleaned[k] = unicodedata.normalize('NFC', v).strip()
            else:
                cleaned[k] = v
                
        # Validate Ol Chiki characters
        ol_text = cleaned.get('target_olchiki_santhali', '')
        invalid_ol = [
            c for c in ol_text 
            if not ('\u1C50' <= c <= '\u1C7F' or c.isspace() or c in ".,!?-—:;'\"()[]/`")
        ]
        if invalid_ol:
            print(f"[REJECTED] {rec_id}: Invalid Ol Chiki characters {invalid_ol}")
            continue
            
        clean_records.append(cleaned)
        
    return clean_records

def export_fln_datasets(raw_file: str, out_dir: str):
    os.makedirs(out_dir, exist_ok=True)
    records = load_and_clean_raw_fln(raw_file)
    
    # 1. Export JSON
    out_json = os.path.join(out_dir, 'fln_lexicon.json')
    with open(out_json, 'w', encoding='utf-8') as f:
        json.dump(records, f, ensure_ascii=False, indent=2)
        
    # 2. Export TSV
    out_tsv = os.path.join(out_dir, 'fln_lexicon.tsv')
    if records:
        fieldnames = [
            'id', 'domain', 'nipun_target_grade', 'source_hindi_normalized',
            'target_olchiki_santhali', 'phonetic_deva_santhali',
            'target_warang_chiti_ho', 'phonetic_deva_ho',
            'target_mundari_deva', 'phonetic_deva_mundari',
            'linguistic_validation_notes'
        ]
        with open(out_tsv, 'w', encoding='utf-8', newline='') as f:
            writer = csv.DictWriter(f, fieldnames=fieldnames, delimiter='\t', extrasaction='ignore')
            writer.writeheader()
            writer.writerows(records)
            
    print(f"Exported {len(records)} clean records to:")
    print(f"  - JSON: {out_json}")
    print(f"  - TSV:  {out_tsv}")

if __name__ == '__main__':
    raw_file = r'c:\Users\Ashraf\Desktop\26042\FLN'
    out_dir = r'c:\Users\Ashraf\Desktop\26042\data\processed\fln'
    export_fln_datasets(raw_file, out_dir)
