package com.example.palashsetu.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NipunCurriculumRepositoryTest {

    @Test
    fun testAllCompetenciesCount() {
        val all = NipunCurriculumRepository.getAllCompetencies()
        assertEquals(24, all.size)
    }

    @Test
    fun testCompetencyCountsPerGrade() {
        val grade1 = NipunCurriculumRepository.getCompetenciesForGrade(grade = 1)
        val grade2 = NipunCurriculumRepository.getCompetenciesForGrade(grade = 2)
        val grade3 = NipunCurriculumRepository.getCompetenciesForGrade(grade = 3)

        assertEquals(8, grade1.size)
        assertEquals(8, grade2.size)
        assertEquals(8, grade3.size)
    }

    @Test
    fun testDomainCounts() {
        val literacy = NipunCurriculumRepository.getCompetenciesForDomain(domain = "LITERACY")
        val numeracy = NipunCurriculumRepository.getCompetenciesForDomain(domain = "NUMERACY")

        assertEquals(12, literacy.size)
        assertEquals(12, numeracy.size)
    }

    @Test
    fun testBilingualFidelityForAll24Competencies() {
        val all = NipunCurriculumRepository.getAllCompetencies()
        for (comp in all) {
            assertTrue("Competency ${comp.code} must have non-empty English title", comp.getTitle(isHindi = false).isNotBlank())
            assertTrue("Competency ${comp.code} must have non-empty Hindi title", comp.getTitle(isHindi = true).isNotBlank())
            assertTrue("Competency ${comp.code} must have non-empty English subtitle", comp.getSubtitle(isHindi = false).isNotBlank())
            assertTrue("Competency ${comp.code} must have non-empty Hindi subtitle", comp.getSubtitle(isHindi = true).isNotBlank())
            assertTrue("Competency ${comp.code} must have non-empty Hindi instruction", comp.instructionHi.isNotBlank())
            assertTrue("Competency ${comp.code} must have non-empty Ol Chiki instruction", comp.instructionOlchiki.isNotBlank())
        }
    }

    @Test
    fun testFilterCompetencies() {
        // Test filtering by Grade 2 + Numeracy
        val g2Math = NipunCurriculumRepository.filterCompetencies(grade = 2, domain = "NUMERACY")
        assertEquals(4, g2Math.size)
        assertTrue(g2Math.all { it.grade == 2 && it.isNumeracy })

        // Test search by code
        val m24Results = NipunCurriculumRepository.filterCompetencies(searchQuery = "M2.4")
        assertEquals(1, m24Results.size)
        assertEquals("M2.4", m24Results.first().code)

        // Test search by Hindi query
        val jorResults = NipunCurriculumRepository.filterCompetencies(searchQuery = "जोड़")
        assertFalse("Search for 'जोड़' should return addition competencies", jorResults.isEmpty())

        // Test search by English query
        val countingResults = NipunCurriculumRepository.filterCompetencies(searchQuery = "Counting")
        assertFalse("Search for 'Counting' should return matching competencies", countingResults.isEmpty())
    }

    @Test
    fun testDefaultCompetencies() {
        val defaultG1 = NipunCurriculumRepository.getDefaultCompetency(grade = 1)
        val defaultG2 = NipunCurriculumRepository.getDefaultCompetency(grade = 2)
        val defaultG3 = NipunCurriculumRepository.getDefaultCompetency(grade = 3)

        assertEquals("M1.1", defaultG1.code)
        assertEquals("M2.4", defaultG2.code)
        assertEquals("M3.1", defaultG3.code)
    }

    @Test
    fun testM24ForestProduceCompetencyContent() {
        val m24 = NipunCurriculumRepository.getCompetencyByCode(code = "M2.4")

        assertNotNull("M2.4 competency must exist", m24)
        m24?.let {
            assertEquals(2, it.grade)
            assertEquals("NUMERACY", it.domain)
            assertTrue(it.isNumeracy)
            assertEquals("M2.4: Addition using Indigenous Forest Produce", it.getTitle(isHindi = false))
            assertEquals("M2.4: वनोपज व प्राकृतिक वस्तुओं से जोड़", it.getTitle(isHindi = true))
            assertEquals("जंगल की वस्तुओं को जोड़कर संख्या लिखें", it.instructionHi)
            assertEquals("ᱵᱤᱨ ᱨᱮᱱᱟᱜ ᱡᱚ ᱡᱚᱲᱟᱣ ᱠᱟᱛᱮ ᱮᱞ ᱚᱞ ᱢᱮ", it.instructionOlchiki)
        }
    }
}
