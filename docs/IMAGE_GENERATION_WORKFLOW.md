# Automated Image Generation Workflow

The 150-item source of truth is `docs/GENERATIVE_PEDAGOGICAL_ASSET_PLAN.md`. The batch runner builds the copy-ready prompt for every matrix row, calls one image provider, saves stable filenames, and updates a resumable manifest after every item.

## Setup

Install the provider clients:

```powershell
python -m pip install -r requirements.txt
```

Set exactly one provider key in the current PowerShell session:

```powershell
$env:GEMINI_API_KEY = "paste-your-key-here"
# or
$env:OPENAI_API_KEY = "paste-your-key-here"
```

Do not commit keys or put them in Markdown, notebooks, or source files.

## Preview all prompts without spending API credits

```powershell
python scripts/generate_image_assets.py --provider gemini --dry-run
```

This writes `assets/images/generated_masters/generation_manifest.json` with all 150 prompts and no images.

## Generate a small pilot first

```powershell
python scripts/generate_image_assets.py --provider gemini --start 1 --end 12 --delay 2
```

The default Gemini model is `gemini-2.5-flash-image`. Override it when the account exposes a different Nano Banana image model:

```powershell
python scripts/generate_image_assets.py --provider gemini --model gemini-3-pro-image-preview
```

For OpenAI GPT Image:

```powershell
python scripts/generate_image_assets.py --provider openai --start 1 --end 12 --delay 2
```

## Generate or resume all 150

```powershell
python scripts/generate_image_assets.py --provider gemini --delay 2
```

Existing files are skipped, so rerunning the same command resumes failed or unfinished work. Use `--overwrite` only when intentionally regenerating selected files. API failures are recorded in the manifest and do not discard successful results.

## Important review boundary

The script automates generation, naming, provenance, and retries. It does not mark assets as educator-approved. After generation, review the selected masters for referent accuracy, anatomy, cultural dignity, text artifacts, clipping, transparency, and the 400x400 runtime derivative requirements from the asset plan.