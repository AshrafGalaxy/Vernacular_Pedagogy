# -*- coding: utf-8 -*-
"""
Local Orchestrator: Launches Phase 2.6 ONNX Export on Remote Colab T4 GPU Session.
Connects to or provisions Colab session, checks for merged FP32 model (training if needed),
executes export_indictrans_onnx.py, downloads the resulting ONNX INT8 model package,
and copies models directly into android/app/src/main/assets/models/mt/.
"""

import os
import sys
import subprocess
import time
import tarfile
import shutil
import re

# Force UTF-8 I/O for Windows consoles
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ENV_FILE = os.path.join(BASE_DIR, ".env")
MODELS_MT = os.path.join(BASE_DIR, "models", "mt")
ANDROID_ASSETS_MT = os.path.join(BASE_DIR, "android", "app", "src", "main", "assets", "models", "mt")
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
    """Validate that HF token contains only safe characters."""
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
    """Ensure a Colab GPU session is active and responsive."""
    print(f"\n[ORCHESTRATOR] Checking Colab session '{session_name}'...", flush=True)

    for attempt in range(max_retries + 1):
        try:
            check = subprocess.run(
                ["wsl", "-d", "Ubuntu", "bash", "-c", f"{COLAB_CLI} status -s {session_name}"],
                capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=30
            )
            if check.returncode == 0 and ("Status:" in check.stdout or "Variant: GPU" in check.stdout):
                print(f"[ORCHESTRATOR] Active session '{session_name}' verified.", flush=True)
                return True
            else:
                print(f"[INFO] Session status: {check.stdout.strip()}", flush=True)
        except subprocess.TimeoutExpired:
            print(f"[WARN] Status check timed out (attempt {attempt + 1}/{max_retries + 1}).", flush=True)
        except FileNotFoundError:
            print("[ERROR] WSL not found.", flush=True)
            sys.exit(1)

        if attempt < max_retries:
            print(f"[ORCHESTRATOR] Provisioning fresh Tesla {gpu} GPU session (attempt {attempt + 1})...", flush=True)
            code = run_wsl(
                f"{COLAB_CLI} new -s {session_name} --gpu {gpu}",
                desc=f"Provisioning Google Colab {gpu} GPU Session"
            )
            if code == 0:
                time.sleep(8)
                continue

    print(f"[ERROR] Failed to provision Colab session '{session_name}'.")
    sys.exit(1)


def inject_hf_token(token, session_name="phase2-train"):
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
        print("[WARN] HF_TOKEN injection failed or timed out. Continuing...", flush=True)


