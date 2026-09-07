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
- [x] **Task 3.5:** Execute training on Colab Tesla T4 GPU, export `sat_piper_model.onnx` (60.6 MB, RTF 0.04-0.08), and verify FLN audio synthesis.
- [x] **Task 3.6:** Publish verified models, SQLite database, and audio samples to Hugging Face Model Hub (`Ashraf01k/vernacular-pedagogy-santhali`).
- [x] **Task 3.7:** Perform hardware stress testing, CPU thread throttling (1, 2, 4 threads), and edge-case evaluation (`scripts/run_model_stress_tests.py`).

---

## Phase 4: Voice Domain Completion & Offline Hindi ASR Engine
- [ ] **Task 4.1:** Ingest and configure lightweight offline Hindi ASR engine:
  - Download & configure offline Hindi ASR (Sherpa-ONNX Zipformer / Vosk Hindi small / Whisper-tiny ONNX INT8 ~39 MB) into `models/asr/`.
  - Integrate Silero VAD ONNX (~2 MB) for classroom noise suppression and voice boundary detection.
  - Verify local CPU speech-to-text transcription latency (<400 ms).
- [ ] **Task 4.2:** Pristine FP32 $\to$ INT8 Re-quantization of IndicTrans2:
  - Re-export merged LoRA checkpoint on Colab using strict FP32 precision to eliminate CPU float16 underflow/overflow (zero NaNs).
  - Enable flawless open-ended sentence translation fallback outside the 368-entry FLN database.

---

## Phase 5: Judge Demonstration Harness & Unified Prototype
- [ ] **Task 5.1:** Build End-to-End Latency & Pipeline Integration Harness (`scripts/verify_pipeline.py`):
  - Audio Input $\to$ VAD $\to$ Hindi ASR $\to$ Hybrid Router (SQLite Fast-Path / CT2 MT) $\to$ Ol Chiki Text $\to$ Piper TTS $\to$ Audio Output.
- [ ] **Task 5.2:** Build Interactive Live Demonstration UI (Local Browser / GUI Prototype):
  - **Live Microphone Input**: Real-time spoken Hindi recording $\to$ instant transcription $\to$ Santhali speech playback.
  - **Curriculum Domain Clicker (Fail-Safe Judge Mode)**: 1-click execution across all 15 NIPUN Bharat classroom domains (sit down, open book, count 1-10, praise, objects).
  - **Real-Time Scientific Telemetry**: Live dials showing ASR ms, Router ms (0.02ms), TTS ms (33ms), Total Turnaround, and RTF.
  - **Free-Form Input Box**: For judge to type any custom phrase.
- [ ] **Task 5.3:** Android Jetpack Compose Prototype Packaging:
  - Package Kotlin Jetpack Compose app per `docs/ANDROID_FRONTEND_SPECIFICATION.md` bundling `fln_lexicon.sqlite`, `sat_piper_model.onnx`, and ONNX Runtime AAR.

