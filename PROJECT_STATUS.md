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
| **AI4Bharat Ingestion Pipeline** | **100% CODE READY** | Google Colab | **1-Minute Step** (Optional HF Token) |
| **IndicTrans2 LoRA Cloud Notebook** | **100% CODE READY** | Google Colab (T4) | **Run Notebook** (Click "Run All") |
| **Piper TTS Cloud Notebook** | **100% CODE READY** | Google Colab (T4) | **Run Notebook** (Click "Run All") |
| **Android Asset Packaging** | **PENDING** | Local Workstation | **Waiting for Model Weights Export** |

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

3. **Audio Preprocessor & Formatters:**
   - `scripts/02_audio_common_voice_prep.py` tested and verified for Piper TTS LJSpeech formatting (`clip_id|transcript`) with 16 kHz Mono WAV batch transcoding.

4. **Self-Contained Cloud Notebooks:**
   - `notebooks/colab_phase1_audio_prep.ipynb`
   - `notebooks/colab_phase2_indictrans2_lora.ipynb`
   - `notebooks/colab_phase3_piper_tts.ipynb`

---

## 3. What Needs Manual Action (User Checklist)

To train the models without straining your local CPU/RAM, the following external actions require user involvement:

### Action 1: Optional Hugging Face Token (For Gated BPCC-Human)
- **Why:** `ai4bharat/BPCC` on Hugging Face is a gated repository requiring user agreement.
- **How to do it (Takes 1 minute):**
  1. Go to [https://huggingface.co/datasets/ai4bharat/BPCC](https://huggingface.co/datasets/ai4bharat/BPCC) and click **"Access repository"** to accept terms.
  2. Copy your User Access Token from [https://huggingface.co/settings/tokens](https://huggingface.co/settings/tokens).
  3. *(Note: If you don't have a token, the script will automatically fallback to the open `ai4bharat/IN22-Conv` dataset without requiring any login!)*

### Action 2: Run Phase 2 MT Fine-Tuning in Google Colab
- **Why:** Training IndicTrans2 320M requires a GPU (T4 or A100).
- **How to do it:**
  1. Open [`notebooks/colab_phase2_indictrans2_lora.ipynb`](https://colab.research.google.com/github/AshrafGalaxy/Vernacular_Pedagogy/blob/main/notebooks/colab_phase2_indictrans2_lora.ipynb) in Google Colab.
  2. Select **Runtime > Change runtime type > T4 GPU**.
  3. (Optional) Paste your Hugging Face token in Section 2.1 cell.
  4. Click **Runtime > Run all**.
  5. When complete, download the generated `indictrans2_sat_int8_ct2.tar.gz` (~65 MB) and place it in `models/mt/`.

### Action 3: Ingest Common Voice Santali Audio (For TTS Voice Bank)
- **Why:** Audio files must be standardized to 16 kHz Mono WAV before training Piper TTS.
- **How to do it:**
  1. Download Mozilla Common Voice Santali v26.0 from [Mozilla Data Collective](https://mozilladatacollective.com/datasets/cmqie985k00cbnr07z9cea5wy) or upload your existing clips to Google Drive.
  2. Open [`notebooks/colab_phase1_audio_prep.ipynb`](https://colab.research.google.com/github/AshrafGalaxy/Vernacular_Pedagogy/blob/main/notebooks/colab_phase1_audio_prep.ipynb) in Colab and execute it to generate `santali_piper_voicebank_16k.tar.gz`.

---

## 4. Immediate Next Step

You do **not** need to do anything locally right now. The immediate next step is:

1. **Option A (Proceed to Cloud Training):** You open [`colab_phase2_indictrans2_lora.ipynb`](https://colab.research.google.com/github/AshrafGalaxy/Vernacular_Pedagogy/blob/main/notebooks/colab_phase2_indictrans2_lora.ipynb) on Google Colab and run it to produce the quantized INT8 translation engine.
2. **Option B (Build Offline Android Runtime Scaffolding First):** While you or the cloud runs the model training in the background, we can build the complete offline Android inference wrapper and pipeline benchmark script (`scripts/verify_pipeline.py`) locally so that as soon as the model weights are downloaded, the system runs end-to-end immediately.
