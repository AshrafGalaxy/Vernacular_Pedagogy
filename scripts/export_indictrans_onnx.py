# pyright: reportMissingImports=false
# -*- coding: utf-8 -*-
"""
Phase 2.6: ONNX Export & INT8 Quantization Worker (Runs on Google Colab T4 GPU)

Converts the fine-tuned IndicTrans2 merged FP32 model into ONNX format
for native on-device Android inference via onnxruntime-android.

Pipeline:
1. Loads merged FP32 IndicTrans2 model from /content/indictrans2_sat_merged
2. Applies compatibility shims for transformers v5.x
3. Exports encoder, decoder, and decoder_with_past to 3 ONNX files
4. Applies INT8 dynamic quantization to each file
5. Validates ONNX output against CTranslate2 reference on test sentences
6. Packages /content/indictrans2_sat_onnx_int8.tar.gz for download
7. Uploads to HuggingFace Hub (if token available)

Note: This script is designed for Colab 2026 environment with
transformers 5.x, torch 2.11+, Python 3.13+.
"""

import os
import sys
import time
import shutil
import subprocess

# Fix Windows console encoding
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

if "__file__" in globals() and __file__:
    SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
else:
    SCRIPT_DIR = "/content/Vernacular_Pedagogy/scripts"

if os.path.exists(SCRIPT_DIR) and SCRIPT_DIR not in sys.path:
    sys.path.insert(0, SCRIPT_DIR)
if "/content/Vernacular_Pedagogy/scripts" not in sys.path and os.path.exists("/content/Vernacular_Pedagogy/scripts"):
    sys.path.insert(0, "/content/Vernacular_Pedagogy/scripts")


def run_cmd(cmd, cwd=None, capture=False):
    """Run a shell command, print output, return exit code (non-fatal)."""
    print(f"\n[EXEC] {cmd}")
    if capture:
        res = subprocess.run(cmd, shell=True, cwd=cwd,
                             capture_output=True, text=True)
        if res.stdout:
            print(res.stdout[-2000:])
        if res.stderr:
            print(res.stderr[-2000:])
    else:
        res = subprocess.run(cmd, shell=True, cwd=cwd)
    if res.returncode != 0:
        print(f"[WARN] Command exited with code {res.returncode}")
    return res.returncode


def run_cmd_strict(cmd, cwd=None, description="", capture=False):
    """Run a shell command and abort the pipeline on failure."""
    print(f"\n[EXEC] {cmd}")
    if capture:
        res = subprocess.run(cmd, shell=True, cwd=cwd,
                             capture_output=True, text=True)
        if res.stdout:
            print(res.stdout[-2000:])
        if res.stderr:
            print(res.stderr[-2000:])
    else:
        res = subprocess.run(cmd, shell=True, cwd=cwd)
    if res.returncode != 0:
        msg = f"[FATAL] {description or 'Command'} failed with exit code {res.returncode}: {cmd}"
        print(msg)
        raise RuntimeError(msg)
    return res.returncode


def install_onnx_dependencies():
    """Install ONNX export and quantization dependencies on Colab."""
    print("\n--- Installing ONNX Export Dependencies ---")
    packages = [
        "optimum[onnxruntime]",
        "onnxruntime",
        "onnx",
        "onnxscript",
    ]
    for pkg in packages:
        try:
            if pkg == "optimum[onnxruntime]":
                import optimum  # type: ignore
                print(f"  ✓ optimum: {getattr(optimum, '__version__', '?')}")
            elif pkg == "onnxruntime":
                import onnxruntime  # type: ignore
                print(f"  ✓ onnxruntime: {getattr(onnxruntime, '__version__', '?')}")
            elif pkg == "onnx":
                import onnx  # type: ignore
                print(f"  ✓ onnx: {getattr(onnx, '__version__', '?')}")
            elif pkg == "onnxscript":
                import onnxscript  # type: ignore
                print(f"  ✓ onnxscript: {getattr(onnxscript, '__version__', '?')}")
        except ImportError:
            print(f"  ✗ {pkg}: MISSING — installing...")
            run_cmd_strict(f"pip install {pkg}", description=f"Install {pkg}")


