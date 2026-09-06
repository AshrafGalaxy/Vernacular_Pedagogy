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

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ENV_FILE = os.path.join(BASE_DIR, ".env")
MODELS_MT = os.path.join(BASE_DIR, "models", "mt")

def get_hf_token():
    token = os.environ.get("HF_TOKEN")
    if token:
        return token.strip()
    if os.path.exists(ENV_FILE):
        with open(ENV_FILE, "r", encoding="utf-8") as f:
            for line in f:
                if line.strip().startswith("HF_TOKEN="):
                    return line.strip().split("=", 1)[1].strip().strip('"').strip("'")
    return ""

def run_wsl(command, desc=None):
    if desc:
        print(f"\n[ORCHESTRATOR] {desc}", flush=True)
    print(f"Command: {command}", flush=True)
    process = subprocess.Popen(
        ["wsl", "-d", "Ubuntu", "bash", "-c", command],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1
    )
    if process.stdout:
        for line in iter(process.stdout.readline, ""):
            print(line, end="", flush=True)
        process.stdout.close()
    return process.wait()

def main():
    token = get_hf_token()
    print("=" * 65)
    print("Phase 2 Cloud Training Orchestrator (Google Colab Tesla T4)")
    print("=" * 65)
    print(f"HF Token detected: {'Yes (Length ' + str(len(token)) + ')' if token else 'No'}")

    os.makedirs(MODELS_MT, exist_ok=True)

    # 1. Inject HF_TOKEN into remote Colab kernel os.environ and /content/.hf_token
    if token:
        inject_script = f"import os; os.environ['HF_TOKEN'] = '{token}'; open('/content/.hf_token', 'w').write('{token}'); print('Remote HF_TOKEN configured!')"
        inject_cmd = f"echo \"{inject_script}\" | /home/ashraf/.local/bin/colab exec -s phase2-train"
        run_wsl(inject_cmd, desc="Injecting HF_TOKEN into Colab Session")

    # 2. Run Phase 2 Training with 3600s timeout
    train_script = "/mnt/c/Users/Ashraf/Desktop/26042/scripts/run_phase2_cloud_train.py"
    train_cmd = f"TERM=xterm /home/ashraf/.local/bin/colab exec -s phase2-train --timeout 3600 -f {train_script}"
    code = run_wsl(train_cmd, desc="Starting Phase 2 Training Worker on Colab T4 GPU")
    
    if code != 0:
        print(f"[ERROR] Remote training failed with exit code {code}")
        sys.exit(code)

    # 3. Download the quantized INT8 model package
    target_archive = os.path.join(MODELS_MT, "indictrans2_sat_int8_ct2.tar.gz")
    download_cmd = f"/home/ashraf/.local/bin/colab download -s phase2-train /content/indictrans2_sat_int8_ct2.tar.gz /mnt/c/Users/Ashraf/Desktop/26042/models/mt/indictrans2_sat_int8_ct2.tar.gz"
    run_wsl(download_cmd, desc="Downloading CTranslate2 INT8 Model Package")

    if os.path.exists(target_archive):
        print(f"\n[SUCCESS] Model successfully downloaded to: {target_archive} ({os.path.getsize(target_archive)/(1024*1024):.1f} MB)")
    else:
        print(f"[WARN] Expected {target_archive} not found after download.")

if __name__ == "__main__":
    main()
