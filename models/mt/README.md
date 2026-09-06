# Machine Translation Models

## IndicTrans2 Santhali (Ol Chiki) INT8 CTranslate2 Model

- **Base Model**: `ai4bharat/indictrans2-indic-indic-dist-320M`
- **Fine-Tuning**: LoRA fine-tuned on Grade 1–3 pedagogical bitext + AI4Bharat BPCC & IN22 Santhali benchmarks (`hin_Deva` $\rightarrow$ `sat_Olck`)
- **Format**: CTranslate2 INT8 Quantized (`model.bin`)
- **Dual Asymmetric Vocabularies**:
  - Source (`source_vocabulary.json`): 122,706 tokens
  - Target (`target_vocabulary.json`): 122,672 tokens
- **Archive**: `indictrans2_sat_int8_ct2.tar.gz` (~286.7 MB compressed, ~334 MB uncompressed)
- **Target Deployment**: Raspberry Pi 4 / Mobile Edge (CPU inference <250ms per token)

### Archive Contents
```
indictrans2_sat_int8_ct2/
├── config.json                 # CTranslate2 model configuration
├── model.bin                   # INT8 quantized weights (~325 MB)
├── source_vocabulary.json      # Source SPM vocabulary (122,706 tokens)
└── target_vocabulary.json      # Target SPM vocabulary (122,672 tokens)
```

### Usage with CTranslate2
```python
import ctranslate2

translator = ctranslate2.Translator("models/mt/indictrans2_sat_int8_ct2", device="cpu", compute_type="int8")
# Tokenized source tokens prefixed with language tags
# tokens = ["__hin_Deva__", "नमस्ते", ...]
# results = translator.translate_batch([tokens])
```
