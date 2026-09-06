# -*- coding: utf-8 -*-
"""
Phase 3 Verification: Piper TTS ONNX Inference & Latency Benchmark
Loads sat_piper_model.onnx using ONNXRuntime on CPU, synthesizes gold-standard
Grade 1-3 Santhali pedagogical commands, measures Real-Time Factor (RTF)
and latency budget (<= 800ms), and exports sample audio files.
"""

import os
import sys
import json
import time
import numpy as np

# Fix Windows console encoding
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODELS_TTS = os.path.join(BASE_DIR, "models", "tts")
ONNX_PATH = os.path.join(MODELS_TTS, "sat_piper_model.onnx")
CONFIG_PATH = os.path.join(MODELS_TTS, "sat_piper_model.onnx.json")
SAMPLES_DIR = os.path.join(MODELS_TTS, "samples")

GOLD_STANDARD_TESTS = [
    ("sit_down", "ᱫᱩᱲᱩᱵ ᱢᱮ", "Sit down (FLN Command)"),
    ("open_book", "ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ", "Open book (FLN Command)"),
    ("count", "ᱞᱮᱠᱷᱟᱭ ᱢᱮ", "Count (FLN Numeracy)"),
    ("praise", "ᱟᱹᱰᱤ ᱵᱷᱟᱹᱜᱤ", "Very good (FLN Praise)")
]


def text_to_phoneme_ids(text: str, phoneme_id_map: dict) -> list:
    """Maps Ol Chiki text characters to Piper phoneme IDs based on model configuration."""
    bos = phoneme_id_map.get("^", [1])[0] if isinstance(phoneme_id_map.get("^"), list) else phoneme_id_map.get("^", 1)
    eos = phoneme_id_map.get("$", [2])[0] if isinstance(phoneme_id_map.get("$"), list) else phoneme_id_map.get("$", 2)
    pad = phoneme_id_map.get("_", [0])[0] if isinstance(phoneme_id_map.get("_"), list) else phoneme_id_map.get("_", 0)

    ids = [bos]
    for char in text:
        if char in phoneme_id_map:
            val = phoneme_id_map[char]
            if isinstance(val, list):
                ids.extend(val)
            else:
                ids.append(val)
            ids.append(pad)  # Inter-character pad separator in Piper VITS
        elif char.isspace():
            space_val = phoneme_id_map.get(" ", phoneme_id_map.get("_", 0))
            if isinstance(space_val, list):
                ids.extend(space_val)
            else:
                ids.append(space_val)
    ids.append(eos)
    return ids


def save_wav(audio_data: np.ndarray, sample_rate: int, output_path: str):
    """Saves float32 audio array to 16-bit PCM WAV."""
    from scipy.io import wavfile
    audio_int16 = np.clip(audio_data * 32767.0, -32768, 32767).astype(np.int16)
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    wavfile.write(output_path, sample_rate, audio_int16)


def run_verification():
    print("=" * 65)
    print("Phase 3 Verification: Piper TTS ONNX Model & Latency Benchmark")
    print("=" * 65)

    if not os.path.exists(ONNX_PATH) or not os.path.exists(CONFIG_PATH):
        print(f"[STATUS] Model files not found yet in {MODELS_TTS}")
        print(f"  Expected ONNX:   {ONNX_PATH}")
        print(f"  Expected Config: {CONFIG_PATH}")
        print("\nPlease run 'python scripts/launch_phase3_on_colab.py' to train and download the model.")
        return False

    try:
        import onnxruntime as ort
    except ImportError:
        print("[ERROR] 'onnxruntime' is required. Install via: pip install onnxruntime scipy")
        return False

    # Load configuration
    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        config = json.load(f)

    sample_rate = config.get("audio", {}).get("sample_rate", 16000)
    phoneme_id_map = config.get("phoneme_id_map", {})
    noise_scale = config.get("inference", {}).get("noise_scale", 0.667)
    length_scale = config.get("inference", {}).get("length_scale", 1.0)
    noise_w = config.get("inference", {}).get("noise_w", 0.8)

    print(f"Model Configuration:")
    print(f"  Sample Rate:     {sample_rate} Hz")
    print(f"  Phoneme Symbols: {len(phoneme_id_map)}")
    print(f"  ONNX Size:       {os.path.getsize(ONNX_PATH)/(1024*1024):.1f} MB")

    # Initialize ONNX session
    session_options = ort.SessionOptions()
    session_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
    session_options.intra_op_num_threads = 2

    t_load_start = time.time()
    session = ort.InferenceSession(ONNX_PATH, sess_options=session_options, providers=["CPUExecutionProvider"])
    load_time_ms = (time.time() - t_load_start) * 1000
    print(f"  Model Load Time: {load_time_ms:.1f} ms\n")

    os.makedirs(SAMPLES_DIR, exist_ok=True)
    all_passed = True

    print(f"{'Phrase':<20} | {'Santhali':<16} | {'Synth Time':<10} | {'Audio Dur':<10} | {'RTF':<8} | {'Status'}")
    print("-" * 80)

    for tag, santhali_text, desc in GOLD_STANDARD_TESTS:
        phoneme_ids = text_to_phoneme_ids(santhali_text, phoneme_id_map)

        input_tensor = np.array([phoneme_ids], dtype=np.int64)
        input_lengths = np.array([len(phoneme_ids)], dtype=np.int64)
        scales = np.array([noise_scale, length_scale, noise_w], dtype=np.float32)

        inputs = {
            "input": input_tensor,
            "input_lengths": input_lengths,
            "scales": scales
        }

        t0 = time.time()
        outputs = session.run(None, inputs)
        infer_time_ms = (time.time() - t0) * 1000

        audio = outputs[0].squeeze()
        audio_dur_sec = len(audio) / float(sample_rate)
        rtf = (infer_time_ms / 1000.0) / audio_dur_sec if audio_dur_sec > 0 else 0.0

        # Save output WAV
        wav_out = os.path.join(SAMPLES_DIR, f"{tag}.wav")
        save_wav(audio, sample_rate, wav_out)

        # Budget check: synthesis latency <= 800ms, RTF <= 0.35
        passed = (infer_time_ms <= 800.0) and (rtf <= 0.35)
        if not passed:
            all_passed = False
        status_str = "PASS" if passed else "WARN"

        print(f"{tag:<20} | {santhali_text:<16} | {infer_time_ms:>7.1f} ms | {audio_dur_sec:>7.2f} s | {rtf:>6.3f} | [{status_str}]")

    print("-" * 80)
    print(f"Audio samples exported to: {SAMPLES_DIR}")
    if all_passed:
        print("[ALL PASS] Spoken voice synthesis is within real-time edge latency budget (<= 800 ms)!")
    return all_passed


if __name__ == "__main__":
    run_verification()
