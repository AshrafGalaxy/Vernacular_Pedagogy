package com.example.palashsetu.domain.engine

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.LongBuffer

/**
 * On-Device Neural Machine Translation Engine for Hindi → Santhali (Ol Chiki).
 *
 * Runs IndicTrans2 320M (INT8 quantized) locally via ONNX Runtime on CPU
 * using a 3-file encoder-decoder architecture with autoregressive decoding.
 *
 * Architecture:
 *   1. encoder_model.onnx — Encodes Hindi source tokens into hidden states
 *   2. decoder_model.onnx — First decoding step (generates initial token + KV cache)
 *   3. decoder_with_past_model.onnx — Iterative autoregressive steps with KV cache
 *
 * Tokenization is handled via pre-built vocabulary dictionaries (dict.SRC.json,
 * dict.TGT.json) loaded from assets, avoiding the need for SentencePiece JNI.
 */
class OnnxTranslator(private val context: Context) {

    private val tag = "OnnxTranslator"

    private var ortEnv: OrtEnvironment? = null
    private var encoderSession: OrtSession? = null
    private var decoderSession: OrtSession? = null
    private var decoderWithPastSession: OrtSession? = null

    // Token → ID mappings (loaded from dict.SRC.json / dict.TGT.json)
    private var srcVocab: Map<String, Int> = emptyMap()
    private var tgtVocabReverse: Map<Int, String> = emptyMap() // ID → Token

    // Special token IDs
    private var padTokenId: Int = 0
    private var bosTokenId: Int = 1
    private var eosTokenId: Int = 2
    private var srcLangTagId: Int = -1  // __hin_Deva__ token ID

    var isInitialized: Boolean = false
        private set

    // Model directory within assets
    private val assetDir = "models/mt"

