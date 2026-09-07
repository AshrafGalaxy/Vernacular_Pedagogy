# -*- coding: utf-8 -*-
"""
Generate Self-Contained Interactive Audio Showcase Player
Embeds all 16 unseen dynamic sentences with base64 audio,
side-by-side Hindi/Ol Chiki scripts, metrics, and sequential playback.
"""

import os
import sys
import json
import base64

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPORT_PATH = os.path.join(BASE_DIR, "benchmark_reports", "unseen_dynamic_mt_test_report.json")
AUDIO_DIR = os.path.join(BASE_DIR, "assets", "demo_audio", "unseen_dynamic")
OUTPUT_HTML = os.path.join(BASE_DIR, "audio_evaluation_player.html")

with open(REPORT_PATH, "r", encoding="utf-8") as f:
    report = json.load(f)

items = report["results"]
summary = report["summary"]

# Embed base64 audio into each item
for item in items:
    wav_path = os.path.join(AUDIO_DIR, item["audio_file"])
    if os.path.exists(wav_path):
        with open(wav_path, "rb") as wf:
            b64 = base64.b64encode(wf.read()).decode("ascii")
            item["b64_audio"] = f"data:audio/wav;base64,{b64}"
            item["file_size_kb"] = round(os.path.getsize(wav_path) / 1024, 1)
    else:
        item["b64_audio"] = ""
        item["file_size_kb"] = 0

