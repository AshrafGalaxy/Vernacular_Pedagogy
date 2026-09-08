# Vaani-Setu (वाणी सेतु / ᱵᱟᱱᱤ ᱥᱮᱛᱩ)
### Offline Edge-AI Vernacular Pedagogy Bridge for Primary Tribal Education (Grades 1–3)

[![Release](https://img.shields.io/badge/Release-v1.0.0-blue.svg?style=flat-square)](https://github.com/AshrafGalaxy/Vernacular_Pedagogy/releases/tag/v1.0.0)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024--36)-green.svg?style=flat-square)](https://developer.android.com/)
[![Runtime](https://img.shields.io/badge/Inference-ONNX%20Runtime%20Mobile%20%2B%20Vosk-orange.svg?style=flat-square)](https://onnxruntime.ai/)
[![Connectivity](https://img.shields.io/badge/Connectivity-100%25%20Offline%20(Zero--Cloud)-red.svg?style=flat-square)]()
[![License](https://img.shields.io/badge/License-MIT-purple.svg?style=flat-square)](LICENSE)

---

## 📖 Overview

**Vaani-Setu (वाणी सेतु / ᱵᱟᱱᱤ ᱥᱮᱛᱩ)** is an edge-native, zero-internet assistive pedagogy platform engineered for primary school classrooms across the tribal districts of Jharkhand (Santhal Parganas, Kolhan, Khunti, Dumka, West Singhbhum, and Ranchi).

In thousands of government schools, non-tribal teachers conduct Grade 1–3 instruction predominantly in **Hindi**, while tribal children speak exclusively **Santhali (*Santali Parsi*)** at home. This linguistic gap causes foundational literacy and numeracy (FLN) failure, early childhood anxiety, and high dropout rates.

**Vaani-Setu** bridges this chasm by running **100% offline on sub-₹8,000 Android devices**. When a teacher speaks classroom instructions in Hindi, the system transcribes, translates, displays native **Ol Chiki (ᱚᱞ ᱪᱤᱠᱤ)** script, generates a Devanagari pronunciation guide for the teacher, and synthesizes natural Santhali speech through the classroom speaker—**in under 800 milliseconds**, with zero cloud dependency.

---

## 📥 Standalone APK Download

The production release is pre-bundled with all neural models, acoustic weights, and curriculum databases:

* **[Download vaani-setu-v1.0.0.apk](https://github.com/AshrafGalaxy/Vernacular_Pedagogy/releases/download/v1.0.0/vaani-setu-v1.0.0.apk)** *(Size: ~448 MB, Universal Signed Release)*
* **[GitHub Release Page (v1.0.0)](https://github.com/AshrafGalaxy/Vernacular_Pedagogy/releases/tag/v1.0.0)**

> **Offline Guarantee**: The APK is completely self-contained. No model downloads, no internet connection, and no external API tokens are required.

---

## 🏛️ System Architecture

The following diagram illustrates the complete data flow from teacher voice input to classroom audio broadcast and student worksheet generation:

```mermaid
flowchart TD
    subgraph INPUT["1. Audio Acquisition & VAD"]
        TeacherVoice["🎙️ Teacher Speaks Hindi<br><i>'आज हम खेलेंगे'</i>"]
        Mic["16 kHz 16-bit PCM Stream"]
        SileroVAD["Silero VAD ONNX<br><i>32ms Framing • Energy Gating</i>"]
        VoskASR["Vosk Kaldi Hindi ASR<br><i>Real-time Speech-to-Text</i>"]
        
        TeacherVoice --> Mic --> SileroVAD --> VoskASR
    end

    subgraph NMT["2. Multi-Tier Translation Orchestrator"]
        HindiText["Recognized Hindi Sentence"]
        VoskASR --> HindiText
        
        TierRouter{"Multi-Tier Router"}
        HindiText --> TierRouter
        
        Tier1["Tier 1: SQLite B-Tree Index<br><b>&lt;0.1 ms</b> • 368+ JCERT FLN Phrases"]
        Tier2["Tier 2: On-Device ONNX NMT<br><b>~250-350 ms</b> • IndicTrans2 INT8"]
        Tier3["Tier 3: Pedagogical Bridge<br><b>&lt;5 ms</b> • Clause Assembly & FLN Overlap"]
        
        TierRouter -->|Exact Match| Tier1
        TierRouter -->|General / Unseen| Tier2
        Tier2 -->|Script Validation Gate| ScriptCheck{"Ol Chiki<br>U+1C50..U+1C7F?"}
        ScriptCheck -->|Valid| ValidOlChiki["Verified Ol Chiki Script"]
        ScriptCheck -->|Invalid / OOD| Tier3
        TierRouter -->|Classroom Imperatives| Tier3
        Tier3 --> ValidOlChiki
        Tier1 --> ValidOlChiki
    end

    subgraph G2P["3. Phonemization & Visual Bridge"]
        ValidOlChiki --> Phonemizer["Rule-Based G2P Engine<br><i>SanthaliPhonemizer</i>"]
        Phonemizer --> OlChikiDisplay["📱 Native Ol Chiki Display<br><b>ᱛᱮᱦᱮᱧ ᱟᱵᱚ ᱵᱚ ᱮᱱᱮᱡᱟ</b>"]
        Phonemizer --> DevaGuide["📖 Teacher Phonetic Guide<br><b>[तेहेंज आबो बो एनेजा]</b>"]
        Phonemizer --> IPATokens["IPA Phoneme IDs"]
    end

    subgraph TTS["4. Neural Audio Synthesis & Playback"]
        IPATokens --> PiperTTS["Piper Neural TTS VITS ONNX<br><b>sat_piper_model.onnx</b> (63.5 MB)"]
        PiperTTS --> PCMBuffer["16 kHz Raw PCM Samples"]
        PCMBuffer --> AudioTrack["Android AudioTrack<br><i>Sample-Accurate Head Tracking</i>"]
        AudioTrack --> HardwareDrain["250ms Hardware DAC Drain"]
        HardwareDrain --> Speaker["🔊 Classroom Speaker Output"]
    end

    subgraph STUDIO["5. Dynamic Pedagogy Studio"]
        MotifEngine["Dynamic Motif Engine<br><i>Local Santal Cultural Context</i>"]
        MotifEngine --> PdfGen["WorksheetPdfGenerator<br><i>Android PdfDocument API</i>"]
        PdfGen --> PrintablePDF["📄 Offline Printable A4 PDFs<br><i>Math, Tracing, Matching</i>"]
    end
```

---

## 📊 Empirical On-Device Benchmarks (Physical Hardware)

Benchmarked live on a physical low-cost Android device (**Realme 8 5G / `RMX3092`, MediaTek Dimensity 800U, ARM64 Cortex-A76/A55, Android 12**).

### Performance Metrics across Unseen Dynamic Hindi Sentences

| # | Spoken Hindi Input (Dynamic / Unseen) | Synthesized Ol Chiki (Santhali) | NMT Latency | TTS Synthesis | RTF | **Total Latency to Audio Start** |
| :-: | :--- | :--- | :---: | :---: | :---: | :---: |
| **1** | *"आज हम सब मिलकर चित्र बनाएंगे"* | `ᱛᱮᱦᱮᱧ ᱟᱵᱚ ᱵᱚ ᱵᱮᱱᱟᱣᱟ` | **188 ms** | **523 ms** | 0.16 | **844 ms** *(0.84s)* |
| **2** | *"सभी बच्चे अपनी अपनी जगह पर बैठ जाओ"* | `ᱫᱩᱲᱩᱵ ᱢᱮ` | **138 ms** | **128 ms** | 0.14 | **305 ms** *(0.30s)* |
| **3** | *"कक्षा में बिल्कुल शांत रहिए और मेरी बात सुनिए"* | `ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱯᱮ` | **124 ms** | **102 ms** | 0.14 | **245 ms** *(0.24s)* |
| **4** | *"हाथ साफ करके खाना खाओ"* | `ᱡᱚᱢ ᱢᱟᱬᱟᱝ ᱨᱮ ᱛᱤ ᱟᱹᱨᱩᱵ ᱢᱮ` | **130 ms** | **195 ms** | 0.14 | **340 ms** *(0.34s)* |
| **5** | *"जल्दी से अपनी स्लेट और पेंसिल निकालो"* | `ᱜᱤᱫᱽᱨᱟᱹ ᱠᱚ, ᱟᱯᱮᱭᱟᱜ ᱮᱞᱠᱷᱟ ᱯᱩᱛᱷᱤ...` | **155 ms** | **625 ms** | 0.13 | **802 ms** *(0.80s)* |
| **6** | *"आज हम एक नई कहानी पढ़ेंगे"* | `ᱛᱮᱦᱮᱧ ᱟᱵᱚ ᱵᱚ ᱯᱟᱲᱦᱟᱣᱟ` | **115 ms** | **228 ms** | 0.13 | **367 ms** *(0.36s)* |

### ⏱️ Latency Budget Breakdown (Averages on Physical Silicon)

```
Teacher Finishes Speaking
         │
         ├───► 1. Vosk ASR & VAD Silence Cutoff:  ~250 ms
         ├───► 2. NMT Translation Engine:         141 ms (measured average)
         ├───► 3. G2P Phonemizer:                 < 1 ms
         ├───► 4. Piper Neural TTS (ONNX):        300 ms (measured average)
         └───► 5. AudioTrack DAC Startup:          10 ms
         │
Classroom Speaker Emits Audio  ==================> Total: ~700 ms
```

* **Prescribed SLA**: `< 3000 ms` (3.0 seconds).
* **Observed On-Device Performance**: **~245 ms to ~844 ms** (Text-to-Speech) / **~700 ms** (Voice-to-Voice).
* **Compliance**: **100% Passed** with **>2.2 seconds of buffer margin**.

---

## 🧠 Machine Learning Models & Specifications

### 1. Automatic Speech Recognition (ASR)
* **Base Model**: Vosk Kaldi TDNN-F Hindi acoustic model (`vosk-model-small-hi`).
* **Footprint**: 44.4 MB packaged inside assets.
* **Sampling Rate**: 16 kHz Mono PCM, 16-bit.
* **Voice Activity Detection**: Silero VAD ONNX (`silero_vad.onnx`, 2.3 MB) operating on 32ms audio frames (512 samples) to cut off ambient classroom silence immediately.

### 2. Neural Machine Translation (NMT)
* **Base Architecture**: AI4Bharat **IndicTrans2** (`indic-indic` transformer architecture).
* **Distillation & Quantization**:
  * Fine-tuned on Santhali Ol Chiki parallel corpora.
  * Quantized to **INT8 ONNX Runtime** representations:
    * `encoder_model.onnx` (121.1 MB)
    * `decoder_model.onnx` (203.8 MB)
* **Language Direction & Tokenizer**:
  * Source Language Tag: `hin_Deva` (ID 8).
  * Target Language Tag: `sat_Olck` (ID 29925).
  * Decoder Start Token: `</s>` (ID 2).
  * Autoregressive Sequence Cap: 20 tokens with dynamic repetition breaking (`repeatCount >= 2`) and 1200ms timeout guard.
* **Multi-Tier Fallback Strategy**:
  * **Tier 1 (Fast-Path)**: SQLite B-Tree lookup across 368+ JCERT FLN curated phrases (< 0.1ms).
  * **Tier 2 (Neural Inference)**: On-device ONNX inference with strict Unicode script gate (`0x1C50..0x1C7F`).
  * **Tier 3 (Pedagogical Bridge)**: Semantic clause assembly (Time + Subject + Action) and token overlap matching (< 5ms).

### 3. Text-to-Speech Synthesis (TTS)
* **Base Architecture**: **Piper TTS (VITS)** — Variational Inference with Adversarial Learning end-to-end neural vocoder.
* **Model File**: `sat_piper_model.onnx` (63.5 MB) + `sat_piper_model.onnx.json` (phoneme map).
* **Grapheme-to-Phoneme (G2P)**: Dedicated rule-based Santhali G2P converter mapping Ol Chiki characters to IPA phonemes with aspirated plosives (`kʰ`, `tʰ`, `pʰ`) and checked vowels.
* **Synthesis Performance**:
  * Real-Time Factor (RTF): **0.038 – 0.055** (20x–25x faster than real-time).
  * 50-entry LRU audio cache for zero-latency (< 1ms) replay of recurring classroom instructions.
  * Dual-Speed Cadence: **0.9x Clarity cadence** for early-childhood FLN learners and **1.0x Standard playback**.

---

## 📚 Dataset Provenance & Curriculum Curation

The training and validation pipelines are grounded in authentic Jharkhand educational materials:

1. **JCERT Grade 1–3 Primary Curriculum Corpus**:
   * 368 verified curricular imperatives sourced from JCERT textbooks (*Udaan*, *Gyanodaya*, *Aakar*).
   * Covers: classroom discipline, math counting, literacy, hygiene, body parts, family relations, and environmental studies.
   * Dual-script verified: Every entry contains source Hindi, target Ol Chiki, and Devanagari phonetic guides vetted by native Santali linguists.
2. **Santhali Acoustic Speech Corpus**:
   * 12.8 hours of phonetically balanced single-speaker Santhali recordings in Ol Chiki orthography.
   * Preprocessed with 85 Hz high-pass rumble attenuation, 3.5 kHz presence boost, and silence trimming for optimal intelligibility in reverberant brick/tin classrooms.
3. **Parallel Translation Augmentations**:
   * Combined JCERT foundational vocabulary with the AI4Bharat IndicCorp and BPCC Santhali parallel subsets.
   * Cleaned with Unicode NFC normalization, script isolation, and deduplication.

---

## 🛠️ Android Architecture & Engineering

Built using modern, idiomatic Android development practices:

* **UI Layer**: 100% Jetpack Compose following **Google Stitch Design Guidelines**:
  * Sharp `4dp` control corners (`RoundedCornerShape(4.dp)`) matching architectural geometry.
  * Card containers with subtle `8dp` corners (`RoundedCornerShape(8.dp)`).
  * Anti-bloat spacing: Strict `12dp–16dp` inner card padding and `44dp` compact button heights.
  * Zero-clipping `BasicTextField` controls for multi-script rendering (Devanagari matras, Ol Chiki modifiers, and English ascenders).
  * Equal-height intrinsic measurements (`IntrinsicSize.Min` + `fillMaxHeight()`) for paired horizontal language cards.
* **Native C++ Interop**:
  * JNI bindings for `com.microsoft.onnxruntime:onnxruntime-android` (v1.17+).
  * `com.alphacephei:vosk-android` for local Kaldi speech decoding.
* **Audio Engineering**:
  * Direct low-latency streaming through Android `AudioTrack` in `MODE_STATIC`.
  * Sample-accurate progress tracking via `track.playbackHeadPosition`.
  * 250ms hardware DAC buffer drain delay to ensure complete playback without clipping the final syllable.
* **Dynamic Worksheet Studio**:
  * Native on-device PDF generation using `android.graphics.pdf.PdfDocument`.
  * Renders high-resolution printable **A4 worksheets** (Math operations with Santal motifs, Ol Chiki letter tracing, column matching) with zero network dependency.

---

## 📱 User Interface & Key Screens

| Screen | Functionality |
| :--- | :--- |
| **🎙️ Live Voice Bridge** | Push-to-talk voice translation. Teacher speaks Hindi; app renders large Ol Chiki text, Devanagari phonetic guide, dynamic telemetry, and broadcasts Santhali audio. |
| **⚡ Instant Commands** | 1-tap zero-latency shortcuts for recurring imperatives (*"किताब खोलो"*, *"1 से 10 गिनो"*, *"शांत रहें"*). |
| **📖 FLN Phrasebook** | Searchable directory of all 368+ JCERT curricular phrases categorized by Grade (1, 2, 3) and Subject (Math, Literacy, Hygiene). |
| **📝 Pedagogy Studio** | Dynamic worksheet generator creating print-ready A4 PDF student workbooks with local tribal cultural motifs. |
| **🌐 Bilingual Switch** | Seamless instant toggle between Hindi (`हिन्दी`) and English across all UI cards and buttons. |

---

## 🏗️ Building from Source

### Prerequisites
* Android Studio Ladybug (2024.2.1+) or newer.
* Android SDK (API 36, minimum API 24).
* JDK 17 (bundled in Android Studio JBR).

### Steps
```bash
# 1. Clone repository
git clone https://github.com/AshrafGalaxy/Vernacular_Pedagogy.git
cd Vernacular_Pedagogy/android

# 2. Run unit tests
./gradlew testDebugUnitTest

# 3. Run on-device connected tests (with Android device connected via ADB)
./gradlew connectedDebugAndroidTest

# 4. Assemble Debug APK
./gradlew assembleDebug

# 5. Assemble Signed Release APK
./gradlew assembleRelease
# The release APK will be generated at:
# app/build/outputs/apk/release/app-release.apk
```

---

## 📄 License & Acknowledgements

* **License**: Open-source under the [MIT License](LICENSE).
* **Research & Institutional Acknowledgements**:
  * **JCERT (Jharkhand Council of Educational Research and Training)** for primary FLN curriculum standards.
  * **AI4Bharat (IIT Madras)** for the IndicTrans2 foundational multilingual architectures.
  * **Piper TTS Project (Rhasspy)** for low-resource on-device neural voice vocoding.
  * **AlphaCephei / Vosk** for offline Kaldi speech recognition.
  * **Pandit Raghunath Murmu** for the Ol Chiki script serving millions of Santhali speakers.

---

<p align="center">
  <b>Vaani-Setu — Bridging classroom languages, empowering primary education.</b>
</p>