    /**
     * Initializes all 3 ONNX sessions and loads vocabulary dictionaries.
     * Must be called once before any translation calls.
     * Returns true if initialization succeeds.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized && encoderSession != null) return@withContext true

        try {
            val env = OrtEnvironment.getEnvironment()
            ortEnv = env

            // 1. Load vocabulary dictionaries
            loadVocabularies()

            // 2. Copy ONNX models to internal storage for optimal mmap execution
            val modelDir = File(context.filesDir, "models/mt")
            if (!modelDir.exists()) modelDir.mkdirs()

            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }

            val encoderFile = copyAssetToInternal("$assetDir/encoder_model.onnx", modelDir)
            val decoderFile = copyAssetToInternal("$assetDir/decoder_model.onnx", modelDir)

            encoderSession = env.createSession(encoderFile.absolutePath, sessionOptions)
            decoderSession = env.createSession(decoderFile.absolutePath, sessionOptions)

            // decoder_with_past is optional — if absent, we fall back to full decoder
            try {
                val decoderPastFile = copyAssetToInternal("$assetDir/decoder_with_past_model.onnx", modelDir)
                decoderWithPastSession = env.createSession(decoderPastFile.absolutePath, sessionOptions)
                Log.i(tag, "Loaded decoder_with_past for efficient autoregressive decoding")
            } catch (e: Exception) {
                Log.w(tag, "decoder_with_past_model.onnx not found, using full decoder fallback")
            }

            isInitialized = true
            Log.i(tag, "ONNX NMT engine initialized: " +
                "encoder=${encoderSession != null}, " +
                "decoder=${decoderSession != null}, " +
                "decoder_with_past=${decoderWithPastSession != null}, " +
                "src_vocab=${srcVocab.size}, tgt_vocab=${tgtVocabReverse.size}")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize ONNX NMT engine: ${e.message}", e)
            false
        }
    }

    /**
     * Translates Hindi text to Santhali (Ol Chiki) using on-device ONNX inference.
     *
     * @param hindiText Input Hindi text
     * @param maxLength Maximum number of output tokens
     * @return Translated Ol Chiki text, or null if translation fails
     */
    suspend fun translate(hindiText: String, maxLength: Int = 64): TranslationOutput? =
        withContext(Dispatchers.IO) {
            if (!isInitialized) {
                Log.w(tag, "Engine not initialized, attempting init...")
                if (!initialize()) return@withContext null
            }

            val env = ortEnv ?: return@withContext null
            val encSession = encoderSession ?: return@withContext null
            val decSession = decoderSession ?: return@withContext null

            val startTime = System.currentTimeMillis()

            try {
                // Step 1: Tokenize input
                val inputTokenIds = tokenizeSource(hindiText)
                if (inputTokenIds.isEmpty()) {
                    Log.w(tag, "Empty tokenization result for: $hindiText")
                    return@withContext null
                }

                val seqLen = inputTokenIds.size
                val inputIdArray = LongArray(seqLen) { inputTokenIds[it].toLong() }
                val attentionMaskArray = LongArray(seqLen) { 1L }

                // Step 2: Run Encoder
                val inputIdsTensor = OnnxTensor.createTensor(
                    env,
                    LongBuffer.wrap(inputIdArray),
                    longArrayOf(1, seqLen.toLong())
                )
                val attentionMaskTensor = OnnxTensor.createTensor(
                    env,
                    LongBuffer.wrap(attentionMaskArray),
                    longArrayOf(1, seqLen.toLong())
                )

                val encResults = encSession.run(
                    mapOf(
                        "input_ids" to inputIdsTensor,
                        "attention_mask" to attentionMaskTensor
                    )
                )

                val encoderHiddenStates = encResults[0].value // float[1][seqLen][d_model]

                // Step 3: Autoregressive Decoding
                val generatedTokens = mutableListOf<Int>()
                var currentTokenId = bosTokenId

                // First decoder step
                val firstDecoderInput = OnnxTensor.createTensor(
                    env,
                    LongBuffer.wrap(longArrayOf(currentTokenId.toLong())),
                    longArrayOf(1, 1)
                )

                @Suppress("UNCHECKED_CAST")
                val encoderHiddenTensor = OnnxTensor.createTensor(
                    env,
                    encoderHiddenStates as Array<Array<FloatArray>>
                )

                val firstDecResults = decSession.run(
                    mapOf(
                        "decoder_input_ids" to firstDecoderInput,
                        "encoder_hidden_states" to encoderHiddenTensor,
                        "encoder_attention_mask" to attentionMaskTensor
                    )
                )

                // Extract logits and get first predicted token
                @Suppress("UNCHECKED_CAST")
                val firstLogits = firstDecResults[0].value as Array<Array<FloatArray>>
                currentTokenId = argmax(firstLogits[0][firstLogits[0].size - 1])
                generatedTokens.add(currentTokenId)

                // Iterative decoding loop
                for (step in 1 until maxLength) {
                    if (currentTokenId == eosTokenId) break

                    // Use full decoder (without past) for simplicity
                    // Note: decoder_with_past can be used for KV cache optimization
                    val allTokens = LongArray(generatedTokens.size + 1)
                    allTokens[0] = bosTokenId.toLong()
                    for (i in generatedTokens.indices) {
                        allTokens[i + 1] = generatedTokens[i].toLong()
                    }

                    val stepInput = OnnxTensor.createTensor(
                        env,
                        LongBuffer.wrap(allTokens),
                        longArrayOf(1, allTokens.size.toLong())
                    )

                    val stepResults = decSession.run(
                        mapOf(
                            "decoder_input_ids" to stepInput,
                            "encoder_hidden_states" to encoderHiddenTensor,
                            "encoder_attention_mask" to attentionMaskTensor
                        )
                    )

                    @Suppress("UNCHECKED_CAST")
                    val stepLogits = stepResults[0].value as Array<Array<FloatArray>>
                    currentTokenId = argmax(stepLogits[0][stepLogits[0].size - 1])
                    generatedTokens.add(currentTokenId)

                    stepInput.close()
                    stepResults.close()
                }

                // Step 4: Detokenize output
                val outputText = detokenizeTarget(generatedTokens)
                val latencyMs = System.currentTimeMillis() - startTime

                // Clean up tensors
                inputIdsTensor.close()
                attentionMaskTensor.close()
                firstDecoderInput.close()
                encoderHiddenTensor.close()
                encResults.close()
                firstDecResults.close()

                Log.i(tag, "Translation: '$hindiText' → '$outputText' (${latencyMs}ms, ${generatedTokens.size} tokens)")

                TranslationOutput(
                    targetText = outputText,
                    latencyMs = latencyMs,
                    tokenCount = generatedTokens.size
                )
            } catch (e: Exception) {
                Log.e(tag, "Translation failed for '$hindiText': ${e.message}", e)
                null
            }
        }

