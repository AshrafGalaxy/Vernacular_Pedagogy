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
| **Piper TTS Cloud Training & Export** | **100% COMPLETE** | Google Colab (T4) | **NONE** (`sat_piper_model.onnx` 60.6 MB verified, RTF 0.05–0.08) |
| **Colab Multi-Account Failover** | **100% COMPLETE** | Local Orchestrator | **NONE** (`ashraf305a@gmail.com` authenticated & run) |
| **Android Edge Runtime Packaging** | **PLANNED** | Local Workstation | **Ready: Phase 4 Implementation** |


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

## 3. Phase 3 Verification & Benchmark Results (Achieved)

- **ONNX Model Size:** `models/tts/sat_piper_model.onnx` (**60.57 MB**)
- **Model Archive:** `models/tts/sat_piper_model.tar.gz` (**55.69 MB**)
- **Local CPU Synthesis Latency Benchmark (`scripts/verify_tts.py`):**
  - `"ᱫᱩᱲᱩᱵ ᱢᱮ"` (Sit down): **47.4 ms** (Audio: 0.66s | **RTF = 0.072**) — **PASS**
  - `"ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ"` (Open book): **77.3 ms** (Audio: 1.26s | **RTF = 0.061**) — **PASS**
  - `"ᱞᱮᱠᱷᱟᱭ ᱢᱮ"` (Count): **41.3 ms** (Audio: 0.51s | **RTF = 0.081**) — **PASS**
  - `"ᱟᱹᱰᱤ ᱵᱷᱟᱹᱜᱤ"` (Very good): **87.1 ms** (Audio: 1.71s | **RTF = 0.051**) — **PASS**
  - **Real-Time Factor:** **0.051 – 0.081** (Target: $\le 0.35$ | **>4x faster than real-time**)
- **Sample Audio Files:** Generated and verified at `models/tts/samples/`.

---

## 4. Edge Storage & Memory Footprint (Audited & Proof-Backed)

Comprehensive breakdown available in [`docs/STORAGE_FOOTPRINT_SPECIFICATION.md`](file:///c:/Users/Ashraf/Desktop/26042/docs/STORAGE_FOOTPRINT_SPECIFICATION.md):
- **FLN SQLite Database:** `assets/fln_lexicon.sqlite` (**184,320 bytes** / 180 KiB)
- **Piper TTS ONNX Model:** `models/tts/sat_piper_model.onnx` (**63,516,051 bytes** / 60.57 MiB)
- **IndicTrans2 INT8 Model:** `models/mt/indictrans2_sat_int8_ct2_unpruned.tar.gz` (**333,953,312 bytes** uncompressed / 318.48 MiB)
- **Total Download / APK Archive:** **342.53 MiB** (359.16 MB)
- **Total Extracted On-Device Storage:** **379.23 MiB** (397.66 MB) $\rightarrow$ **2.48%** of a 16 GB eMMC Android tablet.
- **Dynamic Active RAM Footprint:** **~515 – 555 MB** peak $\rightarrow$ Leaves **>400 MB safe margin** on a 2 GB RAM device.

---

## 5. Hugging Face Model Hub & Agent Skill

- **Model Hub Repository:** `Ashraf01k/vernacular-pedagogy-santhali`
- **Automation Upload Script:** [`scripts/upload_models_to_huggingface.py`](file:///c:/Users/Ashraf/Desktop/26042/scripts/upload_models_to_huggingface.py)
- **Agent Skill:** Registered at [`.agents/skills/hf-cli/SKILL.md`](file:///c:/Users/Ashraf/Desktop/26042/.agents/skills/hf-cli/SKILL.md) and global Antigravity skills.
- **Status:** CLI installed and authenticated as `user=Ashraf01k`. Ready for automated upload upon granting write access.

---

## 6. Next Milestone: Phase 4 Android Edge Runtime

Proceed to **Phase 4: Unified Android Edge Runtime Engine**, binding SQLite FLN cache, CTranslate2 INT8 NMT, and Piper ONNX TTS into a unified offline Android service as specified in [`docs/ANDROID_FRONTEND_SPECIFICATION.md`](file:///c:/Users/Ashraf/Desktop/26042/docs/ANDROID_FRONTEND_SPECIFICATION.md).



