# Vernacular Pedagogy: Master Task Tracker

**Project:** Edge-Native Hindi to Santhali (Ol Chiki `sat_Olck`) Voice & Translation Pipeline  
**Target Architecture:** Android 9+, $\le$ 2 GB Physical RAM, Fully Offline Execution  
**Budget Guardrails:** Latency $\le$ 3.0s, Peak RAM $\le$ 295 MB, Zero heavy local compute  

---

## Phase 0: Ground Truth FLN Database & SQLite Fast-Path
- [x] **Task 0.1:** Define canonical linguistic schema for Santhali (`sat_Olck`) across 7 primary FLN classroom domains.
- [x] **Task 0.2:** Clean, normalize, and validate 368 gold-standard entries (200 conversational + 168 atomic vocabulary flashcards).
- [x] **Task 0.3:** Compile B-Tree indexed SQLite database (`assets/fln_lexicon.sqlite`) with sub-millisecond lookup (<0.1ms).
- [x] **Task 0.4:** Build automated verification script (`scripts/verify_fln_data.py`).

---

## Phase 1: Corpus Preprocessing & Authentic Bitext Ingestion
- [x] **Task 1.1:** Build semantically constrained NIPUN Bharat slot-filling engine (`scripts/03_bitext_normalizer.py`).
- [x] **Task 1.2:** Eliminate awkward/nonsensical combinations (strictly realistic schoolbag items, natural color-noun bindings, natural counting).
- [x] **Task 1.3:** Build AI4Bharat BPCC-Human & IN22 parallel corpus fetcher (`scripts/01_fetch_bpcc_bitext.py`).
- [x] **Task 1.4:** Generate verified training splits (`data/processed/bitext/train.tsv` [466 rows] and `val.tsv` [52 rows]).
- [x] **Task 1.5:** Build Mozilla Common Voice Santali audio preprocessor & Piper TTS formatter (`scripts/02_audio_common_voice_prep.py`).
- [x] **Task 1.6:** Configure secure git-ignored `.env` file for Hugging Face authentication.

---

## Cloud Compute & Colab CLI Infrastructure
- [x] **Task C.1:** Install `google-colab-cli` in WSL Ubuntu environment (Python 3.12).
- [x] **Task C.2:** Authenticate `colab-cli` with Google OAuth (`ashrafahm03@gmail.com`).
- [x] **Task C.3:** Patch upstream `jupyter_kernel_client` attribute compatibility.
- [x] **Task C.4:** Register `colab-mcp` in Antigravity global configuration (`C:\Users\Ashraf\.gemini\config\mcp_config.json`).
- [x] **Task C.5:** Provision remote Google Colab Tesla T4 GPU cloud session (`phase2-train`).
- [x] **Task C.6:** Verify remote CUDA execution on Tesla T4 via `scripts/00_test_colab_connection.py`.

---

## Phase 2: Neural Machine Translation (IndicTrans2 320M LoRA)
- [x] **Task 2.1:** Create autonomous cloud training worker (`scripts/run_phase2_cloud_train.py`).
- [x] **Task 2.2:** Execute remote LoRA fine-tuning on provisioned Tesla T4 GPU instance (`phase2-train`).
  - [x] Pull and merge AI4Bharat BPCC + IN22 bitext with FLN database (466 train, 52 val).
  - [x] Run 3 epochs LoRA fine-tuning on attention projection layers (`q_proj`, `v_proj`, `k_proj`, `out_proj`) [Train Loss: 3.08, Val Loss: 2.90].
  - [x] Merge LoRA adapter weights with base model (`/content/indictrans2_sat_merged`).
  - [x] Quantize merged model to **CTranslate2 INT8** with dual asymmetric vocabularies (src=122,706, tgt=122,672) and embedding normalization.
  - [x] Package `/content/indictrans2_sat_int8_ct2.tar.gz` (286.7 MB).
- [x] **Task 2.3:** Download INT8 model package from Colab VM into local `models/mt/indictrans2_sat_int8_ct2.tar.gz`.
- [x] **Task 2.4:** Verify archive integrity and CTranslate2 model manifest (`model.bin`, `config.json`, vocabularies).

---

## Phase 3: Voice Synthesis (Piper TTS VITS Architecture)
- [x] **Task 3.1:** Build Santhali speech corpus fetcher and preprocessor (`scripts/04_fetch_santhali_audio.py`) supporting AI4Bharat IndicVoices-R & Common Voice with 16 kHz Mono WAV standardization.
- [x] **Task 3.2:** Build autonomous cloud training worker (`scripts/run_phase3_cloud_train.py`) and update Colab notebook (`notebooks/colab_phase3_piper_tts.ipynb`) with deterministic Ol Chiki character alignment (`--phoneme-type text`).
- [x] **Task 3.3:** Build local cloud orchestrator (`scripts/launch_phase3_on_colab.py`) for automated Colab T4 GPU execution and artifact download.
- [x] **Task 3.4:** Build local ONNX CPU verification and latency benchmark harness (`scripts/verify_tts.py`).
- [ ] **Task 3.5:** Execute training on Colab Tesla T4 GPU, export `sat_piper_model.onnx` (~30 MB), and verify FLN audio synthesis.

---

## Phase 4: Offline Android Runtime & End-to-End Latency Verification
- [ ] **Task 4.1:** Build end-to-end local inference orchestration harness (`scripts/verify_pipeline.py`).
  - Silero VAD audio stream segmenter.
  - Vosk / Sherpa-ONNX streaming Hindi ASR.
  - Hybrid translation router (SQLite Fast-Path $\to$ CTranslate2 INT8 Fallback).
  - Piper TTS ONNX audio generation.
- [ ] **Task 4.2:** Measure total spoken voice translation latency ($\le$ 3.0s budget).
- [ ] **Task 4.3:** Verify memory consumption under Android Low Memory Killer budget ($\le$ 295 MB).
- [ ] **Task 4.4:** Generate Android deployment assets and final documentation walkthrough.