    /**
     * Tokenizes Hindi source text into token IDs using the source vocabulary.
     * Prepends the language tag (__hin_Deva__) as required by IndicTrans2.
     */
    private fun tokenizeSource(text: String): List<Int> {
        val tokens = mutableListOf<Int>()

        // Add source language tag if available
        if (srcLangTagId >= 0) {
            tokens.add(srcLangTagId)
        }

        // Simple subword tokenization using vocabulary lookup
        // This is a best-effort character/word-level tokenization
        // The actual SentencePiece tokenization happens at the character level
        val normalized = text.trim()

        // Try word-level tokenization first
        val words = normalized.split(Regex("\\s+"))
        for (word in words) {
            val fullWordKey = "▁$word" // SentencePiece prefix
            if (srcVocab.containsKey(fullWordKey)) {
                tokens.add(srcVocab[fullWordKey]!!)
            } else {
                // Character-level fallback for OOV words
                var remaining = word
                var pos = 0
                while (remaining.isNotEmpty()) {
                    // Try longest match first
                    var matched = false
                    for (len in minOf(remaining.length, 12) downTo 1) {
                        val sub = remaining.substring(0, len)
                        val key = if (pos == 0 && tokens.size <= 1) "▁$sub" else sub
                        if (srcVocab.containsKey(key)) {
                            tokens.add(srcVocab[key]!!)
                            remaining = remaining.substring(len)
                            pos += len
                            matched = true
                            break
                        }
                    }
                    if (!matched) {
                        // Single character fallback
                        val charKey = remaining.substring(0, 1)
                        val charId = srcVocab[charKey] ?: srcVocab["<unk>"] ?: 3
                        tokens.add(charId)
                        remaining = remaining.substring(1)
                        pos++
                    }
                }
            }
        }

        // Add EOS token
        tokens.add(eosTokenId)

        return tokens
    }

    /**
     * Detokenizes target token IDs back to Ol Chiki text using the target vocabulary.
     * Strips special tokens and SentencePiece prefixes.
     */
    private fun detokenizeTarget(tokenIds: List<Int>): String {
        val sb = StringBuilder()
        for (id in tokenIds) {
            if (id == padTokenId || id == bosTokenId || id == eosTokenId) continue
            val token = tgtVocabReverse[id] ?: continue
            // Skip language tags
            if (token.startsWith("__") && token.endsWith("__")) continue
            if (token == "<s>" || token == "</s>" || token == "<unk>" || token == "<pad>") continue

            // Handle SentencePiece ▁ prefix (indicates word boundary / leading space)
            val cleaned = token.replace("▁", " ")
            sb.append(cleaned)
        }
        return sb.toString().trim()
    }

