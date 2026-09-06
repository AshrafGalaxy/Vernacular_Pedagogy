import json
import os
import sqlite3
import sys
import time
import unicodedata

sys.stdout.reconfigure(encoding='utf-8')

DB_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), "assets", "fln_lexicon.sqlite")
JSON_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), "data", "processed", "fln", "fln_lexicon.json")

def build_sqlite_lexicon(json_file: str = JSON_PATH, db_file: str = DB_PATH):
    os.makedirs(os.path.dirname(db_file), exist_ok=True)
    
    # Remove existing database file if present to cleanly rebuild with new schema
    if os.path.exists(db_file):
        os.remove(db_file)
        
    with open(json_file, "r", encoding="utf-8") as f:
        records = json.load(f)
        
    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()
    
    # 1. Lean DDL Schema strictly focused on Santhali FLN
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS fln_lexicon (
        id TEXT PRIMARY KEY,
        domain TEXT NOT NULL,
        nipun_target_grade TEXT NOT NULL,
        source_hindi_normalized TEXT NOT NULL UNIQUE,
        target_olchiki_santhali TEXT NOT NULL,
        phonetic_deva_santhali TEXT NOT NULL,
        cached_audio_relative_path TEXT
    );
    """)
    
    # 2. Trie / B-Tree Indexes for Instant Sub-15ms Latency
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_hindi_trie ON fln_lexicon(source_hindi_normalized);")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_domain ON fln_lexicon(domain);")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_grade ON fln_lexicon(nipun_target_grade);")
    
    # 3. Seed / Insert Verified Santhali FLN Records
    insert_count = 0
    for r in records:
        source_norm = unicodedata.normalize("NFC", r["source_hindi_normalized"]).strip()
        audio_path = r.get("cached_audio_relative_path") or f"audio_cache/{r['id'].lower()}.wav"
        
        cursor.execute("""
        INSERT INTO fln_lexicon (
            id, domain, nipun_target_grade, source_hindi_normalized,
            target_olchiki_santhali, phonetic_deva_santhali,
            cached_audio_relative_path
        ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """, (
            r["id"],
            r["domain"],
            r["nipun_target_grade"],
            source_norm,
            r["target_olchiki_santhali"],
            r["phonetic_deva_santhali"],
            audio_path
        ))
        insert_count += 1
        
    conn.commit()
    
    # Verification query
    cursor.execute("SELECT COUNT(*) FROM fln_lexicon;")
    total_in_db = cursor.fetchone()[0]
    
    cursor.execute("SELECT domain, COUNT(*) FROM fln_lexicon GROUP BY domain;")
    domain_counts = cursor.fetchall()
    
    conn.close()
    
    print(f"Database successfully compiled at: {db_file}")
    print(f"Total verified Santhali FLN records in DB: {total_in_db}")
    print("\nDomain breakdown:")
    for dom, count in domain_counts:
        print(f"  - {dom:<25}: {count} records")
        
    return total_in_db

def test_fast_path_lookup(query_text: str, db_file: str = DB_PATH):
    norm_query = unicodedata.normalize("NFC", query_text).strip()
    
    start = time.perf_counter()
    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()
    
    cursor.execute("""
    SELECT id, domain, nipun_target_grade, target_olchiki_santhali, phonetic_deva_santhali, cached_audio_relative_path 
    FROM fln_lexicon 
    WHERE source_hindi_normalized = ?
    """, (norm_query,))
    row = cursor.fetchone()
    conn.close()
    elapsed_ms = (time.perf_counter() - start) * 1000
    
    if row:
        print(f"\n[QUERY HIT in {elapsed_ms:.3f} ms]: '{query_text}'")
        print(f"  ID:          {row[0]} ({row[1]} | {row[2]})")
        print(f"  Ol Chiki:    {row[3]}")
        print(f"  Phonetic:    {row[4]}")
        print(f"  Audio Path:  {row[5]}")
        return True
    else:
        print(f"\n[QUERY MISS in {elapsed_ms:.3f} ms]: '{query_text}' (Routing to Tier-2 Neural Fallback)")
        return False

if __name__ == "__main__":
    build_sqlite_lexicon()
    
    # Run test lookups across different FLN domains
    print("\n--- Running Tier-1 Fast-Path Santhali Benchmark Lookups ---")
    test_fast_path_lookup("अपनी जगह पर बैठो")
    test_fast_path_lookup("यह महुआ का पेड़ है")
    test_fast_path_lookup("मेरी माँ मुझे प्यार करती है")
    test_fast_path_lookup("अपने सिर को छुओ")
    test_fast_path_lookup("यह क्या है?")
    test_fast_path_lookup("शाबाश, बहुत बढ़िया!")
    test_fast_path_lookup("छह से दस तक गिनो")
    test_fast_path_lookup("यह वाक्य डेटाबेस में नहीं है")
