# -*- coding: utf-8 -*-
"""
Phase 2.5: IndicTrans2 Vocabulary & Embedding Pruning Engine
Dedicated to Hindi (hin_Deva) <-> Santhali (sat_Olck) Edge Deployment.

Prunes ~116,000 unused target language tokens and ~46,000 unused source language
tokens from IndicTrans2 320M, slicing embedding tables and output projection heads
down to strictly active Hindi and Santhali subwords.
"""

import os
import sys
import json
import shutil
import torch
from typing import List, Dict, Tuple, Set

# Fix Windows console encoding
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")


def is_kept_source_token(token: str) -> bool:
    """
    Identifies if a source token belongs to:
    1. Special tokens (<s>, </s>, <unk>, <pad>, <mask>, __hin_Deva__, __sat_Olck__)
    2. Devanagari script (U+0900 - U+097F, U+A8E0 - U+A8FF)
    3. Punctuation, symbols, numbers, and basic ASCII whitespace
    """
    if token.startswith("__") and token.endswith("__"):
        return True
    if token in ["<s>", "</s>", "<unk>", "<pad>", "<mask>"]:
        return True
    clean = token.replace("\u2581", "").strip()
    if not clean:
        return True  # Whitespace subword token
    # Check if contains Devanagari characters
    if any('\u0900' <= c <= '\u097F' or '\uA8E0' <= c <= '\uA8FF' for c in clean):
        return True
    # Check if numbers, punctuation, or basic Latin ASCII
    if all(ord(c) < 128 or c in "‘’“”–—…॥।«»₹" for c in clean):
        return True
    return False


def is_kept_target_token(token: str) -> bool:
    """
    Identifies if a target token belongs to:
    1. Special tokens and language tags
    2. Santhali Ol Chiki script (U+1C50 - U+1C7F)
    3. Punctuation, symbols, numbers, and basic ASCII whitespace
    """
    if token.startswith("__") and token.endswith("__"):
        return True
    if token in ["<s>", "</s>", "<unk>", "<pad>", "<mask>"]:
        return True
    clean = token.replace("\u2581", "").strip()
    if not clean:
        return True
    # Check if contains Ol Chiki characters
    if any('\u1C50' <= c <= '\u1C7F' for c in clean):
        return True
    # Check if numbers, punctuation, or basic Latin ASCII
    if all(ord(c) < 128 or c in "‘’“”–—…॥।«»₹" for c in clean):
        return True
    return False


def extract_vocab_list_from_dict(dict_path: str) -> List[str]:
    """Reads IndicTrans dict.SRC.json or dict.TGT.json and returns tokens ordered by index."""
    with open(dict_path, "r", encoding="utf-8") as f:
        mapping = json.load(f)
    vocab = [None] * len(mapping)
    for token, idx in mapping.items():
        if idx < len(vocab):
            vocab[idx] = token
    return [t or "<unk>" for t in vocab]


