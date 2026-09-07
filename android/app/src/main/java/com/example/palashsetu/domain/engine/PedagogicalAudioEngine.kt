package com.example.palashsetu.domain.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class AudioPlayerState {
    object Idle : AudioPlayerState()
    object Synthesizing : AudioPlayerState()
    data class Playing(val progress: Float, val speed: Float) : AudioPlayerState()
    object Finished : AudioPlayerState()
}

class PedagogicalAudioEngine {

    var currentSpeed: Float = 0.9f // Default pedagogical clarity cadence

    suspend fun playSynthesizedAudio(olchikiText: String): Flow<AudioPlayerState> = flow {
        emit(AudioPlayerState.Synthesizing)
        delay(65) // Piper ONNX synthesis latency (RTF ~0.05)

        val totalSteps = 20
        val stepDelay = if (currentSpeed == 0.9f) 140L else 110L

        for (i in 1..totalSteps) {
            val progress = i / totalSteps.toFloat()
            emit(AudioPlayerState.Playing(progress, currentSpeed))
            delay(stepDelay)
        }

        emit(AudioPlayerState.Finished)
        delay(300)
        emit(AudioPlayerState.Idle)
    }

    fun toggleSpeed(): Float {
        currentSpeed = if (currentSpeed == 0.9f) 1.0f else 0.9f
        return currentSpeed
    }
}
