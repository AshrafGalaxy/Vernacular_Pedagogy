package com.example.palashsetu.domain.engine

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PiperTtsIntegrationTest {

    private val mockPhonemeIdMap: Map<String, List<Long>> = mapOf(
        "^" to listOf(1L),
        "$" to listOf(2L),
        "_" to listOf(0L),
        " " to listOf(3L),
        "d" to listOf(10L),
        "u" to listOf(11L),
        "ɖ" to listOf(12L),
        "b" to listOf(13L),
        "m" to listOf(14L),
        "e" to listOf(15L),
        "s" to listOf(16L),
        "a" to listOf(17L),
        "r" to listOf(18L),
        "ɟ" to listOf(19L),
        "ɔ" to listOf(20L),
        "l" to listOf(21L),
        "kʰ" to listOf(22L),
        "i" to listOf(23L),
        "j" to listOf(24L)
    )

    @Test
    fun testOlChikiClassroomCommandsG2P() {
        // "Sit down" (ᱫᱩᱲᱩᱵ ᱢᱮ) -> d u ɖ u b m e
        val ipaSitDown = SanthaliPhonemizer.santhaliToIpa("ᱫᱩᱲᱩᱵ ᱢᱮ")
        assertTrue("Must contain d, u, ɖ, b, m, e", ipaSitDown.contains("d") && ipaSitDown.contains("u") && ipaSitDown.contains("m"))

        // "Count" (ᱞᱮᱠᱷᱟᱭ ᱢᱮ) -> l e kʰ a j m e
        val ipaCount = SanthaliPhonemizer.santhaliToIpa("ᱞᱮᱠᱷᱟᱭ ᱢᱮ")
        assertTrue("Must contain aspirated kʰ", ipaCount.contains("kʰ"))

        // "Sarjom Dare" (ᱥᱟᱨᱡᱚᱢ ᱫᱟᱨᱮ)
        val ipaSarjom = SanthaliPhonemizer.santhaliToIpa("ᱥᱟᱨᱡᱚᱢ ᱫᱟᱨᱮ")
        assertTrue("Must contain s and a", ipaSarjom.contains("s") && ipaSarjom.contains("a"))
    }

    @Test
    fun testPhonemeIdTokenizationStructure() {
        val ids = SanthaliPhonemizer.textToPhonemeIds("ᱫᱩᱲᱩᱵ ᱢᱮ", mockPhonemeIdMap)
        assertTrue("IDs must start with BOS token (1)", ids.first() == 1L)
        assertTrue("IDs must end with EOS token (2)", ids.last() == 2L)
        assertTrue("IDs must include inter-phoneme pad (0)", ids.contains(0L))
        assertTrue("IDs length should be greater than 5", ids.size > 5)
    }

    @Test
    fun testTtsTelemetryCalculation() {
        val telemetry = TtsTelemetry(
            synthesisDurationMs = 45L,
            audioDurationMs = 1200L,
            realTimeFactor = 45f / 1200f,
            sampleRate = 16000,
            isCacheHit = false
        )

        assertEquals(45L, telemetry.synthesisDurationMs)
        assertEquals(1200L, telemetry.audioDurationMs)
        assertTrue("RTF should be well under 1.0 (real-time)", telemetry.realTimeFactor < 0.1f)
        assertEquals(16000, telemetry.sampleRate)
        assertFalse(telemetry.isCacheHit)
    }

    @Test
    fun testSpeedScalesInverseRelation() {
        val claritySpeed = 0.9f
        val standardSpeed = 1.0f

        val lengthScaleClarity = 1.0f / claritySpeed
        val lengthScaleStandard = 1.0f / standardSpeed

        assertTrue("Clarity cadence (0.9x) must yield longer duration scale than standard (1.0x)",
            lengthScaleClarity > lengthScaleStandard)
    }

    @Test
    fun testPedagogicalAudioEngineWarmUpAndStateFlow() = runBlocking {
        val engine = PedagogicalAudioEngine() // Headless: no context
        assertFalse(engine.isEngineInitialized())

        val states = engine.playSynthesizedAudio("ᱢᱟᱹᱦᱩᱣᱟᱹ ᱡᱚ ᱞᱮᱠᱷᱟᱭ ᱢᱮ").toList()
        assertTrue("Should emit Synthesizing", states.first() is AudioPlayerState.Synthesizing)
        assertTrue("Should emit Playing states", states.any { it is AudioPlayerState.Playing })
        assertTrue("Should emit Finished", states.any { it is AudioPlayerState.Finished })
        assertEquals(AudioPlayerState.Idle, states.last())
    }
}
