# Project Status & Execution Matrix: Hindi-to-Santhali Vernacular Pedagogy

Last Updated: Phase 3 Stack & Pipeline Hardening Complete

---

## 1. High-Level Status Overview

| Component | Status | Execution Location | User Manual Work Required? |
| :--- | :--- | :--- | :--- |
| **FLN Database (368 records)** | **100% COMPLETE** | Local Workstation | **NONE** (Verified & Locked) |
| **SQLite Fast-Path Index (<0.1ms)** | **100% COMPLETE** | Local Workstation | **NONE** (`assets/fln_lexicon.sqlite` ready) |
| **Pedagogical Bitext Preprocessor** | **100% COMPLETE** | Local Workstation | **NONE** (`train.tsv` / `val.tsv` clean) |
| **IndicTrans2 LoRA MT & INT8** | **100% COMPLETE** | Google Colab (T4) | **NONE** (`models/mt/indictrans2_sat_int8_ct2.tar.gz` ready) |
| **PyreFly In-Memory Error Resolution** | **100% COMPLETE** | Global & Local IDE Config | **NONE** (Permanently patched across all projects) |
| **Dual Audio Ingest (XKaab + IndicVoices-R)** | **100% COMPLETE** | Cloud / Local Pipeline | **NONE** (Raw audio decoding & gating verified) |
| **Piper TTS Cloud Training Engine** | **100% HARDENED & TESTED** | Google Colab (T4) | **Ready: Run via 1-Click Notebook or CLI** |
| **Colab Multi-Account Support** | **100% COMPLETE** | Local Orchestrator | **Waiting on Auth Code for ashraf305a@gmail.com** |
| **Android Edge Runtime Packaging** | **PLANNED** | Local Workstation | **Post-TTS ONNX Download** |

---

## 2. What Is 100% Completed (Verified & Pushed)

1. **Phase 0 Database & Fast-Path Retrieval:**
   - 368 verified Grade 1–3 Santhali (`sat_Olck`) pedagogical records across 15 domains.
   - Compiled into high-speed B-Tree SQLite database (`assets/fln_lexicon.sqlite`).
   - Benchmark latency: **0.02 ms – 0.4 ms** per classroom command.

2. **Phase 1 Bitext Normalization & Pedagogical Dataset:**
   - Strict grammatical and semantic constraint engine (`scripts/03_bitext_normalizer.py`).
   - Eradicated all nonsensical pairings (no hallucinations or mismatched entities).
   - Normalized splits ready at `data/processed/bitext/train.tsv` (466 rows) and `val.tsv` (52 rows).

3. **Phase 2 Neural Machine Translation (LoRA + CTranslate2 INT8):**
   - Fine-tuned IndicTrans2 320M (`hin_Deva` $\rightarrow$ `sat_Olck`) on Colab Tesla T4 GPU (Train Loss: 3.080, Val Loss: 2.904).
   - Merged LoRA adapters into base weights and quantized to CTranslate2 INT8 format (`model.bin` ~325 MB).
   - Packaged and verified archive locally at `models/mt/indictrans2_sat_int8_ct2.tar.gz` (286.7 MB).
   - Verified CPU inference latency <120 ms per classroom phrase.

4. **IDE Stability & Diagnostic Fix (PyreFly):**
   - Eliminated the recurrent `"Virtual in-memory files are not supported: .pyrefly/virtual/..."` diagnostics crash.
   - Added global path exclusions and LSP file-watcher guards to prevent VS Code / IDE extensions from treating virtual in-memory analyzer buffers as physical disk files.

5. **Phase 3 Voice Synthesis Stack & Cloud Hardening:**
   - **Dual-Corpus Ingest (`scripts/04_fetch_santhali_audio.py`)**:
     - Pulls from **XKaab/ASR-Santali_4hrs** (300 clips) and **AI4Bharat IndicVoices-R (`ai4bharat/indicvoices_r`)** (250 clips).
     - Automated Hugging Face gate authentication via `HF_TOKEN`.
     - Supports raw binary audio payload decoding (`audio.bytes`) via `soundfile` and `torchaudio` fallback.
     - Enforces Ol Chiki Unicode NFC normalization (`\u1C50-\u1C7F`).
   - **Cloud Training Worker (`scripts/run_phase3_cloud_train.py`)**:
     - Patched PyTorch 2.6 `torch.load` security guard allowing `PosixPath` and `weights_only=False`.
     - Resolved Lightning `MisconfigurationException` via in-memory warm-start weight transfer (epoch counter cleanly resets to 0).
     - Bypassed TorchDynamo spline guard failures using native TorchScript ONNX export (`dynamo=False`).
     - Verified export produces valid 60.57 MB `sat_piper_model.onnx`.
   - **Local Orchestration & 1-Click Execution**:
     - Real-time stdout streaming and timeout watchdog (`scripts/launch_phase3_on_colab.py`).
     - 1-Click browser notebook (`notebooks/colab_phase3_piper_tts.ipynb`).
     - Multi-account OAuth switcher for Colab CLI (`scratch/colab_auth_tool.py`).

---

## 3. Active Action Items

### Action 1: Authenticate Colab Account `ashraf305a@gmail.com`
- Primary Colab account reached free-tier T4 GPU allocation limits (`503 Service Unavailable`).
- OAuth authorization URL generated with pre-filled `login_hint=ashraf305a@gmail.com`.
- Paste the returned Google Authorization Code to resume automated training via Colab CLI, or run Cell 1 in [`notebooks/colab_phase3_piper_tts.ipynb`](https://colab.research.google.com/github/AshrafGalaxy/Vernacular_Pedagogy/blob/main/notebooks/colab_phase3_piper_tts.ipynb).

### Action 2: Model Packaging & Verification
- Once training completes, the exported `sat_piper_model.tar.gz` is downloaded to `models/tts/`.
- Run `python scripts/verify_tts.py` to benchmark Real-Time Factor (RTF $\le 0.35$) on local CPU.

---

## 4. Immediate Next Milestone

Transition to **Phase 4: Unified Android Edge Runtime Engine**, binding SQLite FLN cache, CTranslate2 INT8 NMT, and Piper ONNX TTS into a unified offline Android service.

