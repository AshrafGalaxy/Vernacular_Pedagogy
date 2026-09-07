"""Batch-generate the 150 pedagogical image assets.

The source of truth is docs/GENERATIVE_PEDAGOGICAL_ASSET_PLAN.md. The script
supports Gemini image models and OpenAI GPT Image, writes stable filenames,
and can resume an interrupted batch without replacing existing files.
"""

from __future__ import annotations

import argparse
import base64
import json
import os
import re
import sys
import time
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
PLAN_PATH = ROOT / "docs" / "GENERATIVE_PEDAGOGICAL_ASSET_PLAN.md"
DEFAULT_OUTPUT = ROOT / "assets" / "images" / "generated_masters"
DEFAULT_PROMPT_CATALOG = ROOT / "docs" / "GENERATIVE_ASSET_PROMPTS.md"
PROMPT_PARTS_DIR = ROOT / "docs" / "generative_asset_prompts"

OBJECT_SECTIONS = {
    "A. Food and Fruit",
    "B. Vegetables",
    "C. Animals and Small Fauna",
    "D. Classroom Objects",
    "F. Counting Cards",
    "G. Environment and Realia Scenes",
    "J. Attribute, Shape, and Comparison Assets",
}

PEOPLE_SECTIONS = {
    "E. Body Parts",
    "H. Family and Community",
    "I. Action Vignettes",
}

SHARED_NEGATIVE = (
    "photorealistic, colour fill, gradients, grey wash, busy scene, "
    "multiple unrelated objects, text, writing, gibberish letters, numerals, "
    "logo, watermark, frame, cropped subject, extra fingers, malformed hands, "
    "uncanny face, stereotype, weapon in hand, violence"
)


def parse_plan(path: Path) -> list[dict[str, str]]:
    """Extract every production-matrix row from the Markdown plan."""
    rows: list[dict[str, str]] = []
    section = ""
    row_pattern = re.compile(
        r"^\|\s*(\d+)\s*\|\s*`([^`]+)`\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|$"
    )
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith("### "):
            section = line[4:].strip()
        match = row_pattern.match(line)
        if not match:
            continue
        number, asset_key, lexicon, subject, worksheet_use = match.groups()
        rows.append(
            {
                "number": number,
                "asset_key": asset_key,
                "lexicon": lexicon.strip(),
                "subject": subject.strip(),
                "worksheet_use": worksheet_use.strip(),
                "section": section,
                "filename": f"motif_{asset_key}.png",
            }
        )
    if len(rows) != 150:
        raise ValueError(f"Expected 150 production rows, found {len(rows)} in {path}")
    return rows


def build_prompt(item: dict[str, str]) -> str:
    """Build the single copy-ready prompt for one matrix item."""
    subject = item["subject"]
    negative_prompt = SHARED_NEGATIVE
    if item["section"] in PEOPLE_SECTIONS:
        prefix = (
            "A respectful, non-stereotyped Santhal-region child or adult "
            f"{subject}, child-readable primary-school workbook illustration, "
            "modest everyday clothing, clear hands and facial expression, "
            "clean bold black ink contour, centred with minimal context object "
            "only when needed for comprehension, white background, no text, "
            "no letters, no numerals, no border, no watermark."
        )
    else:
        prefix = (
            f"A single {subject}, child-readable educational illustration for a "
            "Grade 1-3 Santali classroom workbook, accurate Indian/Santhal-region "
            "realia, clean bold black ink contour, a few simple interior contour "
            "lines, friendly but not cartoonish proportions, centred, isolated on "
            "a pure white background, no text, no letters, no numerals, no border, "
            "no shadow, no watermark."
        )
    if item["section"] == "F. Counting Cards":
        prefix += " Use identical mango artwork repeated in a clear countable layout; do not draw numeral glyphs."
    if item["asset_key"] in {"red_mango", "green_sal_leaf", "yellow_rice_stalk"}:
        prefix += " This is a colour-card mode asset: use the requested clear colour fill while retaining the black contour."
        negative_prompt = negative_prompt.replace("colour fill, ", "")
    return f"{prefix} Negative prompt: {negative_prompt}."


def save_bytes(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def write_prompt_catalog(items: list[dict[str, str]], path: Path) -> None:
    """Export every generated prompt as an individual copy-ready Markdown block."""
    lines = [
        "# Generative Asset Prompt Catalog",
        "",
        "Copy any complete fenced block into an image-generation agent.",
        "",
        "Source of truth: `docs/GENERATIVE_PEDAGOGICAL_ASSET_PLAN.md`.",
        "",
    ]
    current_section = ""
    for item in items:
        if item["section"] != current_section:
            current_section = item["section"]
            lines.extend([f"## {current_section}", ""])
        lines.extend(
            [
                f"### {int(item['number']):03} - `{item['asset_key']}`",
                f"- Filename: `{item['filename']}`",
                f"- Lexicon: {item['lexicon']}",
                f"- Subject: {item['subject']}",
                f"- Primary worksheet use: {item['worksheet_use']}",
                "",
                "```text",
                build_prompt(item),
                "```",
                "",
            ]
        )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines), encoding="utf-8")


