package com.example.palashsetu.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

class FlnRepositoryTest {

    @Test
    fun testCurriculumInitializesAndIsNotEmpty() {
        val allPhrases = FlnRepository.getAllPhrases()
        assertTrue("Curriculum should contain entries", allPhrases.isNotEmpty())
        assertTrue("Phrase count should be at least 10", allPhrases.size >= 10)
    }

    @Test
    fun testExactMatchLookup() {
        val result = FlnRepository.findExactMatch("किताब खोलो")
        assertNotNull("Should find exact match for 'किताब खोलो'", result)
        assertEquals("ᱯᱩᱛᱷᱤ ᱡᱷᱤᱡᱽ ᱢᱮ", result?.targetOlChiki)
        assertEquals("[पुथी झीज मे]", result?.phoneticGuide)
        assertTrue(result?.isTier1FastPath == true)
    }

    @Test
    fun testEdgeCasesPunctuationAndWhitespace() {
        // Trailing purnaviram
        val r1 = FlnRepository.findExactMatch("किताब खोलो।")
        assertNotNull("Should match with trailing purnaviram", r1)
        assertEquals("ᱯᱩᱛᱷᱤ ᱡᱷᱤᱡᱽ ᱢᱮ", r1?.targetOlChiki)

        // Trailing exclamation
        val r2 = FlnRepository.findExactMatch("किताब खोलो!")
        assertNotNull("Should match with trailing exclamation", r2)

        // Leading and trailing spaces
        val r3 = FlnRepository.findExactMatch("   बैठ जाओ   ")
        assertNotNull("Should match with padded spaces", r3)
        assertEquals("ᱫᱩᱲᱩᱵ ᱢᱮ", r3?.targetOlChiki)

        // Question mark
        val r4 = FlnRepository.findExactMatch("क्या सबको समझ आया?")
        assertNotNull("Should match query with question mark", r4)
    }

    @Test
    fun testCategoryFiltering() {
        val classroomPhrases = FlnRepository.getPhrasesByCategory("कक्षा प्रबंधन")
        assertTrue("Classroom category should have items", classroomPhrases.isNotEmpty())
        assertTrue(classroomPhrases.all { it.category == "कक्षा प्रबंधन" })

        val mathPhrases = FlnRepository.getPhrasesByCategory("गिनती व गणित")
        assertTrue("Math category should have items", mathPhrases.isNotEmpty())
        assertTrue(mathPhrases.all { it.category == "गिनती व गणित" })

        val allPhrases = FlnRepository.getPhrasesByCategory("सभी (All)")
        assertEquals(FlnRepository.getAllPhrases().size, allPhrases.size)
    }

    @Test
    fun testGradeFiltering() {
        val grade1 = FlnRepository.getPhrasesByGrade(1)
        assertTrue("Grade 1 should contain items", grade1.isNotEmpty())
        assertTrue(grade1.all { it.grade == 1 })

        val grade2 = FlnRepository.getPhrasesByGrade(2)
        assertTrue("Grade 2 should contain items", grade2.isNotEmpty())
        assertTrue(grade2.all { it.grade == 2 })
    }

    @Test
    fun testMultilingualSearch() {
        // Search by Ol Chiki
        val olchikiMatches = FlnRepository.searchPhrases("ᱯᱩᱛᱷᱤ")
        assertTrue("Search by Ol Chiki should return results", olchikiMatches.isNotEmpty())

        // Search by Hindi
        val hindiMatches = FlnRepository.searchPhrases("किताब")
        assertTrue("Search by Hindi should return results", hindiMatches.isNotEmpty())

        // Search by English
        val englishMatches = FlnRepository.searchPhrases("book")
        assertTrue("Search by English should return results", englishMatches.isNotEmpty())

        // Search by Devanagari phonetic guide
        val phoneticMatches = FlnRepository.searchPhrases("पुथी")
        assertTrue("Search by phonetic guide should return results", phoneticMatches.isNotEmpty())

        // Empty / Blank query returns full list
        val allOnEmpty = FlnRepository.searchPhrases("   ")
        assertEquals(FlnRepository.getAllPhrases().size, allOnEmpty.size)

        // Unseen gibberish returns empty list
        val emptyResult = FlnRepository.searchPhrases("xyz999nonsense")
        assertTrue("Gibberish should return empty list", emptyResult.isEmpty())
    }

    @Test
    fun testThrottlingAndLatencyStressBenchmark() {
        // Warm up index
        for (i in 0..100) {
            FlnRepository.findExactMatch("किताब खोलो")
        }

        val iterations = 5000
        val queries = listOf("किताब खोलो", "बैठ जाओ", "1 से 10 गिनो", "शांत रहें", "ताली बजाओ")

        val totalNano = measureNanoTime {
            for (i in 0 until iterations) {
                val q = queries[i % queries.size]
                val match = FlnRepository.findExactMatch(q)
                assertNotNull(match)
            }
        }

        val avgMicroseconds = (totalNano / iterations.toDouble()) / 1000.0
        val qps = (iterations / (totalNano / 1_000_000_000.0)).toLong()

        println("=== FLN TIER-1 LOOKUP STRESS TEST RESULTS ===")
        println("Iterations: $iterations queries")
        println("Average Latency: ${avgMicroseconds} microseconds (${avgMicroseconds / 1000.0} ms)")
        println("Throughput: $qps QPS")

        // SLA assertion: Must be < 1.0 ms (target SLA is < 15.0 ms)
        assertTrue("Tier-1 cache latency must be under 1ms, measured: ${avgMicroseconds / 1000.0} ms", avgMicroseconds < 1000.0)
    }
}
