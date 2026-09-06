# Workspace Rule: Git Management & Cloud-First Training

- **Repository**: Always sync to `https://github.com/AshrafGalaxy/Vernacular_Pedagogy`.
- **Commit Rules**: Commit with concise, descriptive Conventional Commit messages (`feat:`, `data:`, `docs:`, `chore:`, `fix:`). Always push to `origin main` after committing.
- **Compute Guardrail**: Never run heavy model training, quantization, or audio rendering on the local machine. Package all heavy ML/NLP/TTS training tasks as self-contained Google Colab / Kaggle / Hugging Face runnable scripts and notebooks.