html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vernacular Pedagogy - Unseen Dynamic Sentence Audio Showcase</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Noto+Sans+Ol+Chiki:wght@500;700&display=swap" rel="stylesheet">
    <style>
        :root {{
            --bg-primary: #0b0f19;
            --bg-card: rgba(18, 26, 43, 0.85);
            --bg-card-hover: rgba(28, 39, 64, 0.95);
            --accent-primary: #6366f1;
            --accent-glow: rgba(99, 102, 241, 0.35);
            --accent-green: #10b981;
            --accent-cyan: #06b6d4;
            --text-main: #f3f4f6;
            --text-muted: #94a3b8;
            --border-color: rgba(255, 255, 255, 0.08);
        }}

        * {{
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }}

        body {{
            background: radial-gradient(circle at 50% 0%, #172033 0%, var(--bg-primary) 70%);
            color: var(--text-main);
            font-family: 'Outfit', sans-serif;
            padding: 2.5rem 1.5rem 5rem 1.5rem;
            min-height: 100vh;
        }}

        .container {{
            max-width: 1100px;
            margin: 0 auto;
        }}

        .header {{
            text-align: center;
            margin-bottom: 2.5rem;
        }}

        .badge {{
            display: inline-block;
            background: linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(6, 182, 212, 0.2));
            color: var(--accent-cyan);
            border: 1px solid rgba(6, 182, 212, 0.4);
            padding: 0.35rem 1rem;
            border-radius: 9999px;
            font-size: 0.85rem;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            margin-bottom: 1rem;
        }}

        h1 {{
            font-size: 2.4rem;
            font-weight: 700;
            background: linear-gradient(135deg, #ffffff 30%, #94a3b8 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            margin-bottom: 0.75rem;
        }}

        .subtitle {{
            color: var(--text-muted);
            font-size: 1.05rem;
            max-width: 700px;
            margin: 0 auto;
            line-height: 1.6;
        }}

        .metrics-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 1.25rem;
            margin-bottom: 2.5rem;
        }}

        .metric-card {{
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            padding: 1.25rem;
            border-radius: 1rem;
            backdrop-filter: blur(12px);
            text-align: center;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
            transition: transform 0.2s ease, border-color 0.2s ease;
        }}

        .metric-card:hover {{
            transform: translateY(-2px);
            border-color: rgba(99, 102, 241, 0.4);
        }}

        .metric-value {{
            font-size: 1.8rem;
            font-weight: 700;
            color: var(--accent-green);
            margin-bottom: 0.3rem;
        }}

        .metric-label {{
            font-size: 0.82rem;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.04em;
        }}

        .controls-bar {{
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            padding: 1rem 1.5rem;
            border-radius: 1rem;
            margin-bottom: 2rem;
        }}

        .btn {{
            background: linear-gradient(135deg, var(--accent-primary) 0%, #4f46e5 100%);
            color: #fff;
            border: none;
            padding: 0.7rem 1.4rem;
            border-radius: 0.6rem;
            font-weight: 600;
            font-size: 0.95rem;
            cursor: pointer;
            box-shadow: 0 4px 16px var(--accent-glow);
            transition: opacity 0.2s ease, transform 0.1s ease;
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
        }}

        .btn:hover {{
            opacity: 0.92;
            transform: translateY(-1px);
        }}

        .cards-list {{
            display: flex;
            flex-direction: column;
            gap: 1.25rem;
        }}

        .card {{
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: 1.1rem;
            padding: 1.5rem;
            backdrop-filter: blur(12px);
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
            transition: border-color 0.2s ease, transform 0.2s ease, background 0.2s ease;
        }}

        .card:hover {{
            border-color: rgba(99, 102, 241, 0.3);
            background: var(--bg-card-hover);
        }}

        .card.playing {{
            border-color: var(--accent-cyan);
            box-shadow: 0 0 24px rgba(6, 182, 212, 0.25);
        }}

        .card-top {{
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 1rem;
            padding-bottom: 0.75rem;
            border-bottom: 1px solid rgba(255, 255, 255, 0.05);
        }}

        .case-number {{
            font-size: 0.85rem;
            font-weight: 600;
            color: var(--accent-cyan);
            background: rgba(6, 182, 212, 0.12);
            padding: 0.25rem 0.6rem;
            border-radius: 0.4rem;
        }}

        .category-tag {{
            font-size: 0.8rem;
            color: var(--text-muted);
            background: rgba(255, 255, 255, 0.05);
            padding: 0.25rem 0.6rem;
            border-radius: 0.4rem;
        }}

        .text-grid {{
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 1.5rem;
            margin-bottom: 1.25rem;
        }}

        @media (max-width: 768px) {{
            .text-grid {{
                grid-template-columns: 1fr;
                gap: 1rem;
            }}
        }}

        .text-block {{
            display: flex;
            flex-direction: column;
            gap: 0.4rem;
        }}

        .label {{
            font-size: 0.75rem;
            text-transform: uppercase;
            color: var(--text-muted);
            letter-spacing: 0.05em;
            font-weight: 600;
        }}

        .hindi-text {{
            font-size: 1.25rem;
            font-weight: 500;
            color: #ffffff;
            line-height: 1.5;
        }}

        .santhali-text {{
            font-family: 'Noto Sans Ol Chiki', 'Outfit', sans-serif;
            font-size: 1.45rem;
            font-weight: 600;
            color: #38bdf8;
            line-height: 1.5;
        }}

        .player-row {{
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 1.5rem;
            flex-wrap: wrap;
            background: rgba(0, 0, 0, 0.35);
            padding: 0.85rem 1.25rem;
            border-radius: 0.75rem;
        }}

        audio {{
            flex: 1;
            height: 38px;
            min-width: 250px;
            outline: none;
        }}

        .latency-tags {{
            display: flex;
            gap: 0.75rem;
            font-size: 0.82rem;
            color: var(--text-muted);
            align-items: center;
        }}

        .pill {{
            background: rgba(255, 255, 255, 0.07);
            padding: 0.25rem 0.55rem;
            border-radius: 0.3rem;
            color: #cbd5e1;
            font-family: monospace;
        }}

        .pill.fast {{
            color: var(--accent-green);
            background: rgba(16, 185, 129, 0.12);
        }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="badge">Offline Edge Multi-Modal AI Demonstration</div>
            <h1>Unseen Dynamic Sentence Audio Showcase</h1>
            <p class="subtitle">
                Listening evaluation for 16 completely out-of-distribution, complex pedagogical sentences.
                Translations generated via <strong>IndicTrans2 INT8 (CTranslate2)</strong> and speech synthesized via <strong>Piper TTS VITS (16 kHz)</strong>.
            </p>
        </div>

        <div class="metrics-grid">
            <div class="metric-card">
                <div class="metric-value">{summary['total_unseen_sentences']}</div>
                <div class="metric-label">Unseen Sentences</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">{summary['avg_mt_latency_ms']} ms</div>
                <div class="metric-label">Avg MT Latency (CPU)</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">{summary['avg_total_latency_ms']} ms</div>
                <div class="metric-label">Avg End-to-End Turnaround</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">{summary['avg_rtf']}</div>
                <div class="metric-label">Avg RTF (>19x Real-Time)</div>
            </div>
        </div>

        <div class="controls-bar">
            <div>
                <strong>Interactive Listening Bench:</strong> Click any play button below or play all sequentially.
            </div>
            <button class="btn" id="playAllBtn" onclick="togglePlayAll()">
                <span>▶</span> Play All Sequentially
            </button>
        </div>

        <div class="cards-list">
"""

for item in items:
    html_content += f"""
            <div class="card" id="card-{item['id']}">
                <div class="card-top">
                    <span class="case-number">Sentence #{item['id']:02d}</span>
                    <span class="category-tag">{item['category']}</span>
                </div>
                <div class="text-grid">
                    <div class="text-block">
                        <span class="label">Hindi Source (Dynamic / Unseen)</span>
                        <div class="hindi-text">{item['source_hindi']}</div>
                    </div>
                    <div class="text-block">
                        <span class="label">Santhali Target (Generated Ol Chiki)</span>
                        <div class="santhali-text">{item['target_olchiki']}</div>
                    </div>
                </div>
                <div class="player-row">
                    <audio id="audio-{item['id']}" controls src="{item['b64_audio']}" onplay="onPlay({item['id']})" onended="onEnded({item['id']})"></audio>
                    <div class="latency-tags">
                        <span>MT: <span class="pill fast">{item['mt_latency_ms']} ms</span></span>
                        <span>TTS: <span class="pill fast">{item['tts_latency_ms']} ms</span></span>
                        <span>Total: <span class="pill">{item['total_latency_ms']} ms</span></span>
                        <span>Audio: <span class="pill">{item['audio_duration_s']}s</span></span>
                    </div>
                </div>
            </div>
    """

html_content += """
        </div>
    </div>

    <script>
        let isPlayingAll = false;
        let currentIndex = 1;
        const totalItems = """ + str(len(items)) + """;

        function onPlay(id) {
            document.querySelectorAll('.card').forEach(c => c.classList.remove('playing'));
            const card = document.getElementById('card-' + id);
            if (card) card.classList.add('playing');
        }

        function onEnded(id) {
            const card = document.getElementById('card-' + id);
            if (card) card.classList.remove('playing');

            if (isPlayingAll) {
                if (currentIndex < totalItems) {
                    currentIndex++;
                    playIndex(currentIndex);
                } else {
                    stopPlayAll();
                }
            }
        }

        function playIndex(idx) {
            const audio = document.getElementById('audio-' + idx);
            if (audio) {
                audio.scrollIntoView({ behavior: 'smooth', block: 'center' });
                audio.play();
            }
        }

        function togglePlayAll() {
            const btn = document.getElementById('playAllBtn');
            if (!isPlayingAll) {
                isPlayingAll = true;
                currentIndex = 1;
                btn.innerHTML = '<span>⏸</span> Stop Autoplay';
                playIndex(currentIndex);
            } else {
                stopPlayAll();
            }
        }

        function stopPlayAll() {
            isPlayingAll = false;
            const btn = document.getElementById('playAllBtn');
            btn.innerHTML = '<span>▶</span> Play All Sequentially';
            const audio = document.getElementById('audio-' + currentIndex);
            if (audio) audio.pause();
            document.querySelectorAll('.card').forEach(c => c.classList.remove('playing'));
        }
    </script>
</body>
</html>
"""

with open(OUTPUT_HTML, "w", encoding="utf-8") as f:
    f.write(html_content)

print(f"[OK] Showcase HTML generated at: {OUTPUT_HTML} ({os.path.getsize(OUTPUT_HTML)/(1024*1024):.2f} MB)")