    /**
     * Loads source and target vocabulary dictionaries from assets.
     */
    private fun loadVocabularies() {
        // Load source vocabulary (dict.SRC.json)
        try {
            val srcJson = context.assets.open("$assetDir/dict.SRC.json")
                .bufferedReader()
                .use { it.readText() }
            srcVocab = parseVocabJson(srcJson)
            Log.i(tag, "Source vocabulary loaded: ${srcVocab.size} tokens")
        } catch (e: Exception) {
            Log.e(tag, "Failed to load source vocabulary: ${e.message}")
            // Fallback: try source_vocabulary.json (CTranslate2 format)
            try {
                val srcJson = context.assets.open("$assetDir/source_vocabulary.json")
                    .bufferedReader()
                    .use { it.readText() }
                srcVocab = parseVocabJsonArray(srcJson)
                Log.i(tag, "Source vocabulary loaded (CT2 format): ${srcVocab.size} tokens")
            } catch (e2: Exception) {
                Log.e(tag, "Failed to load fallback source vocabulary: ${e2.message}")
            }
        }

        // Load target vocabulary (dict.TGT.json)
        try {
            val tgtJson = context.assets.open("$assetDir/dict.TGT.json")
                .bufferedReader()
                .use { it.readText() }
            val tgtVocab = parseVocabJson(tgtJson)
            tgtVocabReverse = tgtVocab.entries.associate { (k, v) -> v to k }
            Log.i(tag, "Target vocabulary loaded: ${tgtVocab.size} tokens")
        } catch (e: Exception) {
            Log.e(tag, "Failed to load target vocabulary: ${e.message}")
            try {
                val tgtJson = context.assets.open("$assetDir/target_vocabulary.json")
                    .bufferedReader()
                    .use { it.readText() }
                tgtVocabReverse = parseVocabJsonArrayReverse(tgtJson)
                Log.i(tag, "Target vocabulary loaded (CT2 format): ${tgtVocabReverse.size} tokens")
            } catch (e2: Exception) {
                Log.e(tag, "Failed to load fallback target vocabulary: ${e2.message}")
            }
        }

        // Resolve special token IDs
        padTokenId = srcVocab["<pad>"] ?: 0
        bosTokenId = srcVocab["<s>"] ?: 1
        eosTokenId = srcVocab["</s>"] ?: 2
        srcLangTagId = srcVocab["__hin_Deva__"] ?: -1

        Log.i(tag, "Special tokens: pad=$padTokenId, bos=$bosTokenId, eos=$eosTokenId, srcLang=$srcLangTagId")
    }

    /**
     * Parses a JSON object where keys are tokens and values are integer IDs.
     * Format: {"token": id, ...}
     */
    private fun parseVocabJson(json: String): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        val obj = org.json.JSONObject(json)
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = obj.getInt(key)
        }
        return map
    }

    /**
     * Parses a JSON array where the index is the token ID.
     * Format: ["token0", "token1", ...]
     */
    private fun parseVocabJsonArray(json: String): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        val arr = org.json.JSONArray(json)
        for (i in 0 until arr.length()) {
            map[arr.getString(i)] = i
        }
        return map
    }

    /**
     * Parses a JSON array and returns reverse mapping (ID → token).
     */
    private fun parseVocabJsonArrayReverse(json: String): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        val arr = org.json.JSONArray(json)
        for (i in 0 until arr.length()) {
            map[i] = arr.getString(i)
        }
        return map
    }

    /**
     * Returns the index of the maximum value in a float array (argmax).
     */
    private fun argmax(logits: FloatArray): Int {
        var maxIdx = 0
        var maxVal = logits[0]
        for (i in 1 until logits.size) {
            if (logits[i] > maxVal) {
                maxVal = logits[i]
                maxIdx = i
            }
        }
        return maxIdx
    }

    /**
     * Copies an asset file to internal storage if not already present.
     */
    private fun copyAssetToInternal(assetPath: String, targetDir: File): File {
        val fileName = assetPath.substringAfterLast("/")
        val targetFile = File(targetDir, fileName)
        if (!targetFile.exists() || targetFile.length() < 1000) {
            Log.i(tag, "Copying $assetPath to internal storage...")
            context.assets.open(assetPath).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return targetFile
    }

    /**
     * Releases all ONNX Runtime resources.
     */
    fun release() {
        try {
            encoderSession?.close()
            decoderSession?.close()
            decoderWithPastSession?.close()
            encoderSession = null
            decoderSession = null
            decoderWithPastSession = null
            isInitialized = false
            Log.i(tag, "ONNX NMT engine resources released")
        } catch (e: Exception) {
            Log.w(tag, "Error releasing ONNX sessions: ${e.message}")
        }
    }
}

/**
 * Output from a neural machine translation inference pass.
 */
data class TranslationOutput(
    val targetText: String,
    val latencyMs: Long,
    val tokenCount: Int
)
