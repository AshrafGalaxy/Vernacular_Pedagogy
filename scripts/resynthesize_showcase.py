# -*- coding: utf-8 -*-
"""
Re-synthesize all 16 Unseen Dynamic Sentences using the newly fine-tuned 24-epoch Piper TTS model.
"""
import os
import sys
import json
import time
import shutil
import numpy as np
from scipy.io import wavfile

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

BASE_DIR = r"c:\Users\Ashraf\Desktop\26042"
MODELS_TTS = os.path.join(BASE_DIR, "models", "tts")
ONNX_PATH = os.path.join(MODELS_TTS, "sat_piper_model.onnx")
CONFIG_PATH = os.path.join(MODELS_TTS, "sat_piper_model.onnx.json")

OUTPUT_DIR = os.path.join(BASE_DIR, "assets", "demo_audio", "unseen_dynamic")
ARTIFACT_DIR = r"C:\Users\Ashraf\.gemini\antigravity-ide\brain\22f4a990-e354-4022-98a5-322e94d941d0\audio"

os.makedirs(OUTPUT_DIR, exist_ok=True)
os.makedirs(ARTIFACT_DIR, exist_ok=True)

sys.path.insert(0, BASE_DIR)
from scripts.santhali_phonemizer import santhali_to_ipa
import onnxruntime as ort

