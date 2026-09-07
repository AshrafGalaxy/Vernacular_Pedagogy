package com.example.palashsetu.domain.engine

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AsrEngineTest {

    @Test
    fun testVoskJsonParsing_partial() {
        val json = """{"partial" : "बच्चो अपनी"}"""
        val result = VoskAsrEngine.parsePartialJson(json)
        assertEquals("बच्चो अपनी", result)
    }

    @Test
    fun testVoskJsonParsing_emptyPartial() {
        val json = """{"partial" : ""}"""
        val result = VoskAsrEngine.parsePartialJson(json)
        assertEquals("", result)
    }

    @Test
    fun testVoskJsonParsing_final() {
        val json = """{
            "result" : [
                {"conf" : 1.0, "end" : 0.84, "start" : 0.36, "word" : "बच्चो"}
            ],
            "text" : "बच्चो अपनी किताब निकालो"
        }"""
        val result = VoskAsrEngine.parseFinalJson(json)
        assertEquals("बच्चो अपनी किताब निकालो", result)
    }

    @Test
    fun testVoskJsonParsing_malformed() {
        val malformed = "not a valid json"
        assertEquals("", VoskAsrEngine.parsePartialJson(malformed))
        assertEquals("", VoskAsrEngine.parseFinalJson(malformed))
    }

    @Test
    fun testSileroVadConstants() {
        assertEquals(512, SileroVadDetector.FRAME_SIZE)
        assertEquals(16000L, SileroVadDetector.SAMPLE_RATE)
        assertEquals(256, SileroVadDetector.STATE_SIZE)
        assertEquals(25, SileroVadDetector.SILENCE_HANGOVER_FRAMES)
    }

    @Test
    fun testAudioChunkEquality() {
        val samples1 = shortArrayOf(10, 20, 30)
        val samples2 = shortArrayOf(10, 20, 30)
        val chunk1 = AudioChunk(samples1, 3, 0.4f)
        val chunk2 = AudioChunk(samples2, 3, 0.4f)

        assertEquals(chunk1, chunk2)
        assertEquals(chunk1.hashCode(), chunk2.hashCode())
    }

    @Test
    fun testAsrEngineFallbackStreaming() = runTest {
        val engine = AsrEngine(context = null)
        val sample = "किताब खोलो"
        val states = engine.startListening(sample).toList()

        assertTrue("States should not be empty", states.isNotEmpty())
        assertTrue("First state should be Listening", states.first() is AsrState.Listening)

        val hasPartial = states.any { it is AsrState.PartialText }
        assertTrue("Should emit at least one partial state", hasPartial)

        val lastState = states.last()
        assertTrue("Last state should be Recognized", lastState is AsrState.Recognized)
        val recognized = lastState as AsrState.Recognized
        assertEquals(sample, recognized.finalSentence)
    }
}
