# -*- coding: utf-8 -*-
"""
Terminal Audio Player for Unseen Dynamic Sentences
Allows playing any synthesized WAV audio clip directly through PC speakers.
"""

import os
import sys
import argparse

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
AUDIO_DIR = os.path.join(BASE_DIR, "assets", "demo_audio", "unseen_dynamic")


def play_wav(filepath: str):
    """Play a WAV file on Windows using winsound / PowerShell."""
    if not os.path.exists(filepath):
        print(f"[ERROR] File not found: {filepath}")
        return

    print(f"[PLAYING] {os.path.basename(filepath)}...")
    if sys.platform == "win32":
        try:
            import winsound
            winsound.PlaySound(filepath, winsound.SND_FILENAME)
        except Exception:
            os.system(f'powershell -c "(New-Object Media.SoundPlayer \'{filepath}\').PlaySync()"')
    else:
        os.system(f'aplay "{filepath}" 2>/dev/null || ffplay -nodisp -autoexit "{filepath}" 2>/dev/null')


def main():
    parser = argparse.ArgumentParser(description="Terminal Audio Player for Unseen Sentences")
    parser.add_argument("--track", type=int, default=1, help="Track number to play (1 to 16)")
    parser.add_argument("--all", action="store_true", help="Play all 16 tracks sequentially")
    args = parser.parse_args()

    if args.all:
        for i in range(1, 17):
            wav_name = f"unseen_dynamic_{i:02d}.wav"
            wav_path = os.path.join(AUDIO_DIR, wav_name)
            play_wav(wav_path)
    else:
        wav_name = f"unseen_dynamic_{args.track:02d}.wav"
        wav_path = os.path.join(AUDIO_DIR, wav_name)
        play_wav(wav_path)


if __name__ == "__main__":
    main()
