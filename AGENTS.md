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
