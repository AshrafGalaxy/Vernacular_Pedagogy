package com.example.palashsetu.domain.engine

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SanthaliPhonemizerTest {

    @Test
    fun testOlChikiDigitsToWords() {
        val ipa = SanthaliPhonemizer.santhaliToIpa("᱑")
        // ᱑ -> ᱢᱤᱫ -> m i d
        assertTrue("Output should contain IPA for mid (m, i, d)", ipa.contains("m") && ipa.contains("i") && ipa.contains("d"))
    }

    @Test
    fun testCompoundPhonemes() {
        // Deglottalized plosives with Ohod
        val ipaOhod = SanthaliPhonemizer.santhaliToIpa("ᱫᱽ")
        assertEquals("d", ipaOhod)

        // Aspirated plosives with Oh
        val ipaTh = SanthaliPhonemizer.santhaliToIpa("ᱛᱷ")
        assertEquals("tʰ", ipaTh)

        // Sarjom Dare flashcard
        val ipaSarjom = SanthaliPhonemizer.santhaliToIpa("ᱥᱟᱨᱡᱚᱢ ᱫᱟᱨᱮ")
        assertTrue("Sarjom should contain s, a, r, ɟ, ɔ, m", ipaSarjom.contains("s") && ipaSarjom.contains("ɟ"))
    }

    @Test
    fun testTokenizationProducesPadAndBounds() {
        val mockPhonemeMap = mapOf(
            "^" to listOf(1L),
            "$" to listOf(2L),
            "_" to listOf(0L),
            " " to listOf(3L),
            "s" to listOf(10L),
            "a" to listOf(11L)
        )

        val ids = SanthaliPhonemizer.textToPhonemeIds("ᱥᱟ", mockPhonemeMap)
        assertTrue("IDs must start with BOS (1)", ids.first() == 1L)
        assertTrue("IDs must end with EOS (2)", ids.last() == 2L)
        assertTrue("Should contain inter-token padding (0)", ids.contains(0L))
    }

    @Test
    fun testPedagogicalAudioEngineSpeedToggle() {
        val engine = PedagogicalAudioEngine()
        assertEquals(0.9f, engine.currentSpeed, 0.001f)

        val speed1 = engine.toggleSpeed()
        assertEquals(1.0f, speed1, 0.001f)

        val speed2 = engine.toggleSpeed()
        assertEquals(0.9f, speed2, 0.001f)
    }

    @Test
    fun testHeadlessSimulationProgress() = runBlocking {
        val engine = PedagogicalAudioEngine() // Headless: no context
        val states = engine.playSynthesizedAudio("ᱥᱟᱨᱡᱚᱢ ᱫᱟᱨᱮ").toList()

        assertTrue("Must emit states", states.isNotEmpty())
        assertTrue("First state should be Synthesizing", states.first() is AudioPlayerState.Synthesizing)
        assertTrue("Should have Playing states", states.any { it is AudioPlayerState.Playing })
        assertTrue("Should emit Finished", states.any { it is AudioPlayerState.Finished })
        assertTrue("Last state should be Idle", states.last() is AudioPlayerState.Idle)
    }
}
