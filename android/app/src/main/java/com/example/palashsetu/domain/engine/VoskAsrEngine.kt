package com.example.palashsetu.domain.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * On-Device Offline Speech-to-Text Engine for Hindi using Kaldi/Vosk.
 *
 * Runs fully on device CPU without cloud dependencies.
 * Unpacks and loads `vosk-model-small-hi-0.22` (44 MB compressed, 82 MB uncompressed).
 */
class VoskAsrEngine(private val context: Context) {

    private val tag = "VoskAsrEngine"

    companion object {
        const val SAMPLE_RATE = 16000.0f
        const val MODEL_DIR_NAME = "vosk-model-hi"

        fun parsePartialJson(rawJson: String): String {
            val match = "\"partial\"\\s*:\\s*\"([^\"]*)\"".toRegex().find(rawJson)
            if (match != null) {
                return match.groupValues[1].trim()
            }
            return try {
                val json = JSONObject(rawJson)
                json.optString("partial", "").trim()
            } catch (e: Throwable) {
                ""
            }
        }

        fun parseFinalJson(rawJson: String): String {
            val match = "\"text\"\\s*:\\s*\"([^\"]*)\"".toRegex().find(rawJson)
            if (match != null) {
                return match.groupValues[1].trim()
            }
            return try {
                val json = JSONObject(rawJson)
                json.optString("text", "").trim()
            } catch (e: Throwable) {
                ""
            }
        }
    }

    private var voskModel: Model? = null
    private var voskRecognizer: Recognizer? = null

    var isInitialized: Boolean = false
        private set

    /**
     * Initializes the Vosk model and speech recognizer.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized && voskRecognizer != null) return@withContext true

        try {
            LibVosk.setLogLevel(LogLevel.WARNINGS)

            val modelsDir = File(context.filesDir, "models")
            if (!modelsDir.exists()) modelsDir.mkdirs()
            val targetModelDir = File(modelsDir, MODEL_DIR_NAME)

            val sentinelFile = File(targetModelDir, "am/final.mdl")
            if (!sentinelFile.exists() || sentinelFile.length() < 10_000_000) {
                Log.i(tag, "Extracting Vosk Hindi ASR model from assets to ${targetModelDir.absolutePath}...")
                targetModelDir.deleteRecursively()
                targetModelDir.mkdirs()

                context.assets.open("models/vosk-model-small-hi.zip").use { inputStream ->
                    ZipInputStream(inputStream).use { zis ->
                        var entry = zis.nextEntry
                        val buffer = ByteArray(64 * 1024)
                        while (entry != null) {
                            val newFile = File(targetModelDir, entry.name)
                            if (entry.isDirectory) {
                                newFile.mkdirs()
                            } else {
                                newFile.parentFile?.mkdirs()
                                FileOutputStream(newFile).use { fos ->
                                    var len: Int
                                    while (zis.read(buffer).also { len = it } > 0) {
                                        fos.write(buffer, 0, len)
                                    }
                                }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }
                Log.i(tag, "Vosk Hindi ASR model extracted successfully.")
            }

            Log.i(tag, "Loading Vosk Model from ${targetModelDir.absolutePath}...")
            val model = Model(targetModelDir.absolutePath)
            voskModel = model
            voskRecognizer = Recognizer(model, SAMPLE_RATE)
            isInitialized = true
            Log.i(tag, "Vosk Hindi Recognizer initialized and ready.")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize Vosk ASR Engine: ${e.message}", e)
            false
        }
    }

    /**
     * Feeds 16-bit PCM audio samples into the Vosk Recognizer.
     * Returns true if a sentence boundary / silent endpoint was recognized.
     */
    fun acceptAudio(samples: ShortArray, length: Int): Boolean {
        val recognizer = voskRecognizer ?: return false
        return recognizer.acceptWaveForm(samples, length)
    }

    /**
     * Retrieves partial real-time transcription text.
     */
    fun getPartialText(): String {
        val recognizer = voskRecognizer ?: return ""
        val rawJson = recognizer.partialResult ?: return ""
        return parsePartialJson(rawJson)
    }

    /**
     * Retrieves final recognized sentence.
     */
    fun getFinalText(): String {
        val recognizer = voskRecognizer ?: return ""
        val rawJson = recognizer.finalResult ?: return ""
        return parseFinalJson(rawJson)
    }

    /**
     * Resets the recognizer state for a new utterance.
     */
    fun reset() {
        voskRecognizer?.reset()
    }

    /**
     * Closes the recognizer and model native resources.
     */
    fun close() {
        try {
            voskRecognizer?.close()
            voskRecognizer = null
            voskModel?.close()
            voskModel = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(tag, "Error closing Vosk ASR Engine: ${e.message}", e)
        }
    }
}