def write_prompt_parts(items: list[dict[str, str]], directory: Path) -> None:
    """Export one manual-use Markdown file for each production-matrix section."""
    directory.mkdir(parents=True, exist_ok=True)
    grouped: dict[str, list[dict[str, str]]] = {}
    for item in items:
        grouped.setdefault(item["section"], []).append(item)
    for section, section_items in grouped.items():
        section_letter = section.split(".", 1)[0]
        section_name = section.split(".", 1)[1].strip()
        slug = re.sub(r"[^a-z0-9]+", "_", section_name.lower()).strip("_")
        path = directory / f"{section_letter}_{slug}.md"
        lines = [
            f"# {section}",
            "",
            "Copy one complete fenced prompt at a time into your image-generation tool.",
            "Do not ask the model to generate multiple assets in one request.",
            "",
        ]
        for item in section_items:
            lines.extend(
                [
                    f"### {int(item['number']):03} - `{item['asset_key']}`",
                    f"- Filename: `{item['filename']}`",
                    f"- Lexicon: {item['lexicon']}",
                    f"- Subject: {item['subject']}",
                    f"- Primary worksheet use: {item['worksheet_use']}",
                    "",
                    "```text",
                    build_prompt(item),
                    "```",
                    "",
                ]
            )
        path.write_text("\n".join(lines), encoding="utf-8")


def generate_gemini(prompt: str, model: str) -> bytes:
    try:
        from google import genai
        from google.genai import types
    except ImportError as exc:
        raise RuntimeError("Install the Gemini extra with: pip install google-genai") from exc
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        raise RuntimeError("GEMINI_API_KEY is not set")
    client = genai.Client(api_key=api_key)
    response = client.models.generate_content(
        model=model,
        contents=prompt,
        config=types.GenerateContentConfig(
            response_modalities=["IMAGE"],
        ),
    )
    for candidate in response.candidates or []:
        for part in candidate.content.parts or []:
            if getattr(part, "inline_data", None) and part.inline_data.data:
                return bytes(part.inline_data.data)
    raise RuntimeError("Gemini returned no image bytes")


def generate_openai(prompt: str, model: str) -> bytes:
    try:
        from openai import OpenAI
    except ImportError as exc:
        raise RuntimeError("Install the OpenAI extra with: pip install openai") from exc
    if not os.environ.get("OPENAI_API_KEY"):
        raise RuntimeError("OPENAI_API_KEY is not set")
    result = OpenAI().images.generate(
        model=model,
        prompt=prompt,
        size="1024x1024",
        quality="high",
        output_format="png",
    )
    image = result.data[0]
    if getattr(image, "b64_json", None):
        return base64.b64decode(image.b64_json)
    if getattr(image, "url", None):
        from urllib.request import urlopen

        with urlopen(image.url, timeout=120) as response:
            return response.read()
    raise RuntimeError("OpenAI returned no image data")


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate all 150 pedagogical image assets")
    parser.add_argument("--provider", choices=("gemini", "openai"), default="gemini")
    parser.add_argument("--model", help="Provider model override")
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--start", type=int, default=1, help="First matrix number to process")
    parser.add_argument("--end", type=int, default=150, help="Last matrix number to process")
    parser.add_argument("--limit", type=int, help="Maximum number of new images to generate")
    parser.add_argument("--dry-run", action="store_true", help="Write prompts and manifest without API calls")
    parser.add_argument("--overwrite", action="store_true", help="Regenerate existing files")
    parser.add_argument("--delay", type=float, default=1.0, help="Seconds between API calls")
    parser.add_argument(
        "--export-prompt-catalog",
        action="store_true",
        help="Export the complete 150-entry Markdown catalog and exit",
    )
    parser.add_argument(
        "--export-prompt-parts",
        action="store_true",
        help="Export one Markdown file per asset-plan section and exit",
    )
    args = parser.parse_args()

    model = args.model or (
        "gemini-2.5-flash-image" if args.provider == "gemini" else "gpt-image-1"
    )
    items = parse_plan(PLAN_PATH)
    if args.export_prompt_catalog:
        write_prompt_catalog(items, DEFAULT_PROMPT_CATALOG)
        print(f"Wrote {len(items)} prompts to {DEFAULT_PROMPT_CATALOG}")
        return 0
    if args.export_prompt_parts:
        write_prompt_parts(items, PROMPT_PARTS_DIR)
        print(f"Wrote {len(set(item['section'] for item in items))} prompt-part files to {PROMPT_PARTS_DIR}")
        return 0
    selected = [item for item in items if args.start <= int(item["number"]) <= args.end]
    manifest_path = args.output_dir / "generation_manifest.json"
    manifest: list[dict[str, Any]] = []
    if manifest_path.exists():
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    by_key = {entry["asset_key"]: entry for entry in manifest}
    generated = 0

    for item in selected:
        output_path = args.output_dir / item["filename"]
        prompt = build_prompt(item)
        entry = {
            **item,
            "prompt": prompt,
            "provider": args.provider,
            "model": model,
            "status": "pending",
        }
        if output_path.exists() and not args.overwrite:
            entry["status"] = "skipped_existing"
        elif args.dry_run:
            entry["status"] = "dry_run"
        elif args.limit is not None and generated >= args.limit:
            entry["status"] = "deferred_limit"
        else:
            try:
                generator = generate_gemini if args.provider == "gemini" else generate_openai
                save_bytes(output_path, generator(prompt, model))
                entry["status"] = "generated"
                generated += 1
                if args.delay:
                    time.sleep(args.delay)
            except Exception as exc:  # Keep the batch resumable after one provider failure.
                entry["status"] = "failed"
                entry["error"] = str(exc)
                print(f"FAILED {int(item['number']):03} {item['asset_key']}: {exc}", file=sys.stderr)
        by_key[item["asset_key"]] = entry
        args.output_dir.mkdir(parents=True, exist_ok=True)
        manifest_path.write_text(
            json.dumps(sorted(by_key.values(), key=lambda value: int(value["number"])), ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
        print(f"{entry['status'].upper():18} {int(item['number']):03} {item['asset_key']}")

    print(f"Processed {len(selected)} items; generated {generated} new image(s).")
    print(f"Manifest: {manifest_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())