# -*- coding: utf-8 -*-
"""
Local Orchestrator: Launches Phase 3 Piper TTS Training on Remote Colab T4 GPU Session
Reads HF_TOKEN from git-ignored .env, provisions/verifies Colab session,
executes run_phase3_cloud_train.py with 3600s timeout, and downloads
the resulting ONNX TTS model package into models/tts/.
"""

import os
import sys
import subprocess
import time
import re
import tarfile

# Force UTF-8 I/O for Windows consoles and subprocess communication
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ENV_FILE = os.path.join(BASE_DIR, ".env")
MODELS_TTS = os.path.join(BASE_DIR, "models", "tts")

# Colab CLI binary path in WSL
COLAB_CLI = "/home/ashraf/.local/bin/colab"


def get_hf_token():
    """Retrieve HF_TOKEN from environment variable or .env file."""
    token = os.environ.get("HF_TOKEN")
    if token:
        return token.strip()
    if os.path.exists(ENV_FILE):
        with open(ENV_FILE, "r", encoding="utf-8") as f:
            for line in f:
                if line.strip().startswith("HF_TOKEN="):
                    return line.strip().split("=", 1)[1].strip().strip('"').strip("'")
    return ""


def sanitize_token(token):
    """Validate that HF token contains only safe characters to prevent shell injection."""
    if not token:
        return ""
    if not re.match(r'^[a-zA-Z0-9_\-]+$', token):
        print("[ERROR] HF_TOKEN contains unexpected characters. Aborting injection.", flush=True)
        sys.exit(1)
    return token


def run_wsl(command, desc=None, timeout=None):
    """Execute a command via WSL Ubuntu shell with streaming output and hard timeout."""
    if desc:
        print(f"\n[ORCHESTRATOR] {desc}", flush=True)
    print(f"Command: {command}", flush=True)
    try:
        process = subprocess.Popen(
            ["wsl", "-d", "Ubuntu", "bash", "-c", command],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
            bufsize=1
        )
        t0 = time.time()
        while True:
            line = process.stdout.readline()
            if line:
                print(line, end="", flush=True)
            elif process.poll() is not None:
                break
            if timeout and (time.time() - t0) > timeout:
                print(f"\n[ERROR] Command timed out after {timeout}s. Killing process...", flush=True)
                process.kill()
                process.wait()
                return -1
            time.sleep(0.02)
        return process.poll()
    except subprocess.TimeoutExpired:
        print(f"\n[ERROR] Command timed out after {timeout}s. Killing process...", flush=True)
        process.kill()
        process.wait()
        return -1
    except FileNotFoundError:
        print("[ERROR] WSL not found. Ensure Windows Subsystem for Linux is installed.", flush=True)
        return -1


def ensure_colab_session(session_name="phase3-tts", fallback_session="phase2-train", gpu="T4", max_retries=20, retry_delay=30):
    """Ensure a Colab GPU session is active, retrying gracefully if the GPU pool returns 503."""
    print(f"\n[ORCHESTRATOR] Checking Colab GPU sessions...", flush=True)

    for attempt in range(max_retries):
        for s_name in [session_name, fallback_session]:
            try:
                check = subprocess.run(
                    ["wsl", "-d", "Ubuntu", "bash", "-c", f"{COLAB_CLI} status -s {s_name}"],
                    capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=30
                )
                if check.returncode == 0 and ("Status:" in check.stdout or "Variant: GPU" in check.stdout or "IDLE" in check.stdout):
                    print(f"[ORCHESTRATOR] Active session '{s_name}' verified on Colab server.", flush=True)
                    return s_name
            except Exception:
                pass

        print(f"[ORCHESTRATOR] (Attempt {attempt+1}/{max_retries}) Provisioning fresh Tesla {gpu} GPU session '{session_name}'...", flush=True)
        code = run_wsl(f"{COLAB_CLI} new -s {session_name} --gpu {gpu}", desc=f"Provisioning Colab {gpu} GPU Session")
        if code == 0:
            time.sleep(6)
            return session_name

        print(f"[ORCHESTRATOR] Colab {gpu} GPU pool busy (503 Service Unavailable). Retrying in {retry_delay}s...", flush=True)
        time.sleep(retry_delay)

    print(f"[ERROR] Failed to provision Colab session after {max_retries} attempts.")
    sys.exit(1)


