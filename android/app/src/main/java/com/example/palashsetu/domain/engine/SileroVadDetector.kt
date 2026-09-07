package com.example.palashsetu.domain.engine

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * On-Device Real-Time Voice Activity Detector using Silero VAD v4/v5 (ONNX Runtime).
 *
 * Processes 512-sample (32 ms) 16 kHz 16-bit PCM audio chunks.
 * Evaluates speech probability p in [0.0, 1.0] and manages internal recurrent RNN state.
 */
class SileroVadDetector(private val context: Context) {

    private val tag = "SileroVadDetector"

    companion object {
        const val SAMPLE_RATE = 16000L
        const val FRAME_SIZE = 512 // 32 ms at 16 kHz
        const val STATE_SIZE = 2 * 1 * 128 // 256 floats for recurrent state
        const val DEFAULT_THRESHOLD = 0.50f
        const val SILENCE_HANGOVER_FRAMES = 25 // ~800 ms of continuous silence triggers cutoff
    }

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    // Recurrent state tensor buffer (persists across consecutive 32 ms audio frames)
    private var rnnState = FloatArray(STATE_SIZE)

    var isInitialized: Boolean = false
        private set

    /**
     * Initializes the ONNX Runtime session for Silero VAD.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized && ortSession != null) return@withContext true

        try {
            val env = OrtEnvironment.getEnvironment()
            ortEnv = env

            val modelDir = File(context.filesDir, "models")
            if (!modelDir.exists()) modelDir.mkdirs()
            val modelFile = File(modelDir, "silero_vad.onnx")

            if (!modelFile.exists() || modelFile.length() < 1000) {
                Log.i(tag, "Extracting silero_vad.onnx from assets to internal storage...")
                context.assets.open("models/silero_vad.onnx").use { input ->
                    FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(1)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }

            ortSession = env.createSession(modelFile.absolutePath, sessionOptions)
            reset()
            isInitialized = true
            Log.i(tag, "Silero VAD initialized successfully from ${modelFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize Silero VAD: ${e.message}", e)
            false
        }
    }

    /**
     * Evaluates a single 512-sample PCM chunk and returns speech probability in [0.0, 1.0].
     */
    fun processFrame(samples: ShortArray): Float {
        val session = ortSession ?: return 0.0f
        val env = ortEnv ?: return 0.0f

        val numSamples = minOf(samples.size, FRAME_SIZE)
        val floatSamples = FloatArray(FRAME_SIZE)
        for (i in 0 until numSamples) {
            floatSamples[i] = samples[i] / 32768.0f
        }

        return try {
            val inputTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(floatSamples),
                longArrayOf(1, FRAME_SIZE.toLong())
            )

            val stateTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(rnnState),
                longArrayOf(2, 1, 128)
            )

            val srTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(longArrayOf(SAMPLE_RATE)),
                longArrayOf()
            )

            val inputs = mapOf(
                "input" to inputTensor,
                "state" to stateTensor,
                "sr" to srTensor
            )

            val results = session.run(inputs)

            // Extract output probability
            val outputTensor = results.get("output").get() as OnnxTensor
            val outputValue = outputTensor.value as Array<FloatArray>
            val speechProbability = outputValue[0][0]

            // Update recurrent state for next frame
            val nextStateTensor = results.get("stateN").get() as OnnxTensor
            val nextStateValue = nextStateTensor.value as Array<Array<FloatArray>>
            var idx = 0
            for (i in 0 until 2) {
                for (j in 0 until 1) {
                    for (k in 0 until 128) {
                        rnnState[idx++] = nextStateValue[i][j][k]
                    }
                }
            }

            inputTensor.close()
            stateTensor.close()
            srTensor.close()
            results.close()

            speechProbability
        } catch (e: Exception) {
            Log.w(tag, "Silero VAD inference error: ${e.message}")
            0.0f
        }
    }

    /**
     * Resets the recurrent RNN state to zeros (call at the beginning of each recording session).
     */
    fun reset() {
        rnnState.fill(0.0f)
    }

    /**
     * Closes the ONNX session and frees native resources.
     */
    fun close() {
        try {
            ortSession?.close()
            ortSession = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(tag, "Error closing Silero VAD session: ${e.message}", e)
        }
    }
}
