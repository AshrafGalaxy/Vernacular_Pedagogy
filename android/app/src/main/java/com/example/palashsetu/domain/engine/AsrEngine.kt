package com.example.palashsetu.domain.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class AsrState {
    object Idle : AsrState()
    data class Listening(val audioLevel: Float = 0.5f) : AsrState()
    data class PartialText(val text: String) : AsrState()
    data class Recognized(val finalSentence: String, val latencyMs: Long = 180) : AsrState()
}

class AsrEngine {

    private var isListening = false

    suspend fun startListening(sampleSentence: String = "बच्चो, अपनी गणित की किताब निकालो और पृष्ठ संख्या बारह खोलो।"): Flow<AsrState> = flow {
        isListening = true
        emit(AsrState.Listening(0.2f))
        delay(200)

        // Stream partial words
        val words = sampleSentence.split(" ")
        val partialBuilder = StringBuilder()
        for (i in words.indices) {
            if (!isListening) break
            if (partialBuilder.isNotEmpty()) partialBuilder.append(" ")
            partialBuilder.append(words[i])
            emit(AsrState.PartialText(partialBuilder.toString()))
            delay(120)
        }

        delay(150)
        emit(AsrState.Recognized(sampleSentence, latencyMs = 180))
        isListening = false
    }

    fun stopListening() {
        isListening = false
    }
}
