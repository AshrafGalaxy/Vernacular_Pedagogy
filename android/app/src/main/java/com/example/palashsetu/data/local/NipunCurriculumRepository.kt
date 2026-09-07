package com.example.palashsetu.data.local

import android.content.Context
import android.util.Log
import com.example.palashsetu.data.model.NipunCompetency
import org.json.JSONArray

/**
 * Repository for the 24 Standard NIPUN Bharat Foundational Competency Outcomes (Grades 1–3).
 *
 * Populated from `assets/schemas/nipun_curriculum_registry.json` with embedded fallback.
 */
object NipunCurriculumRepository {

    private const val TAG = "NipunCurriculumRepo"
    private var cachedCompetencies: List<NipunCompetency>? = null

    /**
     * Retrieves all 24 NIPUN Bharat competencies.
     */
    fun getAllCompetencies(context: Context? = null): List<NipunCompetency> {
        cachedCompetencies?.let { return it }

        val loaded = context?.let { loadFromAssets(it) } ?: emptyList()
        val result = loaded.ifEmpty { getEmbeddedFallbackList() }
        cachedCompetencies = result
        return result
    }

    /**
     * Filters competencies by student grade (Grade 1, 2, or 3).
     */
    fun getCompetenciesForGrade(context: Context? = null, grade: Int): List<NipunCompetency> {
        val all = getAllCompetencies(context)
        val filtered = all.filter { it.grade == grade }
        return filtered.ifEmpty { all }
    }

    /**
     * Retrieves default standard competency for the specified grade.
     * (Grade 2 defaults to M2.4 as highlighted in the design mockup).
     */
    fun getDefaultCompetency(context: Context? = null, grade: Int): NipunCompetency {
        val gradeList = getCompetenciesForGrade(context, grade)
        return when (grade) {
            1 -> gradeList.find { it.code == "M1.1" } ?: gradeList.first()
            2 -> gradeList.find { it.code == "M2.4" } ?: gradeList.first()
            3 -> gradeList.find { it.code == "M3.1" } ?: gradeList.first()
            else -> gradeList.firstOrNull() ?: getEmbeddedFallbackList().first()
        }
    }

    /**
     * Look up competency by outcome code (e.g., "M2.4", "L1.1").
     */
    fun getCompetencyByCode(context: Context? = null, code: String): NipunCompetency? {
        return getAllCompetencies(context).find { it.code.equals(code, ignoreCase = true) }
    }

