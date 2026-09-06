# pyright: reportMissingImports=false
# -*- coding: utf-8 -*-
"""
Phase 2 Cloud Training Worker (Runs on Google Colab T4 GPU VM)
Automates the full MT pipeline on the remote Colab instance:
1. Clones/pulls Vernacular_Pedagogy repo.
2. Ingests authentic AI4Bharat BPCC & IN22 datasets via HF Token.
3. Merges with verified Grade 1-3 NIPUN Bharat pedagogical bitext.
4. Fine-tunes IndicTrans2 320M Distilled with LoRA on NVIDIA T4 GPU.
5. Merges weights and quantizes to CTranslate2 INT8 format (~65 MB).
6. Packages /content/indictrans2_sat_int8_ct2.tar.gz ready for download.

Note: Heavy ML dependencies (torch, transformers, peft, datasets, IndicTransToolkit)
are provisioned remotely in the Colab VM runtime and intentionally omitted from the
local workstation to adhere to zero-heavy-local-compute project guardrails.
"""

import os
import sys
import subprocess
import time

def run_cmd(cmd, cwd=None):
    print(f"\n[EXEC] {cmd}")
    res = subprocess.run(cmd, shell=True, cwd=cwd)
    if res.returncode != 0:
        print(f"[WARN/ERROR] Command exited with code {res.returncode}")
    return res.returncode