def inject_hf_token(token, session_name="phase3-tts"):
    """Safely inject HF_TOKEN into the remote Colab session."""
    if not token:
        print("[ORCHESTRATOR] No HF_TOKEN to inject. Skipping.", flush=True)
        return

    import base64
    token_b64 = base64.b64encode(token.encode()).decode()
    inject_script = (
        f"import os, base64; "
        f"t = base64.b64decode('{token_b64}').decode(); "
        f"os.environ['HF_TOKEN'] = t; "
        f"open('/content/.hf_token', 'w').write(t); "
        f"print('Remote HF_TOKEN configured!')"
    )
    inject_cmd = f"echo \"{inject_script}\" | {COLAB_CLI} exec -s {session_name} --timeout 120"
    code = run_wsl(inject_cmd, desc="Injecting HF_TOKEN into Colab Session", timeout=150)
    if code != 0:
        print("[WARN] HF_TOKEN injection failed. Continuing without token.", flush=True)


def main():
    token = get_hf_token()
    token = sanitize_token(token)

    print("=" * 65)
    print("Phase 3 Voice Synthesis Orchestrator (Piper TTS VITS on Colab T4)")
    print("=" * 65)
    print(f"HF Token detected: {'Yes' if token else 'No'}")
    print(f"Target directory: {MODELS_TTS}")

    os.makedirs(MODELS_TTS, exist_ok=True)

    # 1. Ensure remote GPU session is alive
    active_session = ensure_colab_session("phase3-tts", fallback_session="phase2-train", gpu="T4")

    # 2. Inject HF_TOKEN into remote Colab kernel
    inject_hf_token(token, session_name=active_session)

    # 3. Run Phase 3 Training Worker
    train_script = "/mnt/c/Users/Ashraf/Desktop/26042/scripts/run_phase3_cloud_train.py"
    train_cmd = f"TERM=xterm {COLAB_CLI} exec -s {active_session} --timeout 3600 -f {train_script}"
    t0 = time.time()
    code = run_wsl(train_cmd, desc=f"Starting Piper TTS Training Worker on {active_session} (T4 GPU)", timeout=3700)
    elapsed = (time.time() - t0) / 60

    if code != 0:
        print(f"\n[ERROR] Remote training failed with exit code {code} after {elapsed:.1f} minutes.")
        sys.exit(code)

    print(f"\n[OK] Training completed in {elapsed:.1f} minutes.")

    # 4. Download the ONNX model package
    target_archive = os.path.join(MODELS_TTS, "sat_piper_model.tar.gz")
    download_cmd = (
        f"{COLAB_CLI} download -s {active_session} "
        f"/content/sat_piper_model.tar.gz "
        f"/mnt/c/Users/Ashraf/Desktop/26042/models/tts/sat_piper_model.tar.gz"
    )
    code = run_wsl(download_cmd, desc="Downloading Piper TTS ONNX Model Package", timeout=600)

    if os.path.exists(target_archive):
        size_mb = os.path.getsize(target_archive) / (1024 * 1024)
        print(f"\n[OK] Model package downloaded ({size_mb:.1f} MB). Unpacking artifacts...")
        with tarfile.open(target_archive, "r:gz") as tar:
            tar.extractall(path=MODELS_TTS)
        print(f"\n{'=' * 65}")
        print(f"[PHASE 3 SUCCESS] Piper TTS Assets Ready in: {MODELS_TTS}")
        for fname in os.listdir(MODELS_TTS):
            fpath = os.path.join(MODELS_TTS, fname)
            if os.path.isfile(fpath):
                print(f"  - {fname}: {os.path.getsize(fpath)/(1024*1024):.2f} MB")
        print(f"  Total pipeline time: {elapsed:.1f} minutes")
        print(f"{'=' * 65}")
    else:
        print(f"\n[WARN] Expected archive not found: {target_archive}")


if __name__ == "__main__":
    main()
