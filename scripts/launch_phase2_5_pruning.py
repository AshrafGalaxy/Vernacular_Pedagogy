# -*- coding: utf-8 -*-
"""
Local Orchestrator for Phase 2.5 Vocabulary Pruning on Google Colab.
Connects to Colab Tesla T4 instance, executes vocabulary pruning and
CTranslate2 INT8 re-quantization, and downloads the pruned package (~65 MB).
"""

import os
import sys
import time
import subprocess
import tarfile

# Fix Windows console encoding
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODELS_MT = os.path.join(BASE_DIR, "models", "mt")
COLAB_CLI = "/home/ashraf/.local/bin/colab"


def run_wsl(command, desc="", timeout=3600):
    """Executes a bash command inside WSL Ubuntu environment."""
    wsl_cmd = ["wsl", "bash", "-c", command]
    print(f"\n[ORCHESTRATOR] {desc or 'Executing WSL Command'}")
    print(f"Command: {command}")
    try:
        res = subprocess.run(wsl_cmd, timeout=timeout)
        return res.returncode
    except subprocess.TimeoutExpired:
        print(f"[ERROR] Command timed out after {timeout} seconds: {command}")
        return 1
    except Exception as err:
        print(f"[ERROR] Failed to execute WSL command: {err}")
        return 1


def ensure_colab_session(session_name="phase2-train", gpu="T4", max_retries=2):
    """Ensures that an active Google Colab GPU session exists."""
    for attempt in range(max_retries + 1):
        print(f"\n[ORCHESTRATOR] Checking Colab session '{session_name}' (attempt {attempt + 1}/{max_retries + 1})...")
        try:
            res = subprocess.run(
                ["wsl", "bash", "-c", f"{COLAB_CLI} status -s {session_name}"],
                capture_output=True, text=True, timeout=30
            )
            output = (res.stdout + res.stderr).strip()
            if res.returncode == 0 and ("running" in output.lower() or "connected" in output.lower() or "idle" in output.lower()):
                print(f"[ORCHESTRATOR] Active session '{session_name}' verified on Colab server.")
                return True
        except Exception as e:
            print(f"[WARN] Session status check failed: {e}")

        if attempt < max_retries:
            print(f"[ORCHESTRATOR] Session '{session_name}' not active. Provisioning fresh Tesla {gpu} GPU session...")
            code = run_wsl(f"{COLAB_CLI} new -s {session_name} --gpu {gpu}", desc=f"Provisioning Google Colab {gpu} GPU Session")
            if code == 0:
                time.sleep(8)
                continue
            else:
                print(f"[WARN] Provisioning attempt {attempt + 1} failed.")

    print(f"[ERROR] Failed to provision Colab session '{session_name}'.")
    sys.exit(1)


def main():
    print("=" * 65)
    print("Phase 2.5: Vocabulary Pruning Orchestrator (Google Colab Tesla T4)")
    print("=" * 65)

    ensure_colab_session("phase2-train", gpu="T4")

    # Step 1: Sync repository code on Colab
    print("\n[ORCHESTRATOR] Syncing code on remote Colab session...")
    run_wsl(
        f"echo 'cd /content/Vernacular_Pedagogy && git pull origin main' | {COLAB_CLI} exec -s phase2-train",
        desc="Syncing repository code on Colab",
        timeout=60
    )

    # Step 2: Execute Pruning Cloud Worker
    print("\n[ORCHESTRATOR] Executing Vocabulary Pruning Cloud Worker...")
    code = run_wsl(
        f"TERM=xterm {COLAB_CLI} exec -s phase2-train --timeout 1800 -f /mnt/c/Users/Ashraf/Desktop/26042/scripts/run_phase2_vocab_pruning_cloud.py",
        desc="Running Vocabulary Pruning on Colab Tesla T4 GPU",
        timeout=1900
    )
    if code != 0:
        print("[FATAL] Vocabulary Pruning worker failed on Colab.")
        sys.exit(code)

    # Step 3: Download Pruned Package
    target_archive = os.path.join(MODELS_MT, "indictrans2_sat_int8_ct2.tar.gz")
    backup_archive = os.path.join(MODELS_MT, "indictrans2_sat_int8_ct2_unpruned.tar.gz")

    if os.path.exists(target_archive):
        print(f"\n[ORCHESTRATOR] Backing up previous unpruned archive to: {backup_archive}")
        if os.path.exists(backup_archive):
            os.remove(backup_archive)
        os.rename(target_archive, backup_archive)

    print("\n[ORCHESTRATOR] Downloading Pruned CTranslate2 INT8 Model Package...")
    wsl_dest = "/mnt/c/Users/Ashraf/Desktop/26042/models/mt/indictrans2_sat_int8_ct2.tar.gz"
    code = run_wsl(
        f"{COLAB_CLI} download -s phase2-train /content/indictrans2_sat_int8_ct2_pruned.tar.gz {wsl_dest}",
        desc="Downloading Pruned CTranslate2 INT8 Model Package",
        timeout=300
    )

    # Step 4: Verify Downloaded Package
    if os.path.exists(target_archive):
        size_mb = os.path.getsize(target_archive) / (1024 * 1024)
        print(f"\n{'=' * 65}")
        print(f"[SUCCESS] Downloaded Pruned INT8 Archive: {target_archive}")
        print(f"  Archive Size: {size_mb:.1f} MB (compared to ~286.7 MB unpruned!)")
        
        with tarfile.open(target_archive, "r:gz") as tf:
            for m in tf.getmembers():
                print(f"    - {m.name}: {m.size:,} bytes")
        print(f"{'=' * 65}")
    else:
        print(f"[ERROR] Expected archive not found at: {target_archive}")
        sys.exit(1)

    # Step 5: Clean up Colab Session to preserve credits
    print("\n[ORCHESTRATOR] Stopping Colab session to preserve compute credits...")
    run_wsl(f"{COLAB_CLI} stop -s phase2-train", desc="Stopping Colab Session", timeout=30)
    print("\n[PHASE 2.5 COMPLETE] Model successfully pruned, re-quantized, and verified!")


if __name__ == "__main__":
    main()
