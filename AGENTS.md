# Agent Operational Rules & Guidelines

## 1. Git Version Control & Remote Sync
- **Repository**: `https://github.com/AshrafGalaxy/Vernacular_Pedagogy`
- **Branch**: `main`
- **Commit Standards**:
  - Every completed phase, script addition, schema update, or data pipeline improvement must be committed and pushed immediately.
  - Commits must use concise, standardized Conventional Commit messages:
    - `feat: <brief description>` for new features, scripts, or schemas.
    - `data: <brief description>` for new datasets, lexicon entries, or augmentations.
    - `docs: <brief description>` for documentation or architectural plans.
    - `fix: <brief description>` for bug fixes or schema corrections.
    - `chore: <brief description>` for configuration or environment setup.
  - After committing, automatically push changes to remote: `git push -u origin main` (or `git push`).

## 2. Compute Guardrails (Zero Heavy Local GPU/CPU Training)
- **Local Machine Boundary**:
  - Local workstation compute is strictly reserved for:
    - Code scaffolding, directory setup, and configuration.
    - Lightweight data processing (SQLite creation, index generation, text normalization, regex cleaning).
    - Unit testing, schema validation, and git workflow.
  - **No local intensive training or model compilation**:
    - IndicTrans2 LoRA fine-tuning, Piper TTS training/fine-tuning, large-scale VAD batch processing, and CTranslate2 weight conversion must NOT run on the local CPU/GPU.
  - **Cloud Compute Offload**:
    - Provide complete, self-contained, one-click runnable scripts and notebooks designed for Google Colab (T4/A100), Kaggle, and Hugging Face.
    - Cloud notebooks must include dependencies setup, automated checkpoint saving, and exporting to Hugging Face or downloadable artifacts.

## 3. Modular Phase-Based Execution
- Work in structured, verified phases.
- Ensure the Grade 1–3 Santhali FLN database is verified, normalized, and indexed before proceeding to dependent downstream tasks.

## 4. Android UI/UX Design System & Layout Guidelines
- **Brand Identity**:
  - App name is strictly **Vaani-Setu** (**VAANI-SETU** / **वाणी सेतु** / Ol Chiki **ᱵᱟᱱᱤ ᱥᱮᱛᱩ**).
  - Brand emblem & launcher icon initials: **`ᱵᱥ`** (BA-SA) in Ol Chiki on Navy `#00236f`.
- **Sharp Corners (Stitch Architectural Geometry)**:
  - Form fields, buttons, chips, and interactive controls must use sharp corners (`4dp` matching Stitch `rounded-lg`).
  - Cards and major containers use subtle `8dp` corners (Stitch `rounded-xl`).
  - Never use bubbly or overly curved `12dp` to `20dp` corners for interactive controls.
- **Symmetry & Compact Spacing (Anti-Bloat)**:
  - Maintain tight, balanced vertical and horizontal spacing.
  - Spacing between adjacent form fields must be concise (`8dp` to `10dp`).
  - Container and card inner padding should be strictly `14dp` to `16dp` (never bloated 20dp+).
  - Button heights: compact `42dp` to `46dp`. Pill/chip heights: `32dp` to `36dp`.
- **Zero Button Text/Icon Wrapping**:
  - Button text and accompanying icons must never break or wrap onto a second line.
  - Enforce `maxLines = 1`, `softWrap = false`, and concise bilingual/language-aware labels.
- **Bilingual Interface (Hindi & English)**:
  - Support seamless switching between Hindi (`हिन्दी`) and English across screens, honoring user choice stored in `UserSessionManager`.
- **Launcher Icon & Visual Branding Integrity**:
  - App launcher icon in the Android app drawer and home screen must consistently match the in-app brand emblem (`ᱵᱥ` Vaani-Setu Ol Chiki characters on Navy `#00236f` with subtle terracotta warmth).
- **Professional Vector Icons**:
  - Use clean Android vector drawables (`ic_visibility`, etc.) for form controls rather than text emojis.
- **Clean Production Form Design**:
  - Eliminate redundant debug helper buttons (e.g. "DEMO PIN भरें") from user-facing UI cards while keeping robust fallback validation.
- **No Text Field Clipping in Compact Form Controls (BasicTextField Pattern)**:
  - Never apply `Modifier.height(44.dp)` directly onto Material 3's `OutlinedTextField`. Material 3 `OutlinedTextField` enforces `minHeight = 56dp` and hardcoded internal `16dp` vertical padding (total 32dp), which squeezes the inner text area down to ~12dp and causes severe clipping of text glyphs (especially Devanagari upper/lower matras, Ol Chiki characters, and English descenders/ascenders).
  - For compact, unbloated form fields (`44dp`), always use `BasicTextField` wrapped in a vertically centered `Box`/`Row` container with `SolidColor(Primary)` cursor, `4dp` sharp corners (`RoundedCornerShape(4.dp)`), `#F8FAFC` container fill, `#0F172A` high-contrast text color, and dynamic border states (`Primary` on focus, `#BA1A1A` on error, `#CBD5E1` resting). This preserves exact `44dp` height symmetry with zero text clipping.
