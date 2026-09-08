package com.example.palashsetu.domain.engine

import android.content.Context
import android.util.Log
import com.example.palashsetu.data.local.FlnRepository
import com.example.palashsetu.data.model.TranslationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Multi-Tier Neural Machine Translation Engine for Hindi → Santhali.
 *
 * Architecture:
 *   Tier 1: SQLite B-Tree Fast-Path (FlnRepository) — <0.1ms exact match lookup
 *   Tier 2: On-Device ONNX Neural Inference (OnnxTranslator) — IndicTrans2 INT8
 *
 * Usage:
 *   1. Call initialize(context) once during app startup
 *   2. Call translate(hindiInput) for each translation request
 */
class NmtEngine {

    private val tag = "NmtEngine"
    private var onnxTranslator: OnnxTranslator? = null
    private var isOnnxAvailable: Boolean = false

    /**
     * Initializes the ONNX neural translation backend.
     * Must be called from a coroutine scope during app initialization.
     * If ONNX models are not available, the engine gracefully falls back
     * to the deterministic SQLite fast-path only.
     */
    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        try {
            val translator = OnnxTranslator(context)
            val success = translator.initialize()
            if (success) {
                onnxTranslator = translator
                isOnnxAvailable = true
                Log.i(tag, "NMT Engine initialized: Tier-1 SQLite + Tier-2 ONNX Neural Inference")
            } else {
                Log.w(tag, "ONNX initialization failed — running in SQLite-only mode")
            }
        } catch (e: Exception) {
            Log.w(tag, "ONNX models not available (${e.message}) — running in SQLite-only mode")
        }
    }

    /**
     * Translates Hindi text to Santhali using the multi-tier architecture.
     *
     * @param hindiInput Source Hindi text to translate
     * @return TranslationResult with Ol Chiki translation and metadata
     */
    suspend fun translate(hindiInput: String): TranslationResult {
        val startTime = System.currentTimeMillis()

        // ─── Tier 1: Deterministic SQLite Fast-Path (<0.1ms) ───
        val fastPathResult = FlnRepository.findExactMatch(hindiInput)
        if (fastPathResult != null) {
            val latency = System.currentTimeMillis() - startTime
            Log.i(tag, "Tier-1 SQLite HIT: '$hindiInput' → '${fastPathResult.targetOlChiki}' (${latency}ms)")
            return fastPathResult.copy(
                latencyMs = latency.coerceAtLeast(1),
                isTier1FastPath = true,
                tier = "Tier-1 SQLite"
            )
        }
        Log.d(tag, "Tier-1 SQLite MISS for: '$hindiInput' — routing to Tier-2 ONNX NMT")

        // ─── Tier 2: On-Device ONNX Neural Inference ───
        if (isOnnxAvailable) {
            try {
                val onnxResult = onnxTranslator?.translate(hindiInput)
                if (onnxResult != null && onnxResult.targetText.isNotBlank()) {
                    val latency = System.currentTimeMillis() - startTime

                    // Validate output contains Ol Chiki characters (U+1C50–U+1C7F)
                    val hasOlChiki = onnxResult.targetText.any { ch ->
                        ch.code in 0x1C50..0x1C7F
                    }
                    if (hasOlChiki) {
                        Log.i(tag, "Tier-2 ONNX OK: '$hindiInput' → '${onnxResult.targetText}' (${latency}ms, ${onnxResult.tokenCount} tokens)")
                        return TranslationResult(
                            sourceHindi = hindiInput,
                            targetOlChiki = onnxResult.targetText,
                            phoneticGuide = SanthaliPhonemizer.toPhoneticDevanagari(onnxResult.targetText),
                            isTier1FastPath = false,
                            latencyMs = latency,
                            verifiedByJcert = false, // Neural translations are not JCERT-verified
                            tier = "Tier-2 ONNX"
                        )
                    } else {
                        Log.w(tag, "Tier-2 ONNX output contains NO Ol Chiki characters: '${onnxResult.targetText}' — falling through to Tier-3 Pedagogical Bridge")
                    }
                } else {
                    Log.w(tag, "Tier-2 ONNX returned null or blank for '$hindiInput'")
                }
            } catch (e: Exception) {
                Log.w(tag, "ONNX inference failed for '$hindiInput': ${e.message}")
            }
        } else {
            Log.d(tag, "ONNX not available — routing to Tier-3 Pedagogical Bridge")
        }

        // ─── Tier 3: Intelligent Pedagogical Semantic Engine ───
        val pedagogicalResult = PedagogicalFallbackEngine.translate(hindiInput)
        if (pedagogicalResult != null) {
            val latency = System.currentTimeMillis() - startTime
            Log.i(tag, "Tier-3 Pedagogical HIT: '$hindiInput' → '${pedagogicalResult.targetOlChiki}' (${latency}ms)")
            return pedagogicalResult.copy(
                latencyMs = latency.coerceAtLeast(1),
                isTier1FastPath = false,
                tier = "Tier-3 Bridge"
            )
        }

        // ─── Tier 4: Fallback when all tiers fail ───
        val latency = System.currentTimeMillis() - startTime
        Log.w(tag, "ALL TIERS FAILED for '$hindiInput' — returning fallback (${latency}ms)")
        return TranslationResult(
            sourceHindi = hindiInput,
            targetOlChiki = "⚠ ᱛᱟᱨᱡᱚᱢᱟ ᱵᱟᱝ ᱧᱟᱢ ᱟᱠᱟᱱᱟ", // "Translation not available" in Ol Chiki
            phoneticGuide = "[तारजोमा बांग ञाम आकाना]",
            isTier1FastPath = false,
            latencyMs = latency,
            verifiedByJcert = false,
            tier = "Tier-4 Fallback"
        )
    }

    /**
     * Releases all engine resources.
     */
    fun release() {
        onnxTranslator?.release()
        onnxTranslator = null
        isOnnxAvailable = false
        Log.i(tag, "NMT Engine resources released")
    }
}