ITEMS = [
    (1, "Cooperative Learning", "आज हम सब मिलकर एक नया खेल खेलेंगे।", "ᱛᱮᱦᱮᱧ ᱵᱚ ᱥᱟᱱᱟᱢ ᱠᱚ ᱢᱮᱥᱟ ᱠᱟᱛᱮ ᱢᱤᱫᱴᱟᱝ ᱱᱟᱣᱟ ᱠᱷᱮᱞᱚᱸᱰᱟᱵᱚ"),
    (2, "Incentive & Praise", "जो बच्चा सबसे पहले उत्तर देगा उसे शाबाशी मिलेगी।", "ᱜᱤᱫᱽᱨᱟᱹ ᱫᱚ ᱯᱳᱭᱞᱳ ᱥᱟᱨᱦᱟᱣᱮ ᱧᱟᱢᱟ ᱫᱚ ᱩᱛᱛᱚᱨᱮ ᱮᱢᱟ"),
    (3, "Environment", "कक्षा के बाहर बहुत तेज़ बारिश हो रही है।", "ᱠᱞᱟᱥ ᱵᱟᱦᱨᱮ ᱨᱮ ᱟᱹᱰᱤ ᱞᱚᱜᱚᱱ ᱫᱟᱜᱡᱩᱠᱤ ᱦᱩᱭᱩᱜ ᱠᱟᱱᱟ"),
    (4, "Social Empathy", "अपनी पेंसिल और रबर अपने दोस्त के साथ साझा करो।", "ᱟᱢᱟᱜ ᱜᱟᱛᱮ ᱟᱨ ᱨᱵᱟᱨ ᱯᱮᱱᱥᱤᱞ ᱥᱟᱞᱟᱜ ᱦᱟᱹᱴᱤᱧ ᱢᱮ"),
    (5, "Cleanliness", "कागज़ को फाड़कर फर्श पर मत फेंको।", "ᱚᱛ ᱨᱮ ᱠᱟᱜᱚᱡᱽ ᱜᱤᱰᱤ ᱠᱟᱛᱮ ᱜᱤᱰᱤ ᱯᱮ"),
    (6, "Directions & Science", "सूरज पूर्व दिशा से उगता है और शाम को पश्चिम में ढलता है।", "ᱥᱤᱸᱜᱟᱹᱲ ᱨᱮ ᱯᱚᱪᱷᱤᱢ ᱥᱮᱫ ᱛᱮ ᱨᱟᱠᱟᱵᱚᱜᱼᱟ ᱟᱨ ᱯᱩᱨᱩᱵ ᱥᱮᱫ ᱛᱮ ᱟᱲᱩᱜᱟ"),
    (7, "Nature & Geography", "नदी के किनारे बहुत सारे हरे भरे पेड़ हैं।", "ᱜᱟᱰᱟ ᱨᱮᱭᱟᱜ ᱟᱭᱢᱟ ᱫᱟᱨᱮ ᱥᱩᱨ ᱨᱮ ᱯᱮᱨᱮᱡ ᱟᱠᱟᱱᱟ"),
    (8, "Astronomy", "रात के समय आसमान में चमकते हुए सितारे दिखाई देते हैं।", "ᱧᱤᱫᱟᱹ ᱨᱮ ᱚᱛᱚᱢ ᱨᱮ ᱛᱟᱨᱩᱵ ᱧᱮᱞ ᱧᱟᱢᱚᱜ ᱠᱟᱱᱟ"),
    (9, "Health & Hygiene", "खाना खाने से पहले हमेशा अपने हाथ साबुन से अच्छी तरह धोएं।", "ᱡᱚᱢ ᱞᱟᱦᱟᱨᱮ ᱥᱟᱱᱟᱢ ᱚᱠᱛᱚ ᱟᱢᱟᱜ ᱛᱤ ᱱᱟᱯᱟᱭ ᱛᱮ ᱥᱟᱯᱷᱟᱭ ᱢᱮ"),
    (10, "Wellness", "साफ़ पानी पीने से हमारा शरीर स्वस्थ रहता है।", "ᱟᱢᱟᱜ ᱦᱚᱲᱢᱚ ᱫᱚ ᱱᱟᱯᱟᱭ ᱫᱟᱜ ᱛᱮ ᱥᱟᱯᱷᱟᱭ ᱛᱟᱦᱮᱸᱱᱟ"),
    (11, "Civic Safety", "सड़क पार करते समय हमेशा दोनों तरफ़ देखना चाहिए।", "ᱵᱟᱱᱟᱨ ᱰᱟᱦᱟᱨ ᱯᱟᱨᱚᱢ ᱠᱟᱛᱮ ᱡᱟᱣᱜᱮ ᱧᱮᱞ ᱦᱩᱭᱩᱜ ᱛᱟᱢᱟ"),
    (12, "Arithmetic Reasoning", "यदि तुम्हारे पास चार सेब हैं और तुमने दो खा लिए तो कितने बचे?", "ᱟᱢᱟᱜ ᱥᱩᱨ ᱨᱮ ᱛᱤᱱᱟᱹᱜ ᱯᱩᱱ ᱥᱮᱣ ᱢᱮᱱᱟᱜᱟ ᱟᱨ ᱟᱢ ᱴᱷᱮᱱ ᱵᱟᱨ ᱮᱢ ᱫᱚᱦᱚ ᱟᱠᱟᱫᱟ"),
    (13, "Physics / Observation", "गोल गेंद ज़मीन पर बहुत तेज़ी से लुढ़कती है।", "ᱵᱚᱞ ᱫᱚ ᱟᱹᱰᱤ ᱞᱚᱜᱚᱱ ᱚᱛ ᱨᱮ ᱜᱩᱞᱟᱹᱭᱮ ᱮᱥᱮᱫᱟ"),
    (14, "Encouragement", "गलतियाँ करने से मत डरो, गलतियों से ही हम नया सीखते हैं।", "ᱱᱟᱶᱟ ᱞᱟᱹᱞᱤᱥ ᱛᱮ ᱱᱟᱣᱟ ᱠᱟᱹᱢᱤ ᱠᱚᱨᱟᱣ ᱫᱚ ᱟᱞᱚᱢ ᱥᱮᱪᱮᱫᱟᱵᱚ"),
    (15, "Moral Values", "हमेशा दूसरों की मदद करनी चाहिए और सच बोलना चाहिए।", "ᱮᱴᱟᱜ ᱠᱚ ᱡᱟᱣᱜᱮ ᱥᱟᱡᱟᱹᱭ ᱞᱟᱹᱭᱟ ᱟᱨ ᱜᱚᱲᱚ ᱟᱭ ᱢᱮ"),
    (16, "School Attendance", "कल सुबह सभी बच्चे समय पर विद्यालय पहुँचें।", "ᱦᱚᱭᱦᱩᱫᱮ ᱥᱟᱱᱟᱢ ᱜᱤᱫᱽᱨᱟᱹ ᱚᱠᱛᱚ ᱛᱮ ᱵᱤᱨᱫᱟᱹᱜᱟᱲ ᱨᱮ ᱥᱮᱴᱮᱨᱚᱜᱼᱟ"),
]

