# -*- coding: utf-8 -*-
"""
Update audio_evaluation_player.html with the new 24-epoch synthesized audio clips.
"""
import os
import re
import base64

BASE_DIR = r"c:\Users\Ashraf\Desktop\26042"
HTML_PATH = os.path.join(BASE_DIR, "audio_evaluation_player.html")
AUDIO_DIR = os.path.join(BASE_DIR, "assets", "demo_audio", "unseen_dynamic")
ARTIFACT_HTML = r"C:\Users\Ashraf\.gemini\antigravity-ide\brain\22f4a990-e354-4022-98a5-322e94d941d0\audio_evaluation_player.html"

with open(HTML_PATH, "r", encoding="utf-8") as f:
    content = f.read()

for i in range(1, 17):
    wav_path = os.path.join(AUDIO_DIR, f"unseen_dynamic_{i:02d}.wav")
    if os.path.exists(wav_path):
        with open(wav_path, "rb") as af:
            b64 = base64.b64encode(af.read()).decode("ascii")
        data_url = f"data:audio/wav;base64,{b64}"
        # Pattern matching: id="audio-i" controls src="..."
        pattern = rf'(<audio id="audio-{i}" controls src=")[^"]*(")'
        content = re.sub(pattern, rf'\g<1>{data_url}\g<2>', content)
        print(f"Updated audio-{i} ({len(b64)} chars)")

with open(HTML_PATH, "w", encoding="utf-8") as f:
    f.write(content)

# Copy to artifact directory
with open(ARTIFACT_HTML, "w", encoding="utf-8") as f:
    f.write(content)

print(f"HTML Player updated: {HTML_PATH}")
print(f"Artifact HTML Player updated: {ARTIFACT_HTML}")
