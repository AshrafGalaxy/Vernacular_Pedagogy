"""
Offline Asset Compression & Standardization Pipeline for Flashcard Motifs.
Transforms 150 raw high-resolution images in Assets_Flash_Cards into
lightweight, aspect-ratio-preserved WebP assets for Android runtime.
"""

import os
import re
import json
from PIL import Image

SOURCE_DIR = "Assets_Flash_Cards"
TARGET_DIR = os.path.join("android", "app", "src", "main", "assets", "images", "flashcards")
MANIFEST_PATH = os.path.join("data", "processed", "motif_assets_manifest.json")
MAX_DIM = 800
WEBP_QUALITY = 85

def clean_motif_id(filename: str) -> str:
    base = re.sub(r'(\.png|\.jpg|\.jpeg)+$', '', filename, flags=re.IGNORECASE)
    # Ensure starts with motif_
    if not base.startswith("motif_"):
        base = "motif_" + base
    return base

def main():
    if not os.path.exists(SOURCE_DIR):
        raise FileNotFoundError(f"Source directory '{SOURCE_DIR}' not found!")
    
    os.makedirs(TARGET_DIR, exist_ok=True)
    os.makedirs(os.path.dirname(MANIFEST_PATH), exist_ok=True)
    
    files = sorted(os.listdir(SOURCE_DIR))
    print(f"Discovered {len(files)} files in '{SOURCE_DIR}'...")
    
    manifest = []
    total_original_bytes = 0
    total_compressed_bytes = 0
    
    for idx, fname in enumerate(files, 1):
        src_path = os.path.join(SOURCE_DIR, fname)
        if not os.path.isfile(src_path):
            continue
            
        orig_size = os.path.getsize(src_path)
        total_original_bytes += orig_size
        
        motif_id = clean_motif_id(fname)
        target_filename = f"{motif_id}.webp"
        target_path = os.path.join(TARGET_DIR, target_filename)
        
        with Image.open(src_path) as img:
            orig_w, orig_h = img.size
            aspect_ratio = round(orig_w / orig_h, 4)
            
            # Format mode
            if img.mode in ("RGBA", "LA") or (img.mode == "P" and "transparency" in img.info):
                img_proc = img.convert("RGBA")
            else:
                img_proc = img.convert("RGB")
                
            # Resize if exceeding max bounding box
            if max(orig_w, orig_h) > MAX_DIM:
                img_proc.thumbnail((MAX_DIM, MAX_DIM), Image.Resampling.LANCZOS)
                
            comp_w, comp_h = img_proc.size
            
            # Save WebP
            img_proc.save(
                target_path,
                format="WEBP",
                quality=WEBP_QUALITY,
                method=6
            )
            
        comp_size = os.path.getsize(target_path)
        total_compressed_bytes += comp_size
        reduction_pct = (1.0 - (comp_size / orig_size)) * 100.0 if orig_size > 0 else 0.0
        
        entry = {
            "id": motif_id,
            "filename": target_filename,
            "asset_path": f"images/flashcards/{target_filename}",
            "original_file": fname,
            "original_width": orig_w,
            "original_height": orig_h,
            "compressed_width": comp_w,
            "compressed_height": comp_h,
            "aspect_ratio": aspect_ratio,
            "original_size_kb": round(orig_size / 1024.0, 2),
            "compressed_size_kb": round(comp_size / 1024.0, 2),
            "reduction_pct": round(reduction_pct, 1)
        }
        manifest.append(entry)
        
        if idx % 25 == 0 or idx == len(files):
            print(f"[{idx:3d}/{len(files)}] Processed '{fname}' -> '{target_filename}' ({entry['compressed_size_kb']} KB, -{entry['reduction_pct']}%)")
            
    with open(MANIFEST_PATH, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2, ensure_ascii=False)
        
    orig_mb = total_original_bytes / (1024.0 * 1024.0)
    comp_mb = total_compressed_bytes / (1024.0 * 1024.0)
    total_reduction = (1.0 - (total_compressed_bytes / total_original_bytes)) * 100.0
    
    print("\n================ COMPRESSION SUMMARY ================")
    print(f"Total files processed: {len(manifest)}")
    print(f"Original size:         {orig_mb:.2f} MB")
    print(f"Compressed WebP size:  {comp_mb:.2f} MB")
    print(f"Total size reduction:  {total_reduction:.2f}%")
    print(f"Manifest written to:   {MANIFEST_PATH}")
    print(f"Assets deployed to:    {TARGET_DIR}")
    print("=====================================================\n")

if __name__ == "__main__":
    main()
