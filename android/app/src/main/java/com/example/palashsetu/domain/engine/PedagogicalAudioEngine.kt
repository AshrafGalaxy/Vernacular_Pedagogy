package com.example.palashsetu.domain.engine

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class AudioPlayerState {
    object Idle : AudioPlayerState()
    object Synthesizing : AudioPlayerState()
    data class Playing(val progress: Float, val speed: Float) : AudioPlayerState()
    object Finished : AudioPlayerState()
}

/**
 * Pedagogical Audio Engine coordinating on-device Piper neural TTS.
 *
 * Runs on-device Piper VITS neural synthesis on CPU for any Santhali Ol Chiki text,
 * dynamically adjusting speech rate (0.9x clarity cadence vs 1.0x standard).
 */
class PedagogicalAudioEngine(
    private var defaultContext: Context? = null
) {
    var currentSpeed: Float = 0.9f // Default pedagogical clarity cadence

    private var ttsEngine: PiperTtsEngine? = null

    init {
        defaultContext?.let {
            ttsEngine = PiperTtsEngine(it.applicationContext)
        }
    }

    fun setContext(context: Context) {
        this.defaultContext = context.applicationContext
        if (ttsEngine == null) {
            ttsEngine = PiperTtsEngine(context.applicationContext)
        }
    }

    suspend fun playSynthesizedAudio(
        olchikiText: String,
        phraseId: String? = null,
        context: Context? = null
    ): Flow<AudioPlayerState> {
        val targetContext = context ?: defaultContext
        if (targetContext != null && ttsEngine == null) {
            ttsEngine = PiperTtsEngine(targetContext.applicationContext)
        }

        val engine = ttsEngine
        return if (engine != null) {
            engine.synthesizeAndPlay(olchikiText, currentSpeed)
        } else {
            // Headless / Unit Test simulated fallback
            flow {
                emit(AudioPlayerState.Synthesizing)
                delay(40)
                val steps = 15
                val stepDelay = if (currentSpeed == 0.9f) 120L else 95L
                for (i in 1..steps) {
                    emit(AudioPlayerState.Playing(i / steps.toFloat(), currentSpeed))
                    delay(stepDelay)
                }
                emit(AudioPlayerState.Finished)
                delay(150)
                emit(AudioPlayerState.Idle)
            }
        }
    }

    fun stopPlayback() {
        ttsEngine?.stopAudio()
    }

    fun toggleSpeed(): Float {
        currentSpeed = if (currentSpeed == 0.9f) 1.0f else 0.9f
        return currentSpeed
    }
}
