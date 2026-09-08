package com.example.palashsetu.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PedagogicalBridgeTest {

    @Test
    fun testDynamicClassroomPatterns() {
        // "आज हम खेलेंगे" -> ᱛᱮᱦᱮᱧ ᱟᱵᱚ ᱵᱚ ᱮᱱᱮᱡᱟ
        val resPlay = PedagogicalFallbackEngine.translate("आज हम खेलेंगे")
        assertNotNull("Should translate 'आज हम खेलेंगे'", resPlay)
        assertEquals("ᱛᱮᱦᱮᱧ ᱟᱵᱚ ᱵᱚ ᱮᱱᱮᱡᱟ", resPlay?.targetOlChiki)
        assertEquals("Tier-3 Bridge", resPlay?.tier)
        assertTrue("Latency must be < 50ms", (resPlay?.latencyMs ?: 999) < 50)
        assertTrue("Verified by JCERT", resPlay?.verifiedByJcert == true)

        // "चित्र बनाओ" -> ᱪᱤᱛᱟᱹᱨ ᱵᱮᱱᱟᱣ ᱢᱮ
        val resDraw = PedagogicalFallbackEngine.translate("चित्र बनाओ")
        assertNotNull("Should translate 'चित्र बनाओ'", resDraw)
        assertEquals("ᱪᱤᱛᱟᱹᱨ ᱵᱮᱱᱟᱣ ᱢᱮ", resDraw?.targetOlChiki)

        // "पानी पी लो" -> ᱫᱟᱜ ᱧᱩᱭ ᱢᱮ
        val resWater = PedagogicalFallbackEngine.translate("पानी पी लो")
        assertNotNull("Should translate 'पानी पी लो'", resWater)
        assertEquals("ᱫᱟᱜ ᱧᱩᱭ ᱢᱮ", resWater?.targetOlChiki)
    }

    @Test
    fun testOlChikiUnicodeIntegrity() {
        val testSentences = listOf(
            "आज हम खेलेंगे",
            "किताब खोलो",
            "शांत रहो",
            "ताली बजाओ",
            "हाथ धो लो",
            "चित्र बनाओ",
            "कक्षा में आओ",
            "जोर से बोलो",
            "घर जाओ",
            "पानी पी लो"
        )

        for (sent in testSentences) {
            val res = PedagogicalFallbackEngine.translate(sent)
            assertNotNull("Should handle sentence: $sent", res)
            val olChiki = res!!.targetOlChiki

            // Every non-space non-punctuation character must be in Ol Chiki Unicode range U+1C50..U+1C7F
            val nonOlChikiChars = olChiki.filter { ch ->
                ch != ' ' && ch != '।' && ch != '.' && ch != '?' && ch != '!' && ch != '-' &&
                (ch.code !in 0x1C50..0x1C7F)
            }
            assertTrue(
                "Sentence '$sent' produced invalid non-Ol-Chiki glyphs: '$nonOlChikiChars' in '$olChiki'",
                nonOlChikiChars.isEmpty()
            )
        }
    }

    @Test
    fun testClauseAssembly() {
        // "आज बच्चे खेलेंगे" -> ᱛᱮᱦᱮᱧ ᱜᱤᱫᱽᱨᱟᱹ ᱠᱚ ᱮᱱᱮᱡ ᱯᱮ
        val res = PedagogicalFallbackEngine.translate("आज बच्चे खेलेंगे")
        assertNotNull(res)
        assertTrue(res!!.targetOlChiki.contains("ᱛᱮᱦᱮᱧ"))
        assertTrue(res.targetOlChiki.contains("ᱜᱤᱫᱽᱨᱟᱹ ᱠᱚ"))
    }

    @Test
    fun testPhoneticGuideGeneration() {
        val res = PedagogicalFallbackEngine.translate("आज हम खेलेंगे")
        assertNotNull(res)
        assertTrue(
            "Phonetic guide must be non-blank and formatted in brackets",
            res!!.phoneticGuide.isNotBlank() && res.phoneticGuide.startsWith("[") && res.phoneticGuide.endsWith("]")
        )
    }
}
