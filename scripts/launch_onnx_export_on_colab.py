# -*- coding: utf-8 -*-
"""
Local Orchestrator: Launches Phase 2.6 ONNX Export on Remote Colab T4 GPU Session.
Connects to existing Colab session, executes export_indictrans_onnx.py,
downloads the resulting ONNX INT8 model package into models/mt/.
"""

import os
import sys
import subprocess
import time
import tarfile

# Force UTF-8 I/O for Windows consoles
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODELS_MT = os.path.join(BASE_DIR, "models", "mt")
COLAB_CLI = "/home/ashraf/.local/bin/colab"


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


def main():
    print("=" * 65)
    print("Phase 2.6 ONNX Export Orchestrator (Google Colab Tesla T4)")
    print("=" * 65)
    print(f"Base directory: {BASE_DIR}")
    print(f"Model output:   {MODELS_MT}")

    os.makedirs(MODELS_MT, exist_ok=True)

    # Step 1: Ensure remote GPU session is alive
    ensure_colab_session("phase2-train", gpu="T4")

    # Step 2: Sync repository code on Colab
    print("\n[ORCHESTRATOR] Syncing code on remote Colab session...")
    run_wsl(
        f"echo 'cd /content/Vernacular_Pedagogy && git pull origin main' | {COLAB_CLI} exec -s phase2-train",
        desc="Syncing repository code on Colab",
        timeout=60
    )

    # Step 3: Execute ONNX Export Cloud Worker
    print("\n[ORCHESTRATOR] Executing ONNX Export Cloud Worker...")
    export_script = "/mnt/c/Users/Ashraf/Desktop/26042/scripts/export_indictrans_onnx.py"
    code = run_wsl(
        f"TERM=xterm {COLAB_CLI} exec -s phase2-train --timeout 1800 -f {export_script}",
        desc="Running ONNX Export on Colab Tesla T4 GPU",
        timeout=1900
    )
    if code != 0:
        print("[FATAL] ONNX Export worker failed on Colab.")
        sys.exit(code)

    # Step 4: Download ONNX INT8 Package
    target_archive = os.path.join(MODELS_MT, "indictrans2_sat_onnx_int8.tar.gz")

    print("\n[ORCHESTRATOR] Downloading ONNX INT8 Model Package...")
    wsl_dest = "/mnt/c/Users/Ashraf/Desktop/26042/models/mt/indictrans2_sat_onnx_int8.tar.gz"
    code = run_wsl(
        f"{COLAB_CLI} download -s phase2-train /content/indictrans2_sat_onnx_int8.tar.gz {wsl_dest}",
        desc="Downloading ONNX INT8 Model Package",
        timeout=600
    )

    # Step 5: Verify and extract
    if os.path.exists(target_archive):
        size_mb = os.path.getsize(target_archive) / (1024 * 1024)
        print(f"\n{'=' * 65}")
        print(f"[SUCCESS] Downloaded ONNX INT8 Archive: {target_archive}")
        print(f"  Archive Size: {size_mb:.1f} MB")

        with tarfile.open(target_archive, "r:gz") as tf:
            for m in tf.getmembers():
                print(f"    - {m.name}: {m.size:,} bytes")
        print(f"{'=' * 65}")

        # Extract to models/mt/indictrans2_sat_onnx_int8/
        extract_dir = os.path.join(MODELS_MT, "indictrans2_sat_onnx_int8")
        if os.path.exists(extract_dir):
            import shutil
            shutil.rmtree(extract_dir)
        with tarfile.open(target_archive, "r:gz") as tf:
            tf.extractall(MODELS_MT)
        print(f"[OK] Extracted to: {extract_dir}")
    else:
        print(f"[ERROR] Expected archive not found at: {target_archive}")
        sys.exit(1)

    # Step 6: Clean up Colab Session
    print("\n[ORCHESTRATOR] Stopping Colab session to preserve compute credits...")
    run_wsl(f"{COLAB_CLI} stop -s phase2-train", desc="Stopping Colab Session", timeout=30)
    print("\n[PHASE 2.6 COMPLETE] ONNX INT8 model exported, downloaded, and extracted!")


if __name__ == "__main__":
    main()
