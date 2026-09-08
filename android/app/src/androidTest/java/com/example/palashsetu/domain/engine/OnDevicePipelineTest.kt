package com.example.palashsetu.domain.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.palashsetu.data.local.FlnRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnDevicePipelineTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        FlnRepository.initialize(context)
    }

    @Test
    fun testOnDeviceNmtPipelineRouting() = runBlocking {
        val nmtEngine = NmtEngine()
        nmtEngine.initialize(context)

        // Dynamic classroom statement that requires Tier-3 pedagogical fallback or Tier-2 ONNX
        val query = "आज हम खेलेंगे"
        val result = nmtEngine.translate(query)

        assertNotNull("Translation result must not be null", result)
        assertTrue(
            "Target Ol Chiki must contain valid Ol Chiki characters, got: '${result.targetOlChiki}'",
            result.targetOlChiki.any { it.code in 0x1C50..0x1C7F }
        )
        assertEquals("ᱛᱮᱦᱮᱧ ᱟᱵᱚ ᱵᱚ ᱮᱱᱮᱡᱟ", result.targetOlChiki)
        assertTrue("Latency must be < 400ms, actual: ${result.latencyMs}ms", result.latencyMs < 400)
        assertTrue("Routing tier must be valid", result.tier.isNotBlank())
        android.util.Log.i("OnDevicePipelineTest", "RESULT: query='$query' -> '${result.targetOlChiki}' tier='${result.tier}' latency=${result.latencyMs}ms")

        nmtEngine.release()
    }

    @Test
    fun testOnDeviceFastPathExactMatch() = runBlocking {
        val nmtEngine = NmtEngine()
        nmtEngine.initialize(context)

        // Exact match from curriculum
        val query = "किताब खोलो"
        val result = nmtEngine.translate(query)

        assertNotNull(result)
        assertTrue(result.targetOlChiki.any { it.code in 0x1C50..0x1C7F })
        assertTrue("Tier 1 fast path should have near-zero latency", result.latencyMs < 50)
        android.util.Log.i("OnDevicePipelineTest", "FASTPATH: query='$query' -> '${result.targetOlChiki}' tier='${result.tier}' latency=${result.latencyMs}ms")

        nmtEngine.release()
    }

    @Test
    fun testOnDevicePiperTtsAudioLifecycle() = runBlocking {
        val audioEngine = PedagogicalAudioEngine(context)
        audioEngine.warmUp()

        val olChiki = "ᱛᱮᱦᱮᱧ ᱟᱵᱚ ᱵᱚ ᱮᱱᱮᱡᱟ"
        val states = audioEngine.playSynthesizedAudio(olChiki).toList()

        assertTrue("States should not be empty", states.isNotEmpty())
        assertTrue("First state should be Synthesizing", states.first() is AudioPlayerState.Synthesizing)
        assertTrue("Should contain Playing states", states.any { it is AudioPlayerState.Playing })
        assertTrue("Should contain Finished state", states.any { it is AudioPlayerState.Finished })
        assertEquals("Last state must be Idle", AudioPlayerState.Idle, states.last())

        val telemetry = audioEngine.lastTelemetry
        if (telemetry != null) {
            android.util.Log.i("OnDevicePipelineTest", "TTS Telemetry: synth=${telemetry.synthesisDurationMs}ms audio=${telemetry.audioDurationMs}ms RTF=${telemetry.realTimeFactor}")
        }
    }
}
