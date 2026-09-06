# -*- coding: utf-8 -*-
"""
Phase 2.5: Cloud Worker for Vocabulary Pruning & CTranslate2 INT8 Re-Quantization
Executed on Google Colab Tesla T4 GPU.

Workflow:
1. Ingests merged IndicTrans2 model (/content/indictrans2_sat_merged)
2. Executes Vocabulary & Embedding Pruning (/content/indictrans2_sat_pruned)
3. Quantizes to CTranslate2 INT8 format with compact vocabularies
4. Packages /content/indictrans2_sat_int8_ct2_pruned.tar.gz (~65 MB)
"""

import os
import sys
import shutil
import time
import torch

# Ensure repository scripts directory is in sys.path
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
if SCRIPT_DIR not in sys.path:
    sys.path.insert(0, SCRIPT_DIR)

from run_phase2_cloud_train import (
    setup_transformers_compat_shims,
    patch_remote_tokenizer,
    patch_remote_modeling,
    run_cmd_strict,
)
from prune_indictrans_vocab import prune_indictrans_model


def main():
    t_start = time.time()
    print("=" * 65)
    print("PHASE 2.5: IndicTrans2 Vocabulary Pruning & CTranslate2 INT8")
    print("=" * 65)
    print(f"CUDA Available: {torch.cuda.is_available()}")
    if torch.cuda.is_available():
        print(f"GPU Device:     {torch.cuda.get_device_name(0)}")
        print(f"Total Memory:   {torch.cuda.get_device_properties(0).total_memory / (1024**3):.2f} GB")

    # Step 1: Environment & Compatibility Shims
    print("\n--- Step 1: Applying Compatibility Shims ---")
    setup_transformers_compat_shims()

    merged_path = "/content/indictrans2_sat_merged"
    pruned_path = "/content/indictrans2_sat_pruned"
    ct2_output = "/content/indictrans2_sat_int8_ct2"
    tar_output = "/content/indictrans2_sat_int8_ct2_pruned.tar.gz"

    hf_token = None
    if os.path.exists("/content/.hf_token"):
        with open("/content/.hf_token", "r") as f:
            hf_token = f.read().strip()

    if not os.path.exists(merged_path):
        raise FileNotFoundError(
            f"Merged model not found at {merged_path}. Please ensure Phase 2 has run."
        )

    # Patch remote code
    patch_remote_tokenizer(merged_path, auth_token=hf_token)
    patch_remote_modeling(auth_token=hf_token)

    # Step 2: Vocabulary & Embedding Pruning
    print("\n--- Step 2: Executing Vocabulary & Embedding Pruning ---")
    if os.path.exists(pruned_path):
        shutil.rmtree(pruned_path)

    orig_src, pruned_src, orig_tgt, pruned_tgt = prune_indictrans_model(
        model_dir=merged_path,
        output_dir=pruned_path,
        device="cpu"
    )

    # Patch pruned model files as well
    patch_remote_tokenizer(pruned_path, auth_token=hf_token)
    patch_remote_modeling(auth_token=hf_token)

    # Step 3: Quantize Pruned Model to CTranslate2 INT8
    print("\n--- Step 3: Quantizing Pruned Model to CTranslate2 INT8 ---")
    if os.path.exists(ct2_output):
        shutil.rmtree(ct2_output)

    from transformers import AutoModelForSeq2SeqLM, AutoTokenizer
    import ctranslate2.converters.transformers as ct_tr

    print("[CT2] Loading pruned model on CPU for quantization...")
    pruned_model = AutoModelForSeq2SeqLM.from_pretrained(
        pruned_path,
        trust_remote_code=True,
        low_cpu_mem_usage=True,
        torch_dtype=torch.float32
    ).cpu()

    pruned_tokenizer = AutoTokenizer.from_pretrained(pruned_path, trust_remote_code=True)

    class PrunedIndicTransLoader(ct_tr.M2M100Loader):
        @property
        def architecture_name(self):
            return "AutoModelForSeq2SeqLM"

        def get_model_class(self, config, model_class):
            return AutoModelForSeq2SeqLM

        def get_model_spec(self, model):
            model.config.normalize_before = True
            model.config.normalize_embedding = True
            return ct_tr.BartLoader.get_model_spec(self, model)

        def get_vocabulary(self, model, tokenizer):
            if hasattr(tokenizer, "src_encoder") and hasattr(tokenizer, "tgt_encoder"):
                src_vocab = [None] * len(tokenizer.src_encoder)
                for token, idx in tokenizer.src_encoder.items():
                    if idx < len(src_vocab):
                        src_vocab[idx] = token
                tgt_vocab = [None] * len(tokenizer.tgt_encoder)
                for token, idx in tokenizer.tgt_encoder.items():
                    if idx < len(tgt_vocab):
                        tgt_vocab[idx] = token
                self._src_vocab = [t or "<unk>" for t in src_vocab]
                self._tgt_vocab = [t or "<unk>" for t in tgt_vocab]
                return self._src_vocab
            return super().get_vocabulary(model, tokenizer)

        def set_vocabulary(self, spec, tokens):
            if hasattr(self, "_src_vocab") and hasattr(self, "_tgt_vocab"):
                spec.register_source_vocabulary(self._src_vocab)
                spec.register_target_vocabulary(self._tgt_vocab)
                print(f"  [VOCAB] Registered compact dual vocabularies: src={len(self._src_vocab):,}, tgt={len(self._tgt_vocab):,}")
            else:
                spec.register_source_vocabulary(tokens)
                spec.register_target_vocabulary(tokens)

    ct_tr._MODEL_LOADERS["IndicTransConfig"] = PrunedIndicTransLoader()

    class InProcessConverter(ct_tr.TransformersConverter):
        def load_model(self, model_class, model_name_or_path, **kwargs):
            return pruned_model
        def load_tokenizer(self, tokenizer_class, model_name_or_path, **kwargs):
            return pruned_tokenizer

    print("[CT2] Converting pruned model to CTranslate2 INT8 in-process...")
    converter = InProcessConverter(
        model_name_or_path=pruned_path,
        trust_remote_code=True,
        low_cpu_mem_usage=True
    )
    converter.convert(ct2_output, quantization="int8")
    print("[OK] CTranslate2 INT8 quantization complete!")

    # Step 4: Package Pruned Model Artifact
    print("\n--- Step 4: Packaging Pruned INT8 Model Artifact ---")
    if os.path.exists(tar_output):
        os.remove(tar_output)

    run_cmd_strict(
        f"cd /content && tar -czf {tar_output} indictrans2_sat_int8_ct2/",
        description="Packaging pruned CTranslate2 model"
    )

    elapsed_min = (time.time() - t_start) / 60
    if os.path.exists(tar_output):
        size_mb = os.path.getsize(tar_output) / (1024 * 1024)
        model_bin = os.path.join(ct2_output, "model.bin")
        bin_mb = os.path.getsize(model_bin) / (1024 * 1024) if os.path.exists(model_bin) else 0

        print(f"\n{'=' * 65}")
        print("[PHASE 2.5 SUCCESS] Pruned CTranslate2 INT8 Package Ready!")
        print(f"  Path:             {tar_output}")
        print(f"  Compressed Size:  {size_mb:.1f} MB")
        print(f"  Uncompressed Bin: {bin_mb:.1f} MB")
        print(f"  Source Vocab:     {pruned_src:,} tokens (down from {orig_src:,})")
        print(f"  Target Vocab:     {pruned_tgt:,} tokens (down from {orig_tgt:,})")
        print(f"  Execution Time:   {elapsed_min:.1f} minutes")
        print(f"{'=' * 65}")
    else:
        raise FileNotFoundError(f"Failed to create package: {tar_output}")


if __name__ == "__main__":
    main()