def main():
    token = get_hf_token()
    token = sanitize_token(token)

    print("=" * 65)
    print("Phase 2.6 ONNX Export Orchestrator (Google Colab Tesla T4)")
    print("=" * 65)
    print(f"HF Token detected: {'Yes (Length ' + str(len(token)) + ')' if token else 'No'}")
    print(f"Base directory:    {BASE_DIR}")
    print(f"Model output:      {MODELS_MT}")
    print(f"Android assets:    {ANDROID_ASSETS_MT}")

    os.makedirs(MODELS_MT, exist_ok=True)
    os.makedirs(ANDROID_ASSETS_MT, exist_ok=True)

    # Step 1: Ensure remote GPU session is alive
    ensure_colab_session("phase2-train", gpu="T4")

    # Step 2: Inject HF Token
    inject_hf_token(token, session_name="phase2-train")

    # Step 3: Sync repository code on Colab
    print("\n[ORCHESTRATOR] Syncing code on remote Colab session...")
    sync_code = run_wsl(
        f"echo 'cd /content/Vernacular_Pedagogy && git pull origin main' | {COLAB_CLI} exec -s phase2-train",
        desc="Syncing repository code on Colab",
        timeout=60
    )
    if sync_code != 0:
        print("[INFO] Repository not present yet, will be cloned if training runs.")

    # Step 4: Check if merged FP32 model exists on Colab
    import base64
    check_py = (
        "import os, sys\n"
        "p = '/content/indictrans2_sat_merged'\n"
        "has_w = os.path.exists(os.path.join(p, 'model.safetensors')) or os.path.exists(os.path.join(p, 'pytorch_model.bin'))\n"
        "print('MERGED_EXISTS=' + str(has_w))\n"
        "sys.exit(0 if has_w else 1)\n"
    )
    b64 = base64.b64encode(check_py.encode()).decode()
    check_code = run_wsl(
        f"echo \"import base64; exec(base64.b64decode('{b64}'))\" | {COLAB_CLI} exec -s phase2-train",
        desc="Verifying Merged FP32 Model Status",
        timeout=60
    )

    if check_code != 0:
        print("\n" + "=" * 65)
        print("[ORCHESTRATOR] Merged FP32 model not found on Colab disk.")
        print("Running Phase 2 Training & Merging first (~10-12 mins on T4 GPU)...")
        print("=" * 65)
        train_script = "/mnt/c/Users/Ashraf/Desktop/26042/scripts/run_phase2_cloud_train.py"
        code = run_wsl(
            f"TERM=xterm {COLAB_CLI} exec -s phase2-train --timeout 3600 -f {train_script}",
            desc="Executing Phase 2 Cloud Training & Merging Worker",
            timeout=3700
        )
        if code != 0:
            print(f"[FATAL] Phase 2 training worker failed with exit code {code}.")
            sys.exit(code)

    # Step 5: Execute ONNX Export Cloud Worker
    print("\n" + "=" * 65)
    print("[ORCHESTRATOR] Executing ONNX Export Cloud Worker (~5-7 mins)...")
    print("=" * 65)
    export_script = "/mnt/c/Users/Ashraf/Desktop/26042/scripts/export_indictrans_onnx.py"
    code = run_wsl(
        f"TERM=xterm {COLAB_CLI} exec -s phase2-train --timeout 1800 -f {export_script}",
        desc="Running ONNX Export & INT8 Quantization on Colab T4 GPU",
        timeout=1900
    )
    if code != 0:
        print(f"[FATAL] ONNX Export worker failed on Colab with exit code {code}.")
        sys.exit(code)

    # Step 6: Download ONNX INT8 Package
    target_archive = os.path.join(MODELS_MT, "indictrans2_sat_onnx_int8.tar.gz")
    print("\n[ORCHESTRATOR] Downloading ONNX INT8 Model Package...")
    wsl_dest = "/mnt/c/Users/Ashraf/Desktop/26042/models/mt/indictrans2_sat_onnx_int8.tar.gz"
    code = run_wsl(
        f"{COLAB_CLI} download -s phase2-train /content/indictrans2_sat_onnx_int8.tar.gz {wsl_dest}",
        desc="Downloading ONNX INT8 Model Package",
        timeout=600
    )

    # Step 7: Verify, extract, and deploy into Android assets
    if os.path.exists(target_archive):
        size_mb = os.path.getsize(target_archive) / (1024 * 1024)
        print(f"\n{'=' * 65}")
        print(f"[SUCCESS] Downloaded ONNX INT8 Archive: {target_archive}")
        print(f"  Archive Size: {size_mb:.1f} MB")

        # Extract to models/mt/indictrans2_sat_onnx_int8/
        extract_dir = os.path.join(MODELS_MT, "indictrans2_sat_onnx_int8")
        if os.path.exists(extract_dir):
            shutil.rmtree(extract_dir)
        with tarfile.open(target_archive, "r:gz") as tf:
            tf.extractall(MODELS_MT)
        print(f"[OK] Extracted to: {extract_dir}")

        # Deploy files directly to Android assets (android/app/src/main/assets/models/mt/)
        print(f"\n[ORCHESTRATOR] Deploying ONNX models & vocabularies to Android assets...")
        deployed_files = []
        for root, _, files in os.walk(extract_dir):
            for file in files:
                src_path = os.path.join(root, file)
                dst_path = os.path.join(ANDROID_ASSETS_MT, file)
                shutil.copy2(src_path, dst_path)
                f_size = os.path.getsize(dst_path) / (1024 * 1024)
                deployed_files.append((file, f_size))

        print(f"[DEPLOY] Deployed {len(deployed_files)} files to {ANDROID_ASSETS_MT}:")
        for name, size in deployed_files:
            print(f"  ✓ {name} ({size:.1f} MB)")
        print(f"{'=' * 65}")
    else:
        print(f"[ERROR] Expected archive not found at: {target_archive}")
        sys.exit(1)

    # Step 8: Clean up Colab Session to preserve credits
    print("\n[ORCHESTRATOR] Stopping Colab session to preserve compute credits...")
    run_wsl(f"{COLAB_CLI} stop -s phase2-train", desc="Stopping Colab Session", timeout=30)
    print("\n[PHASE 2.6 COMPLETE] ONNX INT8 model exported, downloaded, and bundled into Android app!")


if __name__ == "__main__":
    main()
