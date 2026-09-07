package com.example.palashsetu.data.local

import org.junit.Assert.assertEquals
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
            assertEquals("M2.4: Addition using Indigenous Forest Produce", it.title)
            assertEquals("जंगल की वस्तुओं को जोड़कर संख्या लिखें", it.instructionHi)
            assertEquals("ᱵᱤᱨ ᱨᱮᱱᱟᱜ ᱡᱚ ᱡᱚᱲᱟᱣ ᱠᱟᱛᱮ ᱮᱞ ᱚᱞ ᱢᱮ", it.instructionOlchiki)
        }
    }
}
