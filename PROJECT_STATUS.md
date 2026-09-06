# Project Status & Execution Matrix: Hindi-to-Santhali Vernacular Pedagogy

Last Updated: Phase 1 Completion

---

## 1. High-Level Status Overview

| Component | Status | Execution Location | User Manual Work Required? |
| :--- | :--- | :--- | :--- |
| **FLN Database (368 records)** | **100% COMPLETE** | Local Workstation | **NONE** (Verified & Locked) |
| **SQLite Fast-Path Index (<0.1ms)** | **100% COMPLETE** | Local Workstation | **NONE** (`assets/fln_lexicon.sqlite` ready) |
| **Pedagogical Bitext Preprocessor** | **100% COMPLETE** | Local Workstation | **NONE** (`train.tsv` / `val.tsv` clean) |
| **Common Voice Audio Preprocessor** | **100% COMPLETE** | Local / Cloud | **NONE** (`scripts/02_audio_common_voice_prep.py` ready) |
| **AI4Bharat Ingestion Pipeline** | **100% COMPLETE** | Google Colab | **NONE** (Ingested & Processed) |
| **IndicTrans2 LoRA MT & INT8** | **100% COMPLETE** | Google Colab (T4) | **NONE** (`models/mt/indictrans2_sat_int8_ct2.tar.gz` ready) |
| **Piper TTS Cloud Notebook** | **100% CODE READY** | Google Colab (T4) | **Next Step: Run Notebook for Voice Bank Training** |
| **Android Asset Packaging** | **PENDING** | Local Workstation | **Waiting for TTS ONNX Voice Export** |

---

## 2. What Is 100% Completed (No Action Needed)

1. **Phase 0 Database:**
   - 368 verified Grade 1–3 Santhali (`sat_Olck`) records across 15 domains.
   - Compiled into high-speed B-Tree SQLite database (`assets/fln_lexicon.sqlite`).
   - Benchmark latency: **0.02 ms – 0.4 ms** per classroom command.

2. **Phase 1 Bitext Normalization & Pedagogical Dataset:**
   - Cleaned and semantically constrained sentence generator (`scripts/03_bitext_normalizer.py`).
   - Eradicated all nonsensical pairings (no "taking chairs out of bags" or "blue papayas").
   - Normalized splits ready at `data/processed/bitext/train.tsv` (466 rows) and `val.tsv` (52 rows).

3. **Phase 2 Neural Machine Translation (LoRA + CTranslate2 INT8):**
   - Fine-tuned IndicTrans2 320M (`hin_Deva` $\rightarrow$ `sat_Olck`) on Colab Tesla T4 GPU (Train Loss: 3.080, Val Loss: 2.904).
   - Merged LoRA adapters into base weights.
   - Quantized to CTranslate2 INT8 format with dual asymmetric SentencePiece vocabularies (`model.bin` ~325 MB).
   - Exported and verified archive locally at `models/mt/indictrans2_sat_int8_ct2.tar.gz` (286.7 MB).
   - Full technical report available at [`docs/PHASE2_EXECUTION_AND_OPTIMIZATION_REPORT.md`](file:///c:/Users/Ashraf/Desktop/26042/docs/PHASE2_EXECUTION_AND_OPTIMIZATION_REPORT.md).

4. **Self-Contained Cloud Notebooks & Cloud Scripts:**
   - `notebooks/colab_phase1_audio_prep.ipynb`
   - `notebooks/colab_phase2_indictrans2_lora.ipynb`
   - `notebooks/colab_phase3_piper_tts.ipynb`
   - `scripts/run_phase2_cloud_train.py`
   - `scripts/launch_phase2_on_colab.py`

---

## 3. What Needs Manual Action (User Checklist)

### Action 1: Ingest Common Voice Santali Audio (For TTS Voice Bank)
- **Why:** Audio clips must be standardized to 16 kHz Mono WAV before training Piper TTS.
- **How to do it:**
  1. Download Mozilla Common Voice Santali v26.0 from [Mozilla Data Collective](https://mozilladatacollective.com/datasets/cmqie985k00cbnr07z9cea5wy) or upload your existing clips to Google Drive.
  2. Open [`notebooks/colab_phase1_audio_prep.ipynb`](https://colab.research.google.com/github/AshrafGalaxy/Vernacular_Pedagogy/blob/main/notebooks/colab_phase1_audio_prep.ipynb) in Colab and execute it to generate `santali_piper_voicebank_16k.tar.gz`.

### Action 2: Run Phase 3 Piper TTS Fine-Tuning in Google Colab (ACTIVE STEP)
- **Why:** Training Piper TTS VITS architecture requires a GPU (T4 on Google Colab).
- **How to do it:**
  1. Open [`notebooks/colab_phase3_piper_tts.ipynb`](https://colab.research.google.com/github/AshrafGalaxy/Vernacular_Pedagogy/blob/main/notebooks/colab_phase3_piper_tts.ipynb) on Colab.
  2. Ingest the prepared voicebank from Action 1.
  3. Fine-tune Piper VITS and export the ONNX model package (`sat_piper_model.onnx` ~30 MB).
  4. Download the ONNX model into `models/tts/`.

---

## 4. Immediate Next Step

Proceed to **Phase 3: Voice Synthesis (Piper TTS VITS Architecture)**. All machine translation assets are compiled, quantized, and ready.