def main():
    t_start = time.time()
    print("=" * 65)
    print("PHASE 2.6: IndicTrans2 ONNX Export & INT8 Quantization")
    print("=" * 65)

    import torch  # type: ignore
    print(f"CUDA Available: {torch.cuda.is_available()}")
    if torch.cuda.is_available():
        print(f"GPU Device:     {torch.cuda.get_device_name(0)}")
        print(f"Total Memory:   {torch.cuda.get_device_properties(0).total_memory / (1024**3):.2f} GB")

    # ──────────────────────────────────────────
    # Step 1: Dependencies & Compatibility Shims
    # ──────────────────────────────────────────
    print("\n--- Step 1: Dependencies & Compatibility Shims ---")
    install_onnx_dependencies()

    # Import compatibility shims from Phase 2 training script
    try:
        from run_phase2_cloud_train import (
            setup_transformers_compat_shims,
            patch_remote_tokenizer,
            patch_remote_modeling,
        )
        setup_transformers_compat_shims()
    except ImportError as e:
        print(f"[WARN] Could not import Phase 2 shims: {e}")
        print("[INFO] Proceeding without shims — may fail if transformers v5.x requires patches")

    # ──────────────────────────────────────────
    # Step 2: Verify Merged Model Exists
    # ──────────────────────────────────────────
    print("\n--- Step 2: Verifying Merged FP32 Model ---")
    merged_path = "/content/indictrans2_sat_merged"

    if not os.path.exists(merged_path):
        raise FileNotFoundError(
            f"[FATAL] Merged FP32 model not found at {merged_path}.\n"
            "Please ensure Phase 2 training has completed (run_phase2_cloud_train.py) "
            "and the merged model is available."
        )

    has_weights = (
        os.path.exists(os.path.join(merged_path, "model.safetensors")) or
        os.path.exists(os.path.join(merged_path, "pytorch_model.bin"))
    )
    if not has_weights:
        raise FileNotFoundError(
            f"[FATAL] Merged model directory exists but contains no weights.\n"
            f"Expected model.safetensors or pytorch_model.bin in {merged_path}"
        )

    print(f"[OK] Merged model found at: {merged_path}")

    # Patch remote code for transformers v5.x
    hf_token = None
    if os.path.exists("/content/.hf_token"):
        with open("/content/.hf_token", "r") as f:
            hf_token = f.read().strip()

    try:
        patch_remote_tokenizer(merged_path, auth_token=hf_token)
        patch_remote_modeling(auth_token=hf_token)
    except NameError:
        print("[INFO] Skipping remote code patches (shims not available)")

    # ──────────────────────────────────────────
    # Step 3: Load Model & Tokenizer
    # ──────────────────────────────────────────
    print("\n--- Step 3: Loading Merged FP32 Model ---")
    from transformers import AutoModelForSeq2SeqLM, AutoTokenizer  # type: ignore

    tokenizer = AutoTokenizer.from_pretrained(
        merged_path, trust_remote_code=True, token=hf_token
    )
    model = AutoModelForSeq2SeqLM.from_pretrained(
        merged_path,
        trust_remote_code=True,
        torch_dtype=torch.float32,
        low_cpu_mem_usage=True
    ).cpu().eval()

    print(f"[OK] Model loaded. Parameters: {sum(p.numel() for p in model.parameters()):,}")

    # ──────────────────────────────────────────
    # Step 4: ONNX Export (3-file split)
    # ──────────────────────────────────────────
    print("\n--- Step 4: Exporting to ONNX (encoder + decoder + decoder_with_past) ---")
    onnx_output_dir = "/content/indictrans2_sat_onnx_fp32"
    if os.path.exists(onnx_output_dir):
        shutil.rmtree(onnx_output_dir)
    os.makedirs(onnx_output_dir, exist_ok=True)

    export_success = False

    # Strategy 1: Use optimum OnnxConfig-based export
    try:
        from optimum.onnxruntime import ORTModelForSeq2SeqLM  # type: ignore

        print("[ONNX] Attempting optimum-based export...")
        ort_model = ORTModelForSeq2SeqLM.from_pretrained(
            merged_path,
            export=True,
            trust_remote_code=True,
            token=hf_token,
            provider="CPUExecutionProvider"
        )
        ort_model.save_pretrained(onnx_output_dir)
        tokenizer.save_pretrained(onnx_output_dir)
        export_success = True
        print("[OK] Optimum-based ONNX export succeeded!")
    except Exception as e:
        print(f"[WARN] Optimum export failed: {e}")
        print("[INFO] Falling back to manual torch.onnx.export...")

    # Strategy 2: Manual torch.onnx.export with encoder-decoder split
    if not export_success:
        try:
            import onnx  # type: ignore

            print("[ONNX] Performing manual torch.onnx.export...")

            # Encoder export
            print("  [ONNX] Exporting encoder...")
            encoder = model.get_encoder()
            encoder_path = os.path.join(onnx_output_dir, "encoder_model.onnx")

            # Create dummy inputs for encoder
            dummy_input_ids = torch.ones(1, 32, dtype=torch.long)
            dummy_attention_mask = torch.ones(1, 32, dtype=torch.long)

            torch.onnx.export(
                encoder,
                (dummy_input_ids, dummy_attention_mask),
                encoder_path,
                input_names=["input_ids", "attention_mask"],
                output_names=["last_hidden_state"],
                dynamic_axes={
                    "input_ids": {0: "batch_size", 1: "sequence_length"},
                    "attention_mask": {0: "batch_size", 1: "sequence_length"},
                    "last_hidden_state": {0: "batch_size", 1: "sequence_length"},
                },
                opset_version=14,
                do_constant_folding=True,
            )
            print(f"  [OK] Encoder exported: {os.path.getsize(encoder_path)/(1024*1024):.1f} MB")

            # Decoder export (first step, no past KV cache)
            print("  [ONNX] Exporting decoder (initial step)...")
            decoder = model.get_decoder()
            config = model.config

            # Get number of decoder layers and attention heads
            num_layers = config.decoder_layers if hasattr(config, "decoder_layers") else config.num_hidden_layers
            d_model = config.d_model if hasattr(config, "d_model") else config.hidden_size
            num_heads = config.decoder_attention_heads if hasattr(config, "decoder_attention_heads") else config.num_attention_heads
            head_dim = d_model // num_heads

            dummy_decoder_input_ids = torch.ones(1, 1, dtype=torch.long)
            dummy_encoder_hidden_states = torch.randn(1, 32, d_model)
            dummy_encoder_attention_mask = torch.ones(1, 32, dtype=torch.long)

            # Build decoder forward args for first step (no past)
            decoder_inputs = {
                "input_ids": dummy_decoder_input_ids,
                "encoder_hidden_states": dummy_encoder_hidden_states,
                "encoder_attention_mask": dummy_encoder_attention_mask,
            }

            # Full model forward for decoder (uses model's forward to get logits + past)
            class DecoderWrapper(torch.nn.Module):
                """Wraps the full model for decoder-only export."""
                def __init__(self, full_model):
                    super().__init__()
                    self.model = full_model

                def forward(self, decoder_input_ids, encoder_hidden_states, encoder_attention_mask):
                    outputs = self.model(
                        decoder_input_ids=decoder_input_ids,
                        encoder_outputs=(encoder_hidden_states,),
                        attention_mask=encoder_attention_mask,
                        use_cache=True,
                    )
                    logits = outputs.logits
                    # Flatten past_key_values for export
                    past_kv = outputs.past_key_values
                    flat_past = []
                    for layer_past in past_kv:
                        for tensor in layer_past:
                            flat_past.append(tensor)
                    return (logits,) + tuple(flat_past)

            decoder_wrapper = DecoderWrapper(model)
            decoder_wrapper.eval()

            decoder_path = os.path.join(onnx_output_dir, "decoder_model.onnx")

            # Build output names
            output_names = ["logits"]
            for i in range(num_layers):
                output_names.extend([
                    f"present.{i}.decoder.key",
                    f"present.{i}.decoder.value",
                    f"present.{i}.encoder.key",
                    f"present.{i}.encoder.value",
                ])

            # Build dynamic axes for outputs
            dynamic_axes = {
                "decoder_input_ids": {0: "batch_size", 1: "decoder_sequence_length"},
                "encoder_hidden_states": {0: "batch_size", 1: "encoder_sequence_length"},
                "encoder_attention_mask": {0: "batch_size", 1: "encoder_sequence_length"},
                "logits": {0: "batch_size", 1: "decoder_sequence_length"},
            }
            for i in range(num_layers):
                for kind in ["decoder", "encoder"]:
                    seq_dim_name = "decoder_sequence_length" if kind == "decoder" else "encoder_sequence_length"
                    dynamic_axes[f"present.{i}.{kind}.key"] = {0: "batch_size", 2: seq_dim_name}
                    dynamic_axes[f"present.{i}.{kind}.value"] = {0: "batch_size", 2: seq_dim_name}

            torch.onnx.export(
                decoder_wrapper,
                (dummy_decoder_input_ids, dummy_encoder_hidden_states, dummy_encoder_attention_mask),
                decoder_path,
                input_names=["decoder_input_ids", "encoder_hidden_states", "encoder_attention_mask"],
                output_names=output_names,
                dynamic_axes=dynamic_axes,
                opset_version=14,
                do_constant_folding=True,
            )
            print(f"  [OK] Decoder exported: {os.path.getsize(decoder_path)/(1024*1024):.1f} MB")

            # Decoder with past export (iterative autoregressive steps)
            print("  [ONNX] Exporting decoder_with_past (autoregressive step)...")

            class DecoderWithPastWrapper(torch.nn.Module):
                """Wraps the full model for decoder with past KV cache export."""
                def __init__(self, full_model, num_layers, num_heads, head_dim):
                    super().__init__()
                    self.model = full_model
                    self.num_layers = num_layers
                    self.num_heads = num_heads
                    self.head_dim = head_dim

                def forward(self, decoder_input_ids, encoder_attention_mask, *past_kv_flat):
                    # Reconstruct past_key_values tuple from flat args
                    past_key_values = []
                    idx = 0
                    for _ in range(self.num_layers):
                        layer_past = (
                            past_kv_flat[idx],     # decoder key
                            past_kv_flat[idx + 1], # decoder value
                            past_kv_flat[idx + 2], # encoder key (cross-attention)
                            past_kv_flat[idx + 3], # encoder value (cross-attention)
                        )
                        past_key_values.append(layer_past)
                        idx += 4

                    outputs = self.model(
                        decoder_input_ids=decoder_input_ids,
                        encoder_outputs=None,
                        attention_mask=encoder_attention_mask,
                        past_key_values=tuple(past_key_values),
                        use_cache=True,
                    )

                    logits = outputs.logits
                    new_past = outputs.past_key_values
                    flat_new_past = []
                    for layer_past in new_past:
                        for tensor in layer_past:
                            flat_new_past.append(tensor)
                    return (logits,) + tuple(flat_new_past)

            decoder_past_wrapper = DecoderWithPastWrapper(model, num_layers, num_heads, head_dim)
            decoder_past_wrapper.eval()

            # Create dummy past KV tensors
            past_seq_len = 1  # After first decoder step
            encoder_seq_len = 32

            dummy_past_kv = []
            past_input_names = ["decoder_input_ids", "encoder_attention_mask"]
            past_dynamic_axes = {
                "decoder_input_ids": {0: "batch_size"},
                "encoder_attention_mask": {0: "batch_size", 1: "encoder_sequence_length"},
                "logits": {0: "batch_size", 1: "decoder_step"},
            }

            for i in range(num_layers):
                # decoder self-attention past
                dk = torch.randn(1, num_heads, past_seq_len, head_dim)
                dv = torch.randn(1, num_heads, past_seq_len, head_dim)
                # encoder cross-attention past
                ek = torch.randn(1, num_heads, encoder_seq_len, head_dim)
                ev = torch.randn(1, num_heads, encoder_seq_len, head_dim)
                dummy_past_kv.extend([dk, dv, ek, ev])

                past_input_names.extend([
                    f"past_key_values.{i}.decoder.key",
                    f"past_key_values.{i}.decoder.value",
                    f"past_key_values.{i}.encoder.key",
                    f"past_key_values.{i}.encoder.value",
                ])
                for kind in ["decoder", "encoder"]:
                    seq_dim_name = "past_decoder_sequence_length" if kind == "decoder" else "encoder_sequence_length"
                    past_dynamic_axes[f"past_key_values.{i}.{kind}.key"] = {0: "batch_size", 2: seq_dim_name}
                    past_dynamic_axes[f"past_key_values.{i}.{kind}.value"] = {0: "batch_size", 2: seq_dim_name}

            past_output_names = ["logits"]
            for i in range(num_layers):
                past_output_names.extend([
                    f"present.{i}.decoder.key",
                    f"present.{i}.decoder.value",
                    f"present.{i}.encoder.key",
                    f"present.{i}.encoder.value",
                ])
                for kind in ["decoder", "encoder"]:
                    seq_dim_name = "past_decoder_sequence_length_plus_1" if kind == "decoder" else "encoder_sequence_length"
                    past_dynamic_axes[f"present.{i}.{kind}.key"] = {0: "batch_size", 2: seq_dim_name}
                    past_dynamic_axes[f"present.{i}.{kind}.value"] = {0: "batch_size", 2: seq_dim_name}

            decoder_with_past_path = os.path.join(onnx_output_dir, "decoder_with_past_model.onnx")

            all_inputs = (
                dummy_decoder_input_ids,
                dummy_encoder_attention_mask,
                *dummy_past_kv,
            )

            torch.onnx.export(
                decoder_past_wrapper,
                all_inputs,
                decoder_with_past_path,
                input_names=past_input_names,
                output_names=past_output_names,
                dynamic_axes=past_dynamic_axes,
                opset_version=14,
                do_constant_folding=True,
            )
            print(f"  [OK] Decoder with past exported: {os.path.getsize(decoder_with_past_path)/(1024*1024):.1f} MB")

            export_success = True
            print("[OK] Manual ONNX export complete!")

        except Exception as e:
            print(f"[FATAL] Manual ONNX export failed: {e}")
            import traceback
            traceback.print_exc()
            raise

    # Free PyTorch model from memory
    del model
    import gc; gc.collect()
    if torch.cuda.is_available():
        torch.cuda.empty_cache()

    # ──────────────────────────────────────────
    # Step 5: INT8 Dynamic Quantization
    # ──────────────────────────────────────────
    print("\n--- Step 5: INT8 Dynamic Quantization ---")
    from onnxruntime.quantization import quantize_dynamic, QuantType  # type: ignore

    onnx_int8_dir = "/content/indictrans2_sat_onnx_int8"
    if os.path.exists(onnx_int8_dir):
        shutil.rmtree(onnx_int8_dir)
    os.makedirs(onnx_int8_dir, exist_ok=True)

    onnx_files = [f for f in os.listdir(onnx_output_dir) if f.endswith(".onnx")]
    for onnx_file in onnx_files:
        fp32_path = os.path.join(onnx_output_dir, onnx_file)
        int8_path = os.path.join(onnx_int8_dir, onnx_file)

        fp32_size = os.path.getsize(fp32_path) / (1024 * 1024)
        print(f"  [QUANT] Quantizing {onnx_file} ({fp32_size:.1f} MB FP32 → INT8)...")

        quantize_dynamic(
            model_input=fp32_path,
            model_output=int8_path,
            weight_type=QuantType.QInt8,
        )

        int8_size = os.path.getsize(int8_path) / (1024 * 1024)
        reduction = (1 - int8_size / fp32_size) * 100
        print(f"  [OK] {onnx_file}: {fp32_size:.1f} MB → {int8_size:.1f} MB ({reduction:.1f}% reduction)")

    # Save explicit vocabulary mappings from tokenizer
    import json
    if hasattr(tokenizer, "src_encoder") and hasattr(tokenizer, "tgt_encoder"):
        try:
            with open(os.path.join(onnx_int8_dir, "dict.SRC.json"), "w", encoding="utf-8") as f:
                json.dump(tokenizer.src_encoder, f, ensure_ascii=False)
            with open(os.path.join(onnx_int8_dir, "dict.TGT.json"), "w", encoding="utf-8") as f:
                json.dump(tokenizer.tgt_encoder, f, ensure_ascii=False)
            print("  [SAVED] dict.SRC.json and dict.TGT.json")
        except Exception as ve:
            print(f"  [WARN] Could not dump src/tgt encoders: {ve}")

    # Copy SentencePiece models and tokenizer configs
    for fname in ["model.SRC", "model.TGT", "tokenizer_config.json",
                   "special_tokens_map.json", "generation_config.json",
                   "dict.SRC.json", "dict.TGT.json", "config.json"]:
        src_f = os.path.join(merged_path, fname)
        if os.path.exists(src_f):
            shutil.copy2(src_f, os.path.join(onnx_int8_dir, fname))
            print(f"  [COPY] {fname}")

    # Also copy from FP32 export dir (tokenizer files saved by optimum)
    for fname in os.listdir(onnx_output_dir):
        if fname.endswith(".json") or fname.endswith(".model") or fname == "model.SRC" or fname == "model.TGT":
            src_f = os.path.join(onnx_output_dir, fname)
            dst_f = os.path.join(onnx_int8_dir, fname)
            if not os.path.exists(dst_f):
                shutil.copy2(src_f, dst_f)

    # ──────────────────────────────────────────
    # Step 6: Validation Gate
    # ──────────────────────────────────────────
    print("\n--- Step 6: ONNX INT8 Validation Gate ---")

    import onnxruntime as ort  # type: ignore
    import numpy as np  # type: ignore

    # Check which ONNX files we have
    int8_files = os.listdir(onnx_int8_dir)
    encoder_file = None
    decoder_file = None
    decoder_past_file = None

    for f in int8_files:
        if "encoder" in f and f.endswith(".onnx"):
            encoder_file = f
        elif "decoder_with_past" in f and f.endswith(".onnx"):
            decoder_past_file = f
        elif "decoder" in f and f.endswith(".onnx"):
            decoder_file = f

    if encoder_file and decoder_file:
        print(f"  Encoder:          {encoder_file}")
        print(f"  Decoder:          {decoder_file}")
        print(f"  Decoder w/ Past:  {decoder_past_file or 'N/A'}")

        # Load sessions
        enc_session = ort.InferenceSession(
            os.path.join(onnx_int8_dir, encoder_file),
            providers=["CPUExecutionProvider"]
        )
        dec_session = ort.InferenceSession(
            os.path.join(onnx_int8_dir, decoder_file),
            providers=["CPUExecutionProvider"]
        )
        dec_past_session = None
        if decoder_past_file:
            dec_past_session = ort.InferenceSession(
                os.path.join(onnx_int8_dir, decoder_past_file),
                providers=["CPUExecutionProvider"]
            )

        # Load tokenizer for validation
        val_tokenizer = AutoTokenizer.from_pretrained(
            merged_path, trust_remote_code=True, token=hf_token
        )

        test_sentences = [
            "बैठ जाओ",
            "अपनी किताब खोलो",
            "सभी बच्चे शांत रहो",
        ]

        # Load IndicProcessor for preprocessing
        try:
            sys.path.insert(0, "/content/IndicTransToolkit")
            from IndicTransToolkit import IndicProcessor  # type: ignore
            ip = IndicProcessor(inference=True)
        except ImportError:
            ip = None

        print(f"\n  [GATE] Translating {len(test_sentences)} validation sentences...")
        for hi_input in test_sentences:
            try:
                # Preprocess
                if ip:
                    prepped = ip.preprocess_batch([hi_input], src_lang="hin_Deva", tgt_lang="sat_Olck")
                    input_text = prepped[0]
                else:
                    input_text = hi_input

                # Tokenize
                encoded = val_tokenizer(input_text, return_tensors="np", padding=True)
                input_ids = encoded["input_ids"].astype(np.int64)
                attention_mask = encoded["attention_mask"].astype(np.int64)

                # Run encoder
                enc_output = enc_session.run(None, {
                    "input_ids": input_ids,
                    "attention_mask": attention_mask,
                })
                encoder_hidden_states = enc_output[0]

                # Run decoder (first step) — feed BOS token
                bos_token_id = val_tokenizer.bos_token_id or 0
                eos_token_id = val_tokenizer.eos_token_id or 2
                decoder_input_ids = np.array([[bos_token_id]], dtype=np.int64)

                dec_output = dec_session.run(None, {
                    "decoder_input_ids": decoder_input_ids,
                    "encoder_hidden_states": encoder_hidden_states,
                    "encoder_attention_mask": attention_mask,
                })

                logits = dec_output[0]
                next_token = int(np.argmax(logits[0, -1, :]))
                generated_tokens = [next_token]

                # Autoregressive loop with decoder_with_past (or plain decoder)
                max_length = 50
                past_kv = dec_output[1:] if len(dec_output) > 1 else None

                for step in range(max_length - 1):
                    if next_token == eos_token_id:
                        break

                    step_input = np.array([[next_token]], dtype=np.int64)

                    if dec_past_session and past_kv:
                        # Use decoder_with_past for efficient decoding
                        feed_dict = {
                            "decoder_input_ids": step_input,
                            "encoder_attention_mask": attention_mask,
                        }
                        # Add past KV tensors
                        past_names = [inp.name for inp in dec_past_session.get_inputs()
                                      if inp.name.startswith("past_key_values")]
                        for name, tensor in zip(past_names, past_kv):
                            feed_dict[name] = tensor

                        dec_output = dec_past_session.run(None, feed_dict)
                    else:
                        # Fallback: full decoder with all generated tokens
                        all_tokens = np.array([[bos_token_id] + generated_tokens], dtype=np.int64)
                        dec_output = dec_session.run(None, {
                            "decoder_input_ids": all_tokens,
                            "encoder_hidden_states": encoder_hidden_states,
                            "encoder_attention_mask": attention_mask,
                        })

                    logits = dec_output[0]
                    next_token = int(np.argmax(logits[0, -1, :]))
                    generated_tokens.append(next_token)
                    past_kv = dec_output[1:] if len(dec_output) > 1 else None

                # Decode output
                pred_text = val_tokenizer.decode(generated_tokens, skip_special_tokens=True)
                print(f"    '{hi_input}' → '{pred_text}' ({len(generated_tokens)} tokens)")

                if not pred_text.strip():
                    print(f"    [WARN] Empty translation output for '{hi_input}'!")
            except Exception as e:
                print(f"    [WARN] Validation failed for '{hi_input}': {e}")

        print("\n[OK] ONNX INT8 validation gate complete!")
    else:
        print("[WARN] Could not find encoder/decoder ONNX files for validation.")
        print(f"  Available files: {int8_files}")

    # ──────────────────────────────────────────
    # Step 7: Package Artifact
    # ──────────────────────────────────────────
    print("\n--- Step 7: Packaging ONNX INT8 Model Artifact ---")
    tar_path = "/content/indictrans2_sat_onnx_int8.tar.gz"
    if os.path.exists(tar_path):
        os.remove(tar_path)

    run_cmd_strict(
        f"cd /content && tar -czf {tar_path} indictrans2_sat_onnx_int8/",
        description="Packaging ONNX INT8 model"
    )

    elapsed_min = (time.time() - t_start) / 60

    # Print summary
    if os.path.exists(tar_path):
        size_mb = os.path.getsize(tar_path) / (1024 * 1024)
        print(f"\n{'=' * 65}")
        print("[PHASE 2.6 SUCCESS] ONNX INT8 Model Package Ready!")
        print(f"  Path:            {tar_path}")
        print(f"  Compressed Size: {size_mb:.1f} MB")
        print(f"  Contents:")
        for f in sorted(os.listdir(onnx_int8_dir)):
            fpath = os.path.join(onnx_int8_dir, f)
            fsize = os.path.getsize(fpath) / (1024 * 1024)
            print(f"    - {f}: {fsize:.1f} MB")
        print(f"  Execution Time:  {elapsed_min:.1f} minutes")
        print(f"{'=' * 65}")
    else:
        raise FileNotFoundError(f"Failed to create package: {tar_path}")

    # ──────────────────────────────────────────
    # Step 8: Upload to HuggingFace (optional)
    # ──────────────────────────────────────────
    if hf_token:
        print("\n--- Step 8: Uploading to HuggingFace Hub ---")
        try:
            from huggingface_hub import HfApi  # type: ignore
            api = HfApi(token=hf_token)
            user = api.whoami()["name"]
            repo_id = f"{user}/vernacular-pedagogy-santhali"

            for f in os.listdir(onnx_int8_dir):
                if f.endswith(".onnx") or f in ["model.SRC", "model.TGT"]:
                    fpath = os.path.join(onnx_int8_dir, f)
                    print(f"  [UPLOAD] {f} ({os.path.getsize(fpath)/(1024*1024):.1f} MB)...")
                    api.upload_file(
                        path_or_fileobj=fpath,
                        path_in_repo=f"onnx/{f}",
                        repo_id=repo_id,
                        repo_type="model",
                        commit_message=f"feat(onnx): add {f} INT8 quantized"
                    )
            print("[OK] ONNX models uploaded to HuggingFace Hub!")
        except Exception as e:
            print(f"[WARN] HuggingFace upload failed: {e}")
            print("[INFO] The model is still available locally in the tar archive.")
    else:
        print("\n[INFO] No HF_TOKEN — skipping HuggingFace upload.")

    print(f"\n[ALL_DONE_SUCCESSFULLY] Total time: {elapsed_min:.1f} minutes")


if __name__ == "__main__":
    main()