def main():
    print("=" * 70)
    print("SYNTHESIZING 16 UNSEEN DYNAMIC SENTENCES (24-EPOCH PIPER TTS)")
    print("=" * 70)

    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        cfg = json.load(f)

    sample_rate = cfg.get("audio", {}).get("sample_rate", 16000)
    ph_map = cfg.get("phoneme_id_map", {})
    noise_scale = float(cfg.get("inference", {}).get("noise_scale", 0.667))
    length_scale = float(cfg.get("inference", {}).get("length_scale", 1.0))
    noise_w = float(cfg.get("inference", {}).get("noise_w", 0.8))

    pad = ph_map.get('_', [0])[0] if isinstance(ph_map.get('_'), list) else ph_map.get('_', 0)
    bos = ph_map.get('^', [1])[0] if isinstance(ph_map.get('^'), list) else ph_map.get('^', 1)
    eos = ph_map.get('$', [2])[0] if isinstance(ph_map.get('$'), list) else ph_map.get('$', 2)

    session = ort.InferenceSession(ONNX_PATH, providers=["CPUExecutionProvider"])

    for idx, cat, hi, sat in ITEMS:
        fname = f"unseen_dynamic_{idx:02d}.wav"
        out_path = os.path.join(OUTPUT_DIR, fname)
        art_path = os.path.join(ARTIFACT_DIR, fname)

        ipa = santhali_to_ipa(sat)
        ids = [bos]
        for c in ipa:
            if c in ph_map:
                v = ph_map[c]
                if isinstance(v, list):
                    ids.extend(v)
                else:
                    ids.append(v)
                ids.append(pad)
            elif c.isspace():
                sp = ph_map.get(" ", ph_map.get("_", 0))
                if isinstance(sp, list):
                    ids.extend(sp)
                else:
                    ids.append(sp)
        ids.append(eos)

        phoneme_ids = np.array(ids, dtype=np.int64)[None, :]
        lengths = np.array([phoneme_ids.shape[1]], dtype=np.int64)
        scales = np.array([noise_scale, length_scale, noise_w], dtype=np.float32)

        t0 = time.perf_counter()
        audio = session.run(None, {
            "input": phoneme_ids,
            "input_lengths": lengths,
            "scales": scales
        })[0].squeeze()
        dur_ms = (time.perf_counter() - t0) * 1000

        audio_dur = len(audio) / float(sample_rate)
        rtf = (dur_ms / 1000.0) / audio_dur if audio_dur > 0 else 0.0
        rms = float(np.sqrt(np.mean(audio**2)))

        audio_int16 = np.clip(audio * 32767.0, -32768, 32767).astype(np.int16)
        wavfile.write(out_path, sample_rate, audio_int16)
        shutil.copy2(out_path, art_path)

        print(f"[{idx:02d}] {cat:<22} | Synth: {dur_ms:5.1f}ms | Dur: {audio_dur:4.2f}s | RTF: {rtf:5.3f} | RMS: {rms:5.3f} -> {fname}")

    print("=" * 70)
    print("ALL 16 DYNAMIC SENTENCES RESYNTHESIZED SUCCESSFULLY!")
    print(f"Saved to: {OUTPUT_DIR}")
    print(f"Copied to: {ARTIFACT_DIR}")

if __name__ == "__main__":
    main()
