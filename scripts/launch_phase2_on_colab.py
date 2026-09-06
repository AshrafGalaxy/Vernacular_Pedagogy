# -*- coding: utf-8 -*-
"""
Local Orchestrator: Launches Phase 2 Training on Remote Colab T4 GPU Session
Reads HF_TOKEN from git-ignored .env, injects into Colab session,
executes run_phase2_cloud_train.py with 3600s timeout, and downloads
the resulting CTranslate2 INT8 model into models/mt/.
"""

import os
import sys
import subprocess
import time
import re

# Force UTF-8 I/O for Windows consoles and subprocess communication
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ENV_FILE = os.path.join(BASE_DIR, ".env")
MODELS_MT = os.path.join(BASE_DIR, "models", "mt")

# Colab CLI binary path
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
    """Execute a command via WSL Ubuntu shell with streaming output."""
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
        if process.stdout:
            for line in iter(process.stdout.readline, ""):
                print(line, end="", flush=True)
            process.stdout.close()
        return process.wait(timeout=timeout)
    except subprocess.TimeoutExpired:
        print(f"\n[ERROR] Command timed out after {timeout}s. Killing process...", flush=True)
        process.kill()
        process.wait()
        return -1
    except FileNotFoundError:
        print("[ERROR] WSL not found. Ensure Windows Subsystem for Linux is installed.", flush=True)
        return -1


def ensure_colab_session(session_name="phase2-train", gpu="T4", max_retries=2):
    """Ensure a Colab GPU session is active and responsive, provisioning one if necessary."""
    print(f"\n[ORCHESTRATOR] Checking Colab session '{session_name}'...", flush=True)

    for attempt in range(max_retries + 1):
        try:
            check = subprocess.run(
                ["wsl", "-d", "Ubuntu", "bash", "-c", f"{COLAB_CLI} status -s {session_name}"],
                capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=30
            )
            # colab status verifies active connection to the Colab server VM
            if check.returncode == 0 and ("Status:" in check.stdout or "Variant: GPU" in check.stdout):
                print(f"[ORCHESTRATOR] Active session '{session_name}' verified on Colab server.", flush=True)
                return True
            else:
                print(f"[INFO] Session '{session_name}' status check: {check.stdout.strip()}", flush=True)
        except subprocess.TimeoutExpired:
            print(f"[WARN] Session status check timed out (attempt {attempt + 1}/{max_retries + 1}).", flush=True)
        except FileNotFoundError:
            print("[ERROR] WSL not found. Cannot check Colab sessions.", flush=True)
            sys.exit(1)

        if attempt < max_retries:
            print(f"[ORCHESTRATOR] Session '{session_name}' not active. "
                  f"Provisioning fresh Tesla {gpu} GPU session (attempt {attempt + 1})...", flush=True)
            code = run_wsl(
                f"{COLAB_CLI} new -s {session_name} --gpu {gpu}",
                desc=f"Provisioning Google Colab {gpu} GPU Session"
            )
            if code == 0:
                time.sleep(6)
                continue
            else:
                print(f"[WARN] Provisioning attempt {attempt + 1} failed.", flush=True)

    print(f"[ERROR] Failed to provision Colab session '{session_name}' after {max_retries + 1} attempts.")
    sys.exit(1)


def inject_hf_token(token, session_name="phase2-train"):
    """Safely inject HF_TOKEN into the remote Colab session."""
    if not token:
        print("[ORCHESTRATOR] No HF_TOKEN to inject. Skipping.", flush=True)
        return

    # Use base64 encoding to avoid any shell escaping issues
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
        print("[WARN] HF_TOKEN injection failed or timed out. Attempting self-healing recovery...", flush=True)
        # Attempt 1: Restart kernel
        run_wsl(f"{COLAB_CLI} restart-kernel -s {session_name}", desc="Restarting Colab Kernel", timeout=45)
        time.sleep(5)
        code = run_wsl(inject_cmd, desc="Retrying HF_TOKEN injection after kernel restart", timeout=150)
        if code != 0:
            # Attempt 2: Re-provision fresh session
            print("[WARN] Kernel unresponsive. Stopping and provisioning fresh session...", flush=True)
            run_wsl(f"{COLAB_CLI} stop -s {session_name}", timeout=30)
            ensure_colab_session(session_name, gpu="T4")
            time.sleep(6)
            run_wsl(inject_cmd, desc="Injecting HF_TOKEN into fresh session", timeout=150)


def main():
    token = get_hf_token()
    token = sanitize_token(token)

    print("=" * 65)
    print("Phase 2 Cloud Training Orchestrator (Google Colab Tesla T4)")
    print("=" * 65)
    print(f"HF Token detected: {'Yes (Length ' + str(len(token)) + ')' if token else 'No'}")
    print(f"Base directory: {BASE_DIR}")
    print(f"Model output: {MODELS_MT}")

    os.makedirs(MODELS_MT, exist_ok=True)

    # 0. Ensure remote GPU session is alive
    ensure_colab_session("phase2-train", gpu="T4")

    # 1. Inject HF_TOKEN into remote Colab kernel
    inject_hf_token(token, session_name="phase2-train")

    # 2. Run Phase 2 Training with 3600s timeout
    train_script = "/mnt/c/Users/Ashraf/Desktop/26042/scripts/run_phase2_cloud_train.py"
    train_cmd = f"TERM=xterm {COLAB_CLI} exec -s phase2-train --timeout 3600 -f {train_script}"
    t0 = time.time()
    code = run_wsl(train_cmd, desc="Starting Phase 2 Training Worker on Colab T4 GPU", timeout=3700)
    elapsed = (time.time() - t0) / 60

    if code != 0:
        print(f"\n[ERROR] Remote training failed with exit code {code} after {elapsed:.1f} minutes.")
        print("Check the Colab session logs for detailed error output.")
        sys.exit(code)

    print(f"\n[OK] Training completed in {elapsed:.1f} minutes.")

    # 3. Download the quantized INT8 model package
    target_archive = os.path.join(MODELS_MT, "indictrans2_sat_int8_ct2.tar.gz")
    download_cmd = (
        f"{COLAB_CLI} download -s phase2-train "
        f"/content/indictrans2_sat_int8_ct2.tar.gz "
        f"/mnt/c/Users/Ashraf/Desktop/26042/models/mt/indictrans2_sat_int8_ct2.tar.gz"
    )
    code = run_wsl(download_cmd, desc="Downloading CTranslate2 INT8 Model Package", timeout=600)

    if os.path.exists(target_archive):
        size_mb = os.path.getsize(target_archive) / (1024 * 1024)
        print(f"\n{'=' * 65}")
        print(f"[SUCCESS] Model downloaded to: {target_archive}")
        print(f"  Size: {size_mb:.1f} MB")
        print(f"  Total pipeline time: {elapsed:.1f} minutes")
        print(f"{'=' * 65}")
    else:
        print(f"\n[WARN] Expected archive not found: {target_archive}")
        print("You may need to manually download from the Colab session.")
        if code != 0:
            sys.exit(1)


if __name__ == "__main__":
    main()
