# -*- coding: utf-8 -*-
"""
Phase 4: Offline Hindi ASR & Voice Activity Detection Engine
Provides zero-internet, edge-native speech-to-text for Hindi classroom audio:
1. Silero VAD (ONNX): Fast energy-based silence detection & trimming.
2. Vosk Hindi ASR (Kaldi): Offline Hindi speech recognition (16 kHz Mono).
"""

import os
import sys
import json
import wave
import numpy as np

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODELS_ASR = os.path.join(BASE_DIR, "models", "asr")
VOSK_MODEL_DIR = os.path.join(MODELS_ASR, "vosk-model-small-hi-0.22")
SILERO_VAD_PATH = os.path.join(MODELS_ASR, "silero_vad.onnx")

class OfflineHindiASR:
    """Edge-native offline Hindi Automatic Speech Recognition engine."""

    def __init__(self, model_dir: str = VOSK_MODEL_DIR, vad_path: str = SILERO_VAD_PATH):
        self.model_dir = model_dir
        self.vad_path = vad_path
        self.model = None
        self.vad_session = None
        self._init_engine()

    def _init_engine(self):
        # 1. Initialize Vosk ASR
        if os.path.exists(self.model_dir):
            try:
                import vosk
                vosk.SetLogLevel(-1) # Suppress Kaldi verbose output
                self.model = vosk.Model(self.model_dir)
                print(f"[ASR ENGINE] Vosk Hindi model initialized from {self.model_dir}")
            except Exception as e:
                print(f"[ASR WARN] Could not load Vosk model: {e}")
        else:
            print(f"[ASR WARN] Vosk model directory not found at {self.model_dir}")

        # 2. Initialize Silero VAD
        if os.path.exists(self.vad_path):
            try:
                import onnxruntime as ort
                opts = ort.SessionOptions()
                opts.intra_op_num_threads = 1
                opts.inter_op_num_threads = 1
                self.vad_session = ort.InferenceSession(self.vad_path, sess_options=opts, providers=["CPUExecutionProvider"])
                print(f"[ASR ENGINE] Silero VAD ONNX initialized from {self.vad_path}")
            except Exception as e:
                print(f"[ASR WARN] Could not load Silero VAD: {e}")

    def is_speech_active(self, audio_chunk_16k_float: np.ndarray, threshold: float = 0.5) -> bool:
        """
        Evaluate if a 512-sample (32ms) 16kHz audio chunk contains active speech.
        """
        if self.vad_session is None:
            return True # Fallback if VAD not initialized
        
        try:
            if len(audio_chunk_16k_float) < 512:
                # Pad to 512 samples
                audio_chunk_16k_float = np.pad(audio_chunk_16k_float, (0, 512 - len(audio_chunk_16k_float)))
            elif len(audio_chunk_16k_float) > 512:
                audio_chunk_16k_float = audio_chunk_16k_float[:512]

            input_data = np.expand_dims(audio_chunk_16k_float.astype(np.float32), axis=0) # [1, 512]
            sr = np.array(16000, dtype=np.int64)
            h = np.zeros((2, 1, 64), dtype=np.float32)
            c = np.zeros((2, 1, 64), dtype=np.float32)

            inputs = {
                "input": input_data,
                "sr": sr,
                "h": h,
                "c": c
            }
            out = self.vad_session.run(None, inputs)
            prob = out[0][0][0]
            return float(prob) >= threshold
        except Exception:
            return True

    def transcribe_wav(self, wav_path: str) -> str:
        """
        Transcribes a 16 kHz Mono WAV audio file into Hindi Unicode text.
        """
        if self.model is None:
            raise RuntimeError("Vosk model not initialized.")

        import vosk
        wf = wave.open(wav_path, "rb")
        if wf.getnchannels() != 1 or wf.getsampwidth() != 2 or wf.getframerate() != 16000:
            print(f"[ASR WARN] Audio is not 16kHz Mono 16-bit PCM. Channels: {wf.getnchannels()}, Rate: {wf.getframerate()}")
            # We can still proceed if Kaldi can read it
        
        rec = vosk.KaldiRecognizer(self.model, wf.getframerate())
        rec.SetWords(True)

        full_text = []
        while True:
            data = wf.readframes(4000)
            if len(data) == 0:
                break
            if rec.AcceptWaveform(data):
                res = json.loads(rec.Result())
                if "text" in res and res["text"]:
                    full_text.append(res["text"])

        final_res = json.loads(rec.FinalResult())
        if "text" in final_res and final_res["text"]:
            full_text.append(final_res["text"])

        wf.close()
        transcription = " ".join(full_text).strip()
        return transcription

    def transcribe_bytes(self, pcm_bytes: bytes, sample_rate: int = 16000) -> str:
        """
        Transcribes raw PCM 16-bit integer bytes into Hindi Unicode text.
        """
        if self.model is None:
            raise RuntimeError("Vosk model not initialized.")

        import vosk
        rec = vosk.KaldiRecognizer(self.model, sample_rate)
        rec.AcceptWaveform(pcm_bytes)
        final_res = json.loads(rec.FinalResult())
        return final_res.get("text", "").strip()


if __name__ == "__main__":
    print("Testing Offline Hindi ASR Engine Initialization...")
    engine = OfflineHindiASR()
    if engine.model:
        print("[OK] ASR Engine is ready for live offline Hindi transcription!")
    if engine.vad_session:
        print("[OK] Silero VAD is ready for classroom noise gating!")