def prune_indictrans_model(
    model_dir: str,
    output_dir: str,
    device: str = "cpu"
) -> Tuple[int, int, int, int]:
    """
    Loads an IndicTrans2 PyTorch model from model_dir, slices embedding tables
    and projection heads to retain only Hindi and Santhali tokens, and saves
    the pruned model to output_dir.

    Returns:
      (orig_src_len, pruned_src_len, orig_tgt_len, pruned_tgt_len)
    """
    from transformers import AutoModelForSeq2SeqLM, AutoTokenizer, AutoConfig

    print(f"\n[PRUNING] Loading model and tokenizer from: {model_dir}")
    config = AutoConfig.from_pretrained(model_dir, trust_remote_code=True)
    model = AutoModelForSeq2SeqLM.from_pretrained(
        model_dir,
        trust_remote_code=True,
        low_cpu_mem_usage=True,
        torch_dtype=torch.float32
    ).to(device)

    tokenizer = AutoTokenizer.from_pretrained(model_dir, trust_remote_code=True)

    # 1. Extract and filter Source Vocabulary
    src_dict_path = os.path.join(model_dir, "dict.SRC.json")
    if os.path.exists(src_dict_path):
        orig_src_vocab = extract_vocab_list_from_dict(src_dict_path)
    elif hasattr(tokenizer, "src_encoder"):
        orig_src_vocab = [None] * len(tokenizer.src_encoder)
        for t, idx in tokenizer.src_encoder.items():
            if idx < len(orig_src_vocab):
                orig_src_vocab[idx] = t
        orig_src_vocab = [t or "<unk>" for t in orig_src_vocab]
    else:
        raise FileNotFoundError(f"Cannot find source vocabulary in {model_dir}")

    # 2. Extract and filter Target Vocabulary
    tgt_dict_path = os.path.join(model_dir, "dict.TGT.json")
    if os.path.exists(tgt_dict_path):
        orig_tgt_vocab = extract_vocab_list_from_dict(tgt_dict_path)
    elif hasattr(tokenizer, "tgt_encoder"):
        orig_tgt_vocab = [None] * len(tokenizer.tgt_encoder)
        for t, idx in tokenizer.tgt_encoder.items():
            if idx < len(orig_tgt_vocab):
                orig_tgt_vocab[idx] = t
        orig_tgt_vocab = [t or "<unk>" for t in orig_tgt_vocab]
    else:
        raise FileNotFoundError(f"Cannot find target vocabulary in {model_dir}")

    orig_src_len = len(orig_src_vocab)
    orig_tgt_len = len(orig_tgt_vocab)

    # 3. Determine indices to keep
    # Ensure critical special tokens (<pad>, <s>, </s>, <unk>) retain IDs 0, 1, 2, 3 if possible
    kept_src_indices = [i for i, t in enumerate(orig_src_vocab) if is_kept_source_token(t)]
    kept_tgt_indices = [i for i, t in enumerate(orig_tgt_vocab) if is_kept_target_token(t)]

    pruned_src_vocab = [orig_src_vocab[i] for i in kept_src_indices]
    pruned_tgt_vocab = [orig_tgt_vocab[i] for i in kept_tgt_indices]

    pruned_src_len = len(pruned_src_vocab)
    pruned_tgt_len = len(pruned_tgt_vocab)

    print(f"  Source Vocabulary: {orig_src_len:,} -> {pruned_src_len:,} tokens (reduced by {(1 - pruned_src_len/orig_src_len)*100:.1f}%)")
    print(f"  Target Vocabulary: {orig_tgt_len:,} -> {pruned_tgt_len:,} tokens (reduced by {(1 - pruned_tgt_len/orig_tgt_len)*100:.1f}%)")

    # 4. Slice Embedding Tables
    print("[PRUNING] Slicing PyTorch weight tensors...")
    with torch.no_grad():
        src_idx_tensor = torch.tensor(kept_src_indices, dtype=torch.long, device=device)
        tgt_idx_tensor = torch.tensor(kept_tgt_indices, dtype=torch.long, device=device)

        # Slice Encoder Embedding
        old_enc_embed = model.model.encoder.embed_tokens.weight.data
        new_enc_embed = old_enc_embed[src_idx_tensor].clone()
        new_enc_embed_layer = torch.nn.Embedding(pruned_src_len, config.d_model, padding_idx=config.pad_token_id)
        new_enc_embed_layer.weight.data = new_enc_embed
        model.model.encoder.embed_tokens = new_enc_embed_layer

        # Slice Decoder Embedding
        old_dec_embed = model.model.decoder.embed_tokens.weight.data
        new_dec_embed = old_dec_embed[tgt_idx_tensor].clone()
        new_dec_embed_layer = torch.nn.Embedding(pruned_tgt_len, config.d_model, padding_idx=config.pad_token_id)
        new_dec_embed_layer.weight.data = new_dec_embed
        model.model.decoder.embed_tokens = new_dec_embed_layer

        # Slice Output Projection (lm_head)
        old_lm_head = model.lm_head.weight.data
        new_lm_head = old_lm_head[tgt_idx_tensor].clone()
        new_lm_head_layer = torch.nn.Linear(config.d_model, pruned_tgt_len, bias=False)
        new_lm_head_layer.weight.data = new_lm_head
        model.lm_head = new_lm_head_layer

        # Update Config parameters
        config.vocab_size = pruned_tgt_len
        if hasattr(config, "src_vocab_size"):
            config.src_vocab_size = pruned_src_len
        if hasattr(config, "tgt_vocab_size"):
            config.tgt_vocab_size = pruned_tgt_len

        model.config = config

    # 5. Save Pruned Model & Dictionaries
    os.makedirs(output_dir, exist_ok=True)
    print(f"[PRUNING] Saving pruned model to: {output_dir}")

    # Standardize tied weights to dictionary for transformers v5.x compatibility
    model._tied_weights_keys = {"lm_head.weight": "model.decoder.embed_tokens.weight"}
    for mod in model.modules():
        tied = getattr(mod, "_tied_weights_keys", None)
        if isinstance(tied, (list, tuple, set)):
            mod._tied_weights_keys = {"lm_head.weight": "model.decoder.embed_tokens.weight"}

    model.save_pretrained(output_dir)

    # Build new dense mapping dictionaries
    new_src_mapping = {token: new_idx for new_idx, token in enumerate(pruned_src_vocab)}
    new_tgt_mapping = {token: new_idx for new_idx, token in enumerate(pruned_tgt_vocab)}

    with open(os.path.join(output_dir, "dict.SRC.json"), "w", encoding="utf-8") as f:
        json.dump(new_src_mapping, f, ensure_ascii=False, indent=2)

    with open(os.path.join(output_dir, "dict.TGT.json"), "w", encoding="utf-8") as f:
        json.dump(new_tgt_mapping, f, ensure_ascii=False, indent=2)

    # Copy SPM model binaries and tokenizer configs
    for fname in ["model.SRC", "model.TGT", "tokenizer_config.json", "special_tokens_map.json", "generation_config.json"]:
        src_f = os.path.join(model_dir, fname)
        if os.path.exists(src_f):
            shutil.copy2(src_f, os.path.join(output_dir, fname))

    # Copy remote code files if present
    for fname in os.listdir(model_dir):
        if fname.endswith(".py") or fname.endswith(".json"):
            dst_f = os.path.join(output_dir, fname)
            if not os.path.exists(dst_f):
                shutil.copy2(os.path.join(model_dir, fname), dst_f)

    print("[OK] Pruning complete!")
    return orig_src_len, pruned_src_len, orig_tgt_len, pruned_tgt_len


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Prune IndicTrans2 model vocabulary and embeddings")
    parser.add_argument("--model-dir", type=str, required=True, help="Input merged model directory")
    parser.add_argument("--output-dir", type=str, required=True, help="Output pruned model directory")
    args = parser.parse_args()

    prune_indictrans_model(args.model_dir, args.output_dir)
