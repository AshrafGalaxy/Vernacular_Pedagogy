package com.example.palashsetu.domain.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.sqrt

data class AudioChunk(
    val samples: ShortArray,
    val length: Int,
    val audioLevel: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioChunk
        return length == other.length && audioLevel == other.audioLevel && samples.contentEquals(other.samples)
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + length
        result = 31 * result + audioLevel.hashCode()
        return result
    }
}

/**
 * Robust Native Audio Recorder capturing 16 kHz 16-bit Mono PCM chunks (512 samples / 32 ms).
 *
 * Emits AudioChunk stream with normalized RMS audio levels for real-time visualization.
 */
class LiveAudioRecorder(private val context: Context) {

    private val tag = "LiveAudioRecorder"

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val FRAME_SIZE = 512 // 32 ms at 16 kHz
    }

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording: Boolean = false

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Starts audio capture and streams 512-sample AudioChunk instances via Kotlin Flow.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(): Flow<AudioChunk> = flow {
        if (!hasRecordPermission()) {
            Log.e(tag, "RECORD_AUDIO permission is not granted.")
            return@flow
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        val bufferSize = maxOf(minBufferSize, FRAME_SIZE * 4)

        var record: AudioRecord? = null
        try {
            // Attempt hardware voice recognition source first (with AGC/NS)
            record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        } catch (e: Exception) {
            Log.w(tag, "VOICE_RECOGNITION source unavailable, falling back to MIC: ${e.message}")
        }

        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(tag, "AudioRecord failed to initialize.")
            record.release()
            return@flow
        }

        audioRecord = record
        isRecording = true

        try {
            record.startRecording()
            Log.i(tag, "LiveAudioRecorder started capturing at 16 kHz...")

            val buffer = ShortArray(FRAME_SIZE)

            while (currentCoroutineContext().isActive && isRecording) {
                val readSamples = record.read(buffer, 0, FRAME_SIZE)
                if (readSamples > 0) {
                    // Compute RMS amplitude for UI meter
                    var sumSquares = 0.0
                    for (i in 0 until readSamples) {
                        val sample = buffer[i].toDouble()
                        sumSquares += sample * sample
                    }
                    val rms = sqrt(sumSquares / readSamples)
                    val audioLevel = (rms / 10000.0).toFloat().coerceIn(0.05f, 1.0f)

                    val chunkArray = buffer.copyOf(readSamples)
                    emit(AudioChunk(samples = chunkArray, length = readSamples, audioLevel = audioLevel))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error during audio recording loop: ${e.message}", e)
        } finally {
            try {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
            } catch (e: Exception) {
                Log.w(tag, "Error stopping AudioRecord: ${e.message}")
            }
            record.release()
            audioRecord = null
            isRecording = false
            Log.i(tag, "LiveAudioRecorder stopped and released.")
        }
    }.flowOn(Dispatchers.IO)

    fun stopRecording() {
        isRecording = false
    }
}