def main():
    print("=" * 60)
    print("PHASE 2: IndicTrans2 LoRA Cloud Training & CTranslate2 INT8")
    print("=" * 60)
    
    # Check GPU
    import torch  # type: ignore
    print(f"CUDA Available: {torch.cuda.is_available()}")
    if torch.cuda.is_available():
        print(f"GPU Device:     {torch.cuda.get_device_name(0)}")
        print(f"Total Memory:   {torch.cuda.get_device_properties(0).total_memory / (1024**3):.2f} GB")
    else:
        print("[WARNING] CUDA not detected. Training will be slow.")

    # 1. Install Training Dependencies
    print("\n--- Step 1: Installing Cloud Dependencies ---")
    run_cmd("pip install -q torch transformers datasets evaluate sacrebleu peft bitsandbytes accelerate sentencepiece ctranslate2 huggingface_hub")
    if not os.path.exists("/content/IndicTransToolkit"):
        run_cmd("git clone https://github.com/VarunGumma/IndicTransToolkit.git /content/IndicTransToolkit")
    run_cmd("pip install -q -e /content/IndicTransToolkit")

    # 2. Clone/Update Repo
    print("\n--- Step 2: Syncing Repository ---")
    repo_dir = "/content/Vernacular_Pedagogy"
    if not os.path.exists(repo_dir):
        run_cmd(f"git clone https://github.com/AshrafGalaxy/Vernacular_Pedagogy.git {repo_dir}")
    else:
        run_cmd("git pull origin main", cwd=repo_dir)

    # 3. Ingest Authentic Bitext Datasets
    print("\n--- Step 3: Fetching Authentic AI4Bharat BPCC & IN22 Data ---")
    hf_token = os.environ.get("HF_TOKEN", "")
    token_arg = f"--hf-token {hf_token}" if hf_token else ""
    run_cmd(f"python scripts/01_fetch_bpcc_bitext.py {token_arg}", cwd=repo_dir)
    run_cmd("python scripts/03_bitext_normalizer.py", cwd=repo_dir)

    # 4. Load Models & Tokenizer
    print("\n--- Step 4: Loading IndicTrans2 Base Model ---")
    import sys
    if "/content/IndicTransToolkit" not in sys.path:
        sys.path.insert(0, "/content/IndicTransToolkit")
    try:
        from IndicTransToolkit import IndicProcessor  # type: ignore
    except (ImportError, ModuleNotFoundError):
        from IndicTransToolkit.IndicTransToolkit import IndicProcessor  # type: ignore

    from transformers import AutoModelForSeq2SeqLM, AutoTokenizer, Seq2SeqTrainingArguments, Seq2SeqTrainer  # type: ignore
    from peft import LoraConfig, get_peft_model, TaskType, PeftModel  # type: ignore
    from datasets import Dataset  # type: ignore
    import pandas as pd  # type: ignore

    model_name = "ai4bharat/indictrans2-indic-indic-dist-320M"
    src_lang = "hin_Deva"
    tgt_lang = "sat_Olck"

    tokenizer = AutoTokenizer.from_pretrained(model_name, trust_remote_code=True)
    base_model = AutoModelForSeq2SeqLM.from_pretrained(
        model_name,
        trust_remote_code=True,
        torch_dtype=torch.float16,
        device_map="auto"
    )
    ip = IndicProcessor(inference=False)

    # 5. Setup LoRA
    print("\n--- Step 5: Configuring LoRA Adapter ---")
    lora_config = LoraConfig(
        task_type=TaskType.SEQ_2_SEQ_LM,
        r=16,
        lora_alpha=32,
        lora_dropout=0.05,
        target_modules=["q_proj", "v_proj", "k_proj", "out_proj"],
        bias="none"
    )
    peft_model = get_peft_model(base_model, lora_config)
    peft_model.print_trainable_parameters()

    # 6. Load & Tokenize Datasets
    print("\n--- Step 6: Tokenizing Bitext ---")
    train_tsv = os.path.join(repo_dir, "data", "processed", "bitext", "train.tsv")
    val_tsv = os.path.join(repo_dir, "data", "processed", "bitext", "val.tsv")

    def load_ds(path):
        df = pd.read_csv(path, sep="\t")
        src = df["source"].astype(str).tolist()
        tgt = df["target"].astype(str).tolist()
        prepped = ip.preprocess_batch(src, src_lang=src_lang, tgt_lang=tgt_lang)
        return Dataset.from_dict({"source": prepped, "target": tgt})

    train_ds = load_ds(train_tsv)
    val_ds = load_ds(val_tsv)

    def tokenize_fn(examples):
        inputs = tokenizer(examples["source"], max_length=128, truncation=True, padding="max_length")
        labels = tokenizer(text_target=examples["target"], max_length=128, truncation=True, padding="max_length")
        labels["input_ids"] = [
            [(l if l != tokenizer.pad_token_id else -100) for l in label]
            for label in labels["input_ids"]
        ]
        inputs["labels"] = labels["input_ids"]
        return inputs

    tok_train = train_ds.map(tokenize_fn, batched=True, remove_columns=["source", "target"])
    tok_val = val_ds.map(tokenize_fn, batched=True, remove_columns=["source", "target"])
    print(f"Samples: Train={len(tok_train)}, Val={len(tok_val)}")

    # 7. Fine-Tune Model
    print("\n--- Step 7: Starting GPU LoRA Fine-Tuning ---")
    out_lora = "/content/indictrans2_sat_lora"
    training_args = Seq2SeqTrainingArguments(
        output_dir=out_lora,
        per_device_train_batch_size=8,
        per_device_eval_batch_size=8,
        gradient_accumulation_steps=2,
        learning_rate=3e-4,
        num_train_epochs=3,
        fp16=torch.cuda.is_available(),
        evaluation_strategy="epoch",
        save_strategy="epoch",
        save_total_limit=1,
        logging_steps=20,
        report_to="none"
    )

    trainer = Seq2SeqTrainer(
        model=peft_model,
        args=training_args,
        train_dataset=tok_train,
        eval_dataset=tok_val,
        tokenizer=tokenizer
    )

    t0 = time.time()
    trainer.train()
    print(f"Training completed in {(time.time() - t0)/60:.1f} minutes!")
    trainer.save_model("/content/indictrans2_sat_lora_final")

    # 8. Merge LoRA Weights
    print("\n--- Step 8: Merging LoRA Weights with Base Model ---")
    raw_base = AutoModelForSeq2SeqLM.from_pretrained(model_name, trust_remote_code=True)
    merged = PeftModel.from_pretrained(raw_base, "/content/indictrans2_sat_lora_final")
    merged = merged.merge_and_unload()
    merged_path = "/content/indictrans2_sat_merged"
    merged.save_pretrained(merged_path)
    tokenizer.save_pretrained(merged_path)
    print("Merged model saved to:", merged_path)

    # 9. Quantize to CTranslate2 INT8
    print("\n--- Step 9: Quantizing to CTranslate2 INT8 ---")
    ct2_path = "/content/indictrans2_sat_int8_ct2"
    run_cmd(f"ctranslate2-transformers-converter --model {merged_path} --output_dir {ct2_path} --quantization int8 --low_cpu_mem_usage")

    # 10. Package Model Artifact
    print("\n--- Step 10: Packaging INT8 Model Artifact ---")
    tar_path = "/content/indictrans2_sat_int8_ct2.tar.gz"
    run_cmd(f"cd /content && tar -czf {tar_path} indictrans2_sat_int8_ct2/")
    if os.path.exists(tar_path):
        size_mb = os.path.getsize(tar_path) / (1024 * 1024)
        print(f"\n[PHASE 2 SUCCESS] Final INT8 Model Package Ready: {tar_path} ({size_mb:.1f} MB)")
    else:
        print("[ERROR] Packaging failed.")

if __name__ == "__main__":
    main()
