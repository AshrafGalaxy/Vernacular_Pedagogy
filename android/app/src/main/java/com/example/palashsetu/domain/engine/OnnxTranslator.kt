package com.example.palashsetu.domain.engine

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.JsonReader
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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
 *   2. decoder_model.onnx — Generates target Ol Chiki tokens
 *   3. decoder_with_past_model.onnx — Iterative autoregressive steps with KV cache (optional)
 *
 * Tokenization is handled via streaming vocabulary dictionaries (dict.SRC.json,
 * dict.TGT.json) loaded directly from assets without memory bloat.
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
    private var padTokenId: Int = 1
    private var bosTokenId: Int = 0
    private var eosTokenId: Int = 2
    private var decoderStartTokenId: Int = 2 // decoder_start_token_id in IndicTrans2
    private var srcLangTagId: Int = 8        // hin_Deva
    private var tgtLangTagId: Int = 29925    // sat_Olck

    var isInitialized: Boolean = false
        private set

    // Model directory within assets
    private val assetDir = "models/mt"

    /**
     * Initializes all ONNX sessions and loads vocabulary dictionaries.
     * Must be called once before any translation calls.
     * Returns true if initialization succeeds.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized && encoderSession != null) return@withContext true

        try {
            val env = OrtEnvironment.getEnvironment()
            ortEnv = env

            // 1. Load vocabulary dictionaries using streaming parser (zero memory bloat)
            loadVocabularies()

            // 2. Copy ONNX models to internal storage for optimal execution
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
    suspend fun translate(hindiText: String, maxLength: Int = 40): TranslationOutput? =
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
                // Step 1: Tokenize input with IndicTrans2 format: [hin_Deva, sat_Olck, ...tokens, </s>]
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
                val runningDecoderTokens = mutableListOf<Long>(decoderStartTokenId.toLong())

                // First decoder step
                val firstDecoderInput = OnnxTensor.createTensor(
                    env,
                    LongBuffer.wrap(longArrayOf(decoderStartTokenId.toLong())),
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

                @Suppress("UNCHECKED_CAST")
                val firstLogits = firstDecResults[0].value as Array<Array<FloatArray>>
                var currentTokenId = argmax(firstLogits[0][firstLogits[0].size - 1])

                if (currentTokenId != eosTokenId && currentTokenId != padTokenId) {
                    generatedTokens.add(currentTokenId)
                    runningDecoderTokens.add(currentTokenId.toLong())
                }

                // Iterative decoding loop
                for (step in 1 until maxLength) {
                    if (currentTokenId == eosTokenId) break

                    val stepTokens = runningDecoderTokens.toLongArray()
                    val stepInput = OnnxTensor.createTensor(
                        env,
                        LongBuffer.wrap(stepTokens),
                        longArrayOf(1, stepTokens.size.toLong())
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

                    stepInput.close()
                    stepResults.close()

                    if (currentTokenId == eosTokenId) break
                    if (currentTokenId != padTokenId) {
                        generatedTokens.add(currentTokenId)
                        runningDecoderTokens.add(currentTokenId.toLong())
                    }
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
     * Prepends the language direction tags (hin_Deva, sat_Olck) as required by IndicTrans2.
     */
    private fun tokenizeSource(text: String): List<Int> {
        val tokens = mutableListOf<Int>()

        // Add IndicTrans2 language direction tags
        if (srcLangTagId >= 0) tokens.add(srcLangTagId)
        if (tgtLangTagId >= 0) tokens.add(tgtLangTagId)

        val normalized = text.trim()
        val words = normalized.split(Regex("\\s+"))

        for (word in words) {
            val fullWordKey = "\u2581$word" // SentencePiece prefix
            if (srcVocab.containsKey(fullWordKey)) {
                tokens.add(srcVocab[fullWordKey]!!)
            } else {
                var remaining = word
                var pos = 0
                while (remaining.isNotEmpty()) {
                    var matched = false
                    for (len in minOf(remaining.length, 12) downTo 1) {
                        val sub = remaining.substring(0, len)
                        val key = if (pos == 0) "\u2581$sub" else sub
                        if (srcVocab.containsKey(key)) {
                            tokens.add(srcVocab[key]!!)
                            remaining = remaining.substring(len)
                            pos += len
                            matched = true
                            break
                        }
                    }
                    if (!matched) {
                        val charKey = remaining.substring(0, 1)
                        val charId = srcVocab[charKey] ?: srcVocab["<unk>"] ?: 3
                        tokens.add(charId)
                        remaining = remaining.substring(1)
                        pos++
                    }
                }
            }
        }

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
            if (id == padTokenId || id == bosTokenId || id == eosTokenId || id == decoderStartTokenId) continue
            val token = tgtVocabReverse[id] ?: continue
            // Skip language tags and special tokens
            if (token.startsWith("__") && token.endsWith("__")) continue
            if (token == "<s>" || token == "</s>" || token == "<unk>" || token == "<pad>") continue

            val cleaned = token.replace("\u2581", " ")
            sb.append(cleaned)
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Loads source and target vocabulary dictionaries from assets using streaming JsonReader.
     */
    private fun loadVocabularies() {
        // Load source vocabulary (dict.SRC.json)
        try {
            context.assets.open("$assetDir/dict.SRC.json").use { stream ->
                srcVocab = parseVocabStream(stream)
            }
            Log.i(tag, "Source vocabulary loaded: ${srcVocab.size} tokens")
        } catch (e: Exception) {
            Log.e(tag, "Failed to load source vocabulary: ${e.message}")
        }

        // Load target vocabulary (dict.TGT.json)
        try {
            context.assets.open("$assetDir/dict.TGT.json").use { stream ->
                tgtVocabReverse = parseVocabReverseStream(stream)
            }
            Log.i(tag, "Target vocabulary loaded: ${tgtVocabReverse.size} tokens")
        } catch (e: Exception) {
            Log.e(tag, "Failed to load target vocabulary: ${e.message}")
        }

        // Resolve special token IDs
        padTokenId = srcVocab["<pad>"] ?: 1
        bosTokenId = srcVocab["<s>"] ?: 0
        eosTokenId = srcVocab["</s>"] ?: 2
        decoderStartTokenId = 2 // IndicTrans2 decoder starts with </s> (id 2)
        srcLangTagId = srcVocab["hin_Deva"] ?: srcVocab["__hin_Deva__"] ?: 8
        tgtLangTagId = srcVocab["sat_Olck"] ?: srcVocab["__sat_Olck__"] ?: 29925

        Log.i(tag, "Special tokens: pad=$padTokenId, bos=$bosTokenId, eos=$eosTokenId, " +
            "decStart=$decoderStartTokenId, srcLang=$srcLangTagId, tgtLang=$tgtLangTagId")
    }

    /**
     * Streaming JSON reader for {"token": id, ...} maps.
     * Prevents large heap allocations and OOM on 5MB+ vocabularies.
     */
    private fun parseVocabStream(stream: InputStream): Map<String, Int> {
        val map = HashMap<String, Int>(130000)
        val reader = JsonReader(InputStreamReader(stream, Charsets.UTF_8))
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            val value = reader.nextInt()
            map[key] = value
        }
        reader.endObject()
        reader.close()
        return map
    }

    /**
     * Streaming JSON reader for reverse {id: "token"} mapping directly from {"token": id, ...}.
     */
    private fun parseVocabReverseStream(stream: InputStream): Map<Int, String> {
        val map = HashMap<Int, String>(130000)
        val reader = JsonReader(InputStreamReader(stream, Charsets.UTF_8))
        reader.beginObject()
        while (reader.hasNext()) {
            val token = reader.nextName()
            val id = reader.nextInt()
            map[id] = token
        }
        reader.endObject()
        reader.close()
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
     * Copies an asset file to internal app storage.
     */
    private fun copyAssetToInternal(assetPath: String, targetDir: File): File {
        val fileName = File(assetPath).name
        val targetFile = File(targetDir, fileName)

        val assetLength = try {
            context.assets.openFd(assetPath).length
        } catch (e: Exception) {
            -1L
        }

        if (targetFile.exists() && (assetLength <= 0 || targetFile.length() == assetLength)) {
            return targetFile
        }

        Log.i(tag, "Copying asset $assetPath to ${targetFile.absolutePath}...")
        context.assets.open(assetPath).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        Log.i(tag, "Copied $assetPath (${targetFile.length()} bytes)")
        return targetFile
    }

    /**
     * Releases ONNX sessions and frees native memory.
     */
    fun release() {
        try {
            encoderSession?.close()
            decoderSession?.close()
            decoderWithPastSession?.close()
            ortEnv?.close()
        } catch (e: Exception) {
            Log.w(tag, "Error closing ONNX sessions: ${e.message}")
        }
        encoderSession = null
        decoderSession = null
        decoderWithPastSession = null
        ortEnv = null
        isInitialized = false
        Log.i(tag, "ONNX NMT engine released")
    }
}

/**
 * Output data class containing translation result and metadata.
 */
data class TranslationOutput(
    val targetText: String,
    val latencyMs: Long,
    val tokenCount: Int
)