    private fun loadFromAssets(context: Context): List<NipunCompetency> {
        return try {
            val jsonString = context.assets.open("schemas/nipun_curriculum_registry.json")
                .bufferedReader()
                .use { it.readText() }

            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<NipunCompetency>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    NipunCompetency(
                        code = obj.optString("code", "M2.4"),
                        grade = obj.optInt("grade", 2),
                        domain = obj.optString("domain", "NUMERACY"),
                        title = obj.optString("title", ""),
                        subtitle = obj.optString("subtitle", ""),
                        instructionHi = obj.optString("instruction_hi", ""),
                        instructionOlchiki = obj.optString("instruction_olchiki", ""),
                        motifAsset = obj.optString("motif_asset", ""),
                        templateType = obj.optString("template_type", "")
                    )
                )
            }
            Log.i(TAG, "Loaded ${list.size} NIPUN competencies from assets.")
            list
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load competencies from assets, using fallback: ${e.message}")
            emptyList()
        }
    }

    private fun getEmbeddedFallbackList(): List<NipunCompetency> {
        return listOf(
            // Grade 1
            NipunCompetency("L1.1", 1, "LITERACY", "L1.1: Native Script Character Recognition", "Identify and circle Ol Chiki base alphabets", "अक्षर पहचान कर गोला लगाओ", "ᱟᱠᱷᱚᱨ ᱪᱤᱱᱦᱟᱹᱣ ᱠᱟᱛᱮ ᱜᱩᱞ ᱢᱮ"),
            NipunCompetency("L1.2", 1, "LITERACY", "L1.2: Sound Blending & Syllables", "Blend individual phonetic sounds into simple words", "ध्वनियों को जोड़कर सही शब्द बनाओ", "ᱥᱟᱰᱮ ᱠᱚ ᱡᱚᱲᱟᱣ ᱠᱟᱛᱮ ᱟᱹᱲᱟᱹ ᱵᱮᱱᱟᱣ ᱢᱮ"),
            NipunCompetency("L1.3", 1, "LITERACY", "L1.3: Object-to-Word Bilingual Pairing", "Match indigenous rural objects with their names", "चित्र देखकर सही नाम से मिलाओ", "ᱪᱤᱛᱟᱹᱨ ᱧᱮᱞ ᱠᱟᱛᱮ ᱥᱟᱹᱨᱤ ᱧᱩᱛᱩᱢ ᱥᱟᱞᱟᱜ ᱡᱚᱲᱟᱣ ᱢᱮ"),
            NipunCompetency("L1.4", 1, "LITERACY", "L1.4: Print Concepts & Letter Tracking", "Track text direction and identify word spaces", "शब्दों के बीच सही खाली जगह पहचानो", "ᱟᱹᱲᱟᱹ ᱠᱚ ᱛᱟᱞᱟ ᱨᱮ ᱯᱷᱟᱸᱠ ᱪᱤᱱᱦᱟᱹᱣ ᱢᱮ"),
            NipunCompetency("M1.1", 1, "NUMERACY", "M1.1: Pictorial Counting up to 9", "Count concrete village items (Mahua seeds & Sal leaves)", "महुआ के फलों को गिनकर सही संख्या पर गोला लगाएं", "ᱢᱟᱦᱩᱣᱟ ᱡᱚ ᱞᱮᱠᱷᱟ ᱠᱟᱛᱮ ᱮᱞ ᱨᱮ ᱜᱩᱞ ᱢᱮ"),
            NipunCompetency("M1.2", 1, "NUMERACY", "M1.2: Number-Quantity Association (1-99)", "Write numerals for counted objects", "वस्तुओं को गिनें और संख्या बॉक्स में लिखें", "ᱡᱤᱱᱤᱥ ᱠᱚ ᱞᱮᱠᱷᱟᱭ ᱢᱮ ᱟᱨ ᱮᱞ ᱚᱞ ᱢᱮ"),
            NipunCompetency("M1.3", 1, "NUMERACY", "M1.3: Concrete Single-Digit Addition", "Combine two visual sets up to total 9", "दोनों समूहों को जोड़कर कुल संख्या लिखो", "ᱵᱟᱱᱟᱨ ᱜᱟᱫᱮᱞ ᱡᱚᱲᱟᱣ ᱠᱟᱛᱮ ᱮᱞ ᱚᱞ ᱢᱮ"),
            NipunCompetency("M1.4", 1, "NUMERACY", "M1.4: 2D Spatial & Size Comparison", "Identify big/small and circle/square with realia", "बड़ी वस्तु पर गोला लगाओ", "ᱢᱟᱨᱟᱝ ᱡᱤᱱᱤᱥ ᱨᱮ ᱜᱩᱞ ᱢᱮ"),

            // Grade 2
            NipunCompetency("L2.1", 2, "LITERACY", "L2.1: Sight Word Vocabulary Reading", "Read 2-3 letter high-frequency everyday words", "सही शब्द पढ़कर खाली स्थान भरो", "ᱥᱟᱹᱨᱤ ᱟᱹᱲᱟᱹ ᱯᱟᱲᱦᱟᱣ ᱠᱟᱛᱮ ᱯᱮᱨᱮᱡ ᱢᱮ"),
            NipunCompetency("L2.2", 2, "LITERACY", "L2.2: Action Word (Verb) Comprehension", "Identify imperative classroom actions (Sit, Write, Read)", "सही क्रिया शब्द चुनकर खाली जगह भरो", "ᱥᱟᱹᱨᱤ ᱠᱟᱹᱢᱤ ᱟᱹᱲᱟᱹ ᱵᱟᱪᱷᱟᱣ ᱠᱟᱛᱮ ᱯᱮᱨᱮᱡ ᱢᱮ"),
            NipunCompetency("L2.3", 2, "LITERACY", "L2.3: Simple Sentence Reading", "Read 4-5 word sentences with comprehension", "वाक्य पढ़कर सही चित्र से मिलाओ", "ᱟᱭᱟᱛ ᱯᱟᱲᱦᱟᱣ ᱠᱟᱛᱮ ᱥᱟᱹᱨᱤ ᱪᱤᱛᱟᱹᱨ ᱥᱟᱞᱟᱜ ᱡᱚᱲᱟᱣ ᱢᱮ"),
            NipunCompetency("L2.4", 2, "LITERACY", "L2.4: Picture Story Comprehension", "Answer factual questions from short illustrated stories", "कहानी पढ़कर सही उत्तर चुनो", "ᱠᱟᱹᱦᱱᱤ ᱯᱟᱲᱦᱟᱣ ᱠᱟᱛᱮ ᱥᱟᱹᱨᱤ ᱛᱮᱞᱟ ᱵᱟᱪᱷᱟᱣ ᱢᱮ"),
            NipunCompetency("M2.1", 2, "NUMERACY", "M2.1: Concrete Single-Digit Addition", "Combine two visual groups of river fish up to sum 10", "मछलियों को जोड़कर कुल संख्या लिखो", "ᱦᱟᱹᱠᱩ ᱠᱚ ᱡᱚᱲᱟᱣ ᱠᱟᱛᱮ ᱢᱩᱴᱷᱟᱹᱱ ᱞᱮᱠᱷᱟ ᱚᱞ ᱢᱮ"),
            NipunCompetency("M2.2", 2, "NUMERACY", "M2.2: Subtraction by Crossing Out", "Cross out gathered Sal leaves to subtract up to 9", "पत्तियों को काटकर बचे हुए की संख्या लिखो", "ᱥᱟᱠᱟᱢ ᱠᱚ ᱜᱮᱫ ᱠᱟᱛᱮ ᱥᱟᱨᱮᱡ ᱞᱮᱠᱷᱟ ᱚᱞ ᱢᱮ"),
            NipunCompetency("M2.3", 2, "NUMERACY", "M2.3: Two-Digit Place Value (Tens & Ones)", "Bundle objects into tens and units up to 99", "दहाई और इकाई अलग करके संख्या लिखो", "ᱜᱮᱞᱟᱝ ᱟᱨ ᱢᱤᱫᱟᱝ ᱦᱟᱹᱴᱤᱧ ᱠᱟᱛᱮ ᱮᱞ ᱚᱞ ᱢᱮ"),
            NipunCompetency("M2.4", 2, "NUMERACY", "M2.4: Addition using Indigenous Forest Produce", "Addition with indigenous forest produce & realia up to 20", "जंगल की वस्तुओं को जोड़कर संख्या लिखें", "ᱵᱤᱨ ᱨᱮᱱᱟᱜ ᱡᱚ ᱡᱚᱲᱟᱣ ᱠᱟᱛᱮ ᱮᱞ ᱚᱞ ᱢᱮ"),

            // Grade 3
            NipunCompetency("L3.1", 3, "LITERACY", "L3.1: Fluent Passage Reading", "Read age-appropriate stories with comprehension", "अनुच्छेद को पढ़कर नीचे दिए गए प्रश्नों के उत्तर दें", "ᱯᱟᱨᱟᱥ ᱯᱟᱲᱦᱟᱣ ᱠᱟᱛᱮ ᱠᱩᱠᱞᱤ ᱨᱮᱱᱟᱜ ᱛᱮᱞᱟ ᱮᱢ ᱢᱮ"),
            NipunCompetency("L3.2", 3, "LITERACY", "L3.2: Dual-Script Sentence Construction", "Arrange tribal words into Subject-Object-Verb order", "शब्दों को सही क्रम में लगाकर वाक्य बनाओ", "ᱟᱹᱲᱟᱹ ᱠᱚ ᱥᱟᱹᱨᱤ ᱛᱷᱟᱨ ᱨᱮ ᱥᱟᱡᱟᱣ ᱠᱟᱛᱮ ᱟᱭᱟᱛ ᱵᱮᱱᱟᱣ ᱢᱮ"),
            NipunCompetency("L3.3", 3, "LITERACY", "L3.3: Dialogue & Expression Reading", "Differentiate questions and exclamations in reading", "उचित भाव के साथ संवाद को पहचानो", "ᱥᱟᱹᱨᱤ ᱵᱷᱟᱵᱽ ᱥᱟᱞᱟᱜ ᱨᱚᱯᱚᱲ ᱪᱤᱱᱦᱟᱹᱣ ᱢᱮ"),
            NipunCompetency("L3.4", 3, "LITERACY", "L3.4: Bilingual Paragraph Verification", "Verify translated sentences against Hindi meaning", "हिंदी अर्थ देखकर सही संथाली वाक्य चुनें", "ᱦᱤᱱᱫᱤ ᱢᱮᱱᱮᱛ ᱧᱮᱞ ᱠᱟᱛᱮ ᱥᱟᱹᱨᱤ ᱥᱟᱱᱛᱟᱲᱤ ᱟᱭᱟᱛ ᱵᱟᱪᱷᱟᱣ ᱢᱮ"),
            NipunCompetency("M3.1", 3, "NUMERACY", "M3.1: Two-Digit Addition in Daily Life", "Solve village Haat/Bazaar buying scenarios up to 99", "बाजार के हिसाब को जोड़कर कुल खर्च निकालो", "ᱦᱟᱴ ᱨᱮᱱᱟᱜ ᱞᱮᱠᱷᱟ ᱡᱚᱲᱟᱣ ᱠᱟᱛᱮ ᱢᱩᱴᱷᱟᱹᱱ ᱠᱷᱚᱨᱪᱟ ᱚᱞ ᱢᱮ"),
            NipunCompetency("M3.2", 3, "NUMERACY", "M3.2: Three-Digit Number Sense (up to 999)", "Identify hundreds, tens, and ones in 3-digit figures", "सैकड़ा, दहाई और इकाई पहचान कर संख्या लिखें", "ᱥᱟᱭ, ᱜᱮᱞ ᱟᱨ ᱢᱤᱫ ᱪᱤᱱᱦᱟᱹᱣ ᱠᱟᱛᱮ ᱮᱞ ᱚᱞ ᱢᱮ"),
            NipunCompetency("M3.3", 3, "NUMERACY", "M3.3: Equal Grouping & Repeated Addition", "Group Sal leaves into equal bundles for multiplication", "बराबर समूहों को गिनो और कुल संख्या लिखो", "ᱥᱚᱢᱟᱱ ᱜᱟᱫᱮᱞ ᱠᱚ ᱞᱮᱠᱷᱟᱭ ᱢᱮ ᱟᱨ ᱮᱞ ᱚᱞ ᱢᱮ"),
            NipunCompetency("M3.4", 3, "NUMERACY", "M3.4: Simple Measurement with Non-Standard Units", "Compare lengths and weights using local village references", "सही माप पहचानकर मिलान करो", "ᱥᱟᱹᱨᱤ ᱡᱚᱠᱷᱟ ᱪᱤᱱᱦᱟᱹᱣ ᱠᱟᱛᱮ ᱡᱚᱲᱟᱣ ᱢᱮ")
        )
    }
}
