package com.example.palashsetu.data.local

import android.content.Context
import android.util.Log
import com.example.palashsetu.data.model.NipunCompetency
import org.json.JSONArray

/**
 * Repository for the 24 Standard NIPUN Bharat Foundational Competency Outcomes (Grades 1–3).
 *
 * Populated from `assets/schemas/nipun_curriculum_registry.json` with embedded bilingual fallback.
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
     * Filters competencies by domain ("LITERACY" or "NUMERACY").
     */
    fun getCompetenciesForDomain(context: Context? = null, domain: String): List<NipunCompetency> {
        val all = getAllCompetencies(context)
        return all.filter { it.domain.equals(domain, ignoreCase = true) }
    }

    /**
     * Multi-criteria search and filter across all 24 competencies.
     * Supports filtering by grade, domain, and searching across code, Hindi, and English text.
     */
    fun filterCompetencies(
        context: Context? = null,
        grade: Int? = null,
        domain: String? = null,
        searchQuery: String = ""
    ): List<NipunCompetency> {
        val all = getAllCompetencies(context)
        val trimmedQuery = searchQuery.trim().lowercase()

        return all.filter { comp ->
            val matchGrade = grade == null || comp.grade == grade
            val matchDomain = domain == null || comp.domain.equals(domain, ignoreCase = true)
            val matchQuery = trimmedQuery.isEmpty() ||
                comp.code.lowercase().contains(trimmedQuery) ||
                comp.titleEn.lowercase().contains(trimmedQuery) ||
                comp.titleHi.lowercase().contains(trimmedQuery) ||
                comp.subtitleEn.lowercase().contains(trimmedQuery) ||
                comp.subtitleHi.lowercase().contains(trimmedQuery) ||
                comp.instructionHi.lowercase().contains(trimmedQuery) ||
                comp.instructionOlchiki.lowercase().contains(trimmedQuery)

            matchGrade && matchDomain && matchQuery
        }
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
                        titleHi = obj.optString("title_hi", ""),
                        titleEn = obj.optString("title_en", obj.optString("title", "")),
                        subtitleHi = obj.optString("subtitle_hi", ""),
                        subtitleEn = obj.optString("subtitle_en", obj.optString("subtitle", "")),
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

    internal fun getEmbeddedFallbackList(): List<NipunCompetency> {
        return listOf(
            // Grade 1 (8 Competencies: 4 Literacy + 4 Numeracy)
            NipunCompetency(
                code = "L1.1", grade = 1, domain = "LITERACY",
                title = "L1.1: Native Script & Alphabet Recognition",
                titleEn = "L1.1: Native Script & Alphabet Recognition",
                titleHi = "L1.1: मूल लिपि व वर्णमाला पहचान",
                subtitle = "Identify base consonants and vowels in Ol Chiki and Devanagari",
                subtitleEn = "Identify base consonants and vowels in Ol Chiki and Devanagari",
                subtitleHi = "ओल चिकी और देवनागरी में मूल व्यंजन और स्वर पहचानें",
                instructionHi = "अक्षर पहचान कर गोला लगाओ",
                instructionOlchiki = "ᱟᱠᱷᱚᱨ ᱪᱤᱱᱦᱟᱹᱣ ᱠᱟᱛᱮ ᱜᱩᱞ ᱢᱮ",
                motifAsset = "images/motifs/motif_slate_pencil.png", templateType = "CIRCLE_GLYPH"
            ),
            NipunCompetency(
                code = "L1.2", grade = 1, domain = "LITERACY",
                title = "L1.2: Phonemic Awareness & Sound Blending",
                titleEn = "L1.2: Phonemic Awareness & Sound Blending",
                titleHi = "L1.2: ध्वन्यात्मक जागरूकता व ध्वनि संयोजन",
                subtitle = "Blend individual phonetic sounds into simple words",
                subtitleEn = "Blend individual phonetic sounds into simple words",
                subtitleHi = "ध्वनियों को जोड़कर सरल 2-3 अक्षरों के शब्द बनाएं",
                instructionHi = "ध्वनियों को जोड़कर सही शब्द बनाओ",
                instructionOlchiki = "ᱥᱟᱰᱮ ᱠᱚ ᱡᱚᱲᱟᱣ ᱠᱟᱛᱮ ᱟᱹᱲᱟᱹ ᱵᱮᱱᱟᱣ ᱢᱮ",
                motifAsset = "images/motifs/motif_drum_tamak.png", templateType = "FLASHCARD_DRILL"
            ),
            NipunCompetency(
                code = "L1.3", grade = 1, domain = "LITERACY",
                title = "L1.3: Object-to-Word Bilingual Vocabulary",
                titleEn = "L1.3: Object-to-Word Bilingual Vocabulary",
                titleHi = "L1.3: वस्तु से शब्द द्विभाषी शब्दावली",
                subtitle = "Match indigenous rural objects with their names",
                subtitleEn = "Match indigenous rural objects with their names",
                subtitleHi = "परिवेशीय वस्तुओं को उनके प्रामाणिक नामों से मिलाएं",
                instructionHi = "चित्र देखकर सही नाम से मिलाओ",
                instructionOlchiki = "ᱪᱤᱛᱟᱹᱨ ᱧᱮᱞ ᱠᱟᱛᱮ ᱥᱟᱹᱨᱤ ᱧᱩᱛᱩᱢ ᱥᱟᱞᱟᱜ ᱡᱚᱲᱟᱣ ᱢᱮ",
                motifAsset = "images/motifs/motif_mango_fruit.png", templateType = "MATCH_THE_COLUMNS"
            ),
            NipunCompetency(
                code = "L1.4", grade = 1, domain = "LITERACY",
                title = "L1.4: Print Concepts & Letter Tracking",
                titleEn = "L1.4: Print Concepts & Letter Tracking",
                titleHi = "L1.4: मुद्रण अवधारणाएं व पठन दिशा",
                subtitle = "Track text direction and identify word spaces",
                subtitleEn = "Track text direction and identify word spaces",
                subtitleHi = "बाएं से दाएं पठन क्रम और शब्दों के बीच स्थान पहचानें",
                instructionHi = "शब्दों के बीच सही खाली जगह पहचानो",
                instructionOlchiki = "ᱟᱹᱲᱟᱹ ᱠᱚ ᱛᱟᱞᱟ ᱨᱮ ᱯᱷᱟᱸᱠ ᱪᱤᱱᱦᱟᱹᱣ ᱢᱮ",
                motifAsset = "images/motifs/motif_slate_pencil.png", templateType = "READING_PASSAGE"
            ),
            NipunCompetency(
                code = "M1.1", grade = 1, domain = "NUMERACY",
                title = "M1.1: Pictorial Counting up to 9",
                titleEn = "M1.1: Pictorial Counting up to 9",
                titleHi = "M1.1: ठोस चित्रात्मक गणना (1–9)",
                subtitle = "Count concrete village items (Mahua seeds & Sal leaves)",
                subtitleEn = "Count concrete village items (Mahua seeds & Sal leaves)",
                subtitleHi = "महुआ बीज और सखुआ पत्तों जैसे ठोस वस्तुओं को गिनें",
                instructionHi = "महुआ के फलों को गिनकर सही संख्या पर गोला लगाएं",
                instructionOlchiki = "ᱢᱟᱦᱩᱣᱟ ᱡᱚ ᱞᱮᱠᱷᱟ ᱠᱟᱛᱮ ᱮᱞ ᱨᱮ ᱜᱩᱞ ᱢᱮ",
                motifAsset = "images/motifs/motif_mahua_fruit.png", templateType = "COUNT_AND_CIRCLE"
            ),
            NipunCompetency(
                code = "M1.2", grade = 1, domain = "NUMERACY",
                title = "M1.2: Number-Quantity Association (1-99)",
                titleEn = "M1.2: Number-Quantity Association (1-99)",
                titleHi = "M1.2: संख्या-मात्रा संबंध (1–99)",
                subtitle = "Write numerals for counted objects",
                subtitleEn = "Write numerals for counted objects",
                subtitleHi = "गिनी गई वस्तुओं के अनुसार संख्या बॉक्स में लिखें",
                instructionHi = "वस्तुओं को गिनें और संख्या बॉक्स में लिखें",
                instructionOlchiki = "ᱡᱤᱱᱤᱥ ᱠᱚ ᱞᱮᱠᱷᱟᱭ ᱢᱮ ᱟᱨ ᱮᱞ ᱚᱞ ᱢᱮ",
                motifAsset = "images/motifs/motif_earthen_pot.png", templateType = "COUNT_AND_WRITE"
            ),
            NipunCompetency(
                code = "M1.3", grade = 1, domain = "NUMERACY",
                title = "M1.3: Concrete Single-Digit Addition",
                titleEn = "M1.3: Concrete Single-Digit Addition",
                titleHi = "M1.3: ठोस एकल-अंक जोड़ (9 तक)",
                subtitle = "Combine two visual sets up to total 9",
                subtitleEn = "Combine two visual sets up to total 9",
                subtitleHi = "दो दृश्य समूहों को जोड़कर कुल 9 तक योग निकालें",
                instructionHi = "दोनों समूहों को जोड़कर कुल संख्या लिखो",
                instructionOlchiki = "ᱵᱟᱱᱟᱨ ᱜᱟᱫᱮᱞ ᱡᱚᱲᱟᱣ ᱠᱟᱛᱮ ᱮᱞ ᱚᱞ ᱢᱮ",
                motifAsset = "images/motifs/motif_sal_leaf.png", templateType = "OBJECT_ADDITION"
            ),
            NipunCompetency(
                code = "M1.4", grade = 1, domain = "NUMERACY",
                title = "M1.4: 2D Spatial & Size Comparison",
                titleEn = "M1.4: 2D Spatial & Size Comparison",
                titleHi = "M1.4: स्थानिक समझ व द्विविमीय आकृतियां",
                subtitle = "Identify big/small and circle/square with realia",
                subtitleEn = "Identify big/small and circle/square with realia",
                subtitleHi = "गोल, चौकोर आकृतियां और बड़ा/छोटा पहचानें",
                instructionHi = "बड़ी वस्तु पर गोला लगाओ",
                instructionOlchiki = "ᱢᱟᱨᱟᱝ ᱡᱤᱱᱤᱥ ᱨᱮ ᱜᱩᱞ ᱢᱮ",
                motifAsset = "images/motifs/motif_earthen_pot.png", templateType = "SHAPE_COMPARISON"
            ),

            // Grade 2 (8 Competencies: 4 Literacy + 4 Numeracy)
            NipunCompetency(
                code = "L2.1", grade = 2, domain = "LITERACY",
                title = "L2.1: Sight Word Vocabulary Reading",
                titleEn = "L2.1: Sight Word Vocabulary Reading",
                titleHi = "L2.1: दृष्टि शब्द शब्दावली पठन",
                subtitle = "Read 2-3 letter high-frequency everyday words",
                subtitleEn = "Read 2-3 letter high-frequency everyday words",
                subtitleHi = "दैनिक जीवन के 2-3 अक्षरीय शब्दों को शुद्धता से पढ़ें",
                instructionHi = "सही शब्द पढ़कर खाली स्थान भरो",
                instructionOlchiki = "ᱥᱟᱹᱨᱤ ᱟᱹᱲᱟᱹ ᱯᱟᱲᱦᱟᱣ ᱠᱟᱛᱮ ᱯᱮᱨᱮᱡ ᱢᱮ",
                motifAsset = "images/motifs/motif_slate_pencil.png", templateType = "FILL_IN_BLANK"
            ),
            NipunCompetency(
                code = "L2.2", grade = 2, domain = "LITERACY",
                title = "L2.2: Action Word (Verb) Comprehension",
                titleEn = "L2.2: Action Word (Verb) Comprehension",
                titleHi = "L2.2: क्रिया शब्द व आज्ञावाचक समझ",
                subtitle = "Identify imperative classroom actions (Sit, Write, Read)",
                subtitleEn = "Identify imperative classroom actions (Sit, Write, Read)",
                subtitleHi = "कक्षा के सामान्य क्रिया निर्देशों (बैठो, लिखो, पढ़ो) को समझें",
                instructionHi = "सही क्रिया शब्द चुनकर खाली जगह भरो",
                instructionOlchiki = "ᱥᱟᱹᱨᱤ ᱠᱟᱹᱢᱤ ᱟᱹᱲᱟᱹ ᱵᱟᱪᱷᱟᱣ ᱠᱟᱛᱮ ᱯᱮᱨᱮᱡ ᱢᱮ",
                motifAsset = "images/motifs/motif_slate_pencil.png", templateType = "FILL_IN_BLANK"
            ),
            NipunCompetency(
                code = "L2.3", grade = 2, domain = "LITERACY",
                title = "L2.3: Simple Sentence Reading",
                titleEn = "L2.3: Simple Sentence Reading",
                titleHi = "L2.3: सरल वाक्य पठन",
                subtitle = "Read 4-5 word sentences with comprehension",
                subtitleEn = "Read 4-5 word sentences with comprehension",
                subtitleHi = "4-5 शब्दों के सरल वाक्यों को समझ के साथ पढ़ें",
                instructionHi = "वाक्य पढ़कर सही चित्र से मिलाओ",
                instructionOlchiki = "ᱟᱭᱟᱛ ᱯᱟᱲᱦᱟᱣ ᱠᱟᱛᱮ ᱥᱟᱹᱨᱤ ᱪᱤᱛᱟᱹᱨ ᱥᱟᱞᱟᱜ ᱡᱚᱲᱟᱣ ᱢᱮ",
                motifAsset = "images/motifs/motif_mango_fruit.png", templateType = "MATCH_THE_COLUMNS"
            ),
            NipunCompetency(
                code = "L2.4", grade = 2, domain = "LITERACY",
                title = "L2.4: Picture Story Comprehension",
                titleEn = "L2.4: Picture Story Comprehension",
                titleHi = "L2.4: चित्र कहानी बोध",
                subtitle = "Answer factual questions from short illustrated stories",
                subtitleEn = "Answer factual questions from short illustrated stories",
                subtitleHi = "सचित्र लघु कहानी पढ़कर सीधे प्रश्नों के उत्तर दें",
                instructionHi = "कहानी पढ़कर सही उत्तर चुनो",
                instructionOlchiki = "ᱠᱟᱹᱦᱱᱤ ᱯᱟᱲᱦᱟᱣ ᱠᱟᱛᱮ ᱥᱟᱹᱨᱤ ᱛᱮᱞᱟ ᱵᱟᱪᱷᱟᱣ ᱢᱮ",
                motifAsset = "images/motifs/motif_bow_arrow.png", templateType = "READING_PASSAGE"
            ),
            NipunCompetency(
                code = "M2.1", grade = 2, domain = "NUMERACY",
                title = "M2.1: Concrete Single-Digit Addition",
                titleEn = "M2.1: Concrete Single-Digit Addition",
                titleHi = "M2.1: ठोस एकल-अंक जोड़ (20 तक)",
                subtitle = "Combine two visual groups of river fish up to sum 10",
                subtitleEn = "Combine two visual groups of river fish up to sum 10",
                subtitleHi = "नदी की मछलियों व वस्तुओं को जोड़कर 10 तक योग निकालें",
                instructionHi = "मछलियों को जोड़कर कुल संख्या लिखो",
                instructionOlchiki = "ᱦᱟᱹᱠᱩ ᱠᱚ ᱡᱚᱲᱟᱣ ᱠᱟᱛᱮ ᱢᱩᱴᱷᱟᱹᱱ ᱞᱮᱠᱷᱟ ᱚᱞ ᱢᱮ",
                motifAsset = "images/motifs/motif_indigenous_fish.png", templateType = "OBJECT_ADDITION"
            ),
            NipunCompetency(
                code = "M2.2", grade = 2, domain = "NUMERACY",
                title = "M2.2: Subtraction by Crossing Out",
                titleEn = "M2.2: Subtraction by Crossing Out",
                titleHi = "M2.2: काटकर घटाव की समझ",
                subtitle = "Cross out gathered Sal leaves to subtract up to 9",
                subtitleEn = "Cross out gathered Sal leaves to subtract up to 9",
                subtitleHi = "एकत्रित सखुआ पत्तों को काटकर 9 तक घटाव करें",
                instructionHi = "पत्तियों को काटकर बचे हुए की संख्या लिखो",
                instructionOlchiki = "ᱥᱟᱠᱟᱢ ᱠᱚ ᱜᱮᱫ ᱠᱟᱛᱮ ᱥᱟᱨᱮᱡ ᱞᱮᱠᱷᱟ ᱚᱞ ᱢᱮ",
                motifAsset = "images/motifs/motif_sal_leaf.png", templateType = "CROSS_OUT_SUBTRACTION"
            ),
            NipunCompetency(
                code = "M2.3", grade = 2, domain = "NUMERACY",
                title = "M2.3: Two-Digit Place Value (Tens & Ones)",
                titleEn = "M2.3: Two-Digit Place Value (Tens & Ones)",
                titleHi = "M2.3: दो-अंकीय स्थानीय मान (दहाई व इकाई)",
                subtitle = "Bundle objects into tens and units up to 99",
                subtitleEn = "Bundle objects into tens and units up to 99",
                subtitleHi = "वस्तुओं को दहाई और इकाई में बांटकर 99 तक समझें",
                instructionHi = "दहाई और इकाई अलग करके संख्या लिखो",
                instructionOlchiki = "ᱜᱮᱞᱟᱝ ᱟᱨ ᱢᱤᱫᱟᱝ ᱦᱟᱹᱴᱤᱧ ᱠᱟᱛᱮ ᱮᱞ ᱚᱞ ᱢᱮ",
                motifAsset = "images/motifs/motif_mahua_fruit.png", templateType = "COUNT_AND_WRITE"
            ),
            NipunCompetency(
                code = "M2.4", grade = 2, domain = "NUMERACY",
                title = "M2.4: Addition using Indigenous Forest Produce",
                titleEn = "M2.4: Addition using Indigenous Forest Produce",
                titleHi = "M2.4: वनोपज व प्राकृतिक वस्तुओं से जोड़",
                subtitle = "Addition with indigenous forest produce & realia up to 20",
                subtitleEn = "Addition with indigenous forest produce & realia up to 20",
                subtitleHi = "जंगल की वस्तुओं (महुआ, सखुआ) को जोड़कर 20 तक संख्या लिखें",
                instructionHi = "जंगल की वस्तुओं को जोड़कर संख्या लिखें",
                instructionOlchiki = "ᱵᱤᱨ ᱨᱮᱱᱟᱜ ᱡᱚ ᱡᱚᱲᱟᱣ ᱠᱟᱛᱮ ᱮᱞ ᱚᱞ ᱢᱮ",
                motifAsset = "images/motifs/motif_mahua_fruit.png", templateType = "OBJECT_ADDITION"
            ),

            // Grade 3 (8 Competencies: 4 Literacy + 4 Numeracy)
            NipunCompetency(
                code = "L3.1", grade = 3, domain = "LITERACY",
                title = "L3.1: Fluent Passage Reading",
                titleEn = "L3.1: Fluent Passage Reading",
                titleHi = "L3.1: प्रवाहपूर्ण गद्यांश पठन",
                subtitle = "Read age-appropriate stories with comprehension",
                subtitleEn = "Read age-appropriate stories with comprehension",
                subtitleHi = "उम्र-अनुकूल कहानियों को समझ के साथ धाराप्रवाह पढ़ें",
                instructionHi = "अनुच्छेद को पढ़कर नीचे दिए गए प्रश्नों के उत्तर दें",
                instructionOlchiki = "ᱯᱟᱨᱟᱥ ᱯᱟᱲᱦᱟᱣ ᱠᱟᱛᱮ ᱠᱩᱠᱞᱤ ᱨᱮᱱᱟᱜ ᱛᱮᱞᱟ ᱮᱢ ᱢᱮ",
                motifAsset = "images/motifs/motif_bow_arrow.png", templateType = "READING_PASSAGE"
            ),
            NipunCompetency(
                code = "L3.2", grade = 3, domain = "LITERACY",
                title = "L3.2: Dual-Script Sentence Construction",
                titleEn = "L3.2: Dual-Script Sentence Construction",
                titleHi = "L3.2: द्वि-लिपि वाक्य संरचना",
                subtitle = "Arrange tribal words into Subject-Object-Verb order",
                subtitleEn = "Arrange tribal words into Subject-Object-Verb order",
                subtitleHi = "जनजातीय शब्दों को कर्ता-कर्म-क्रिया क्रम में सजाएं",
                instructionHi = "शब्दों को सही क्रम में लगाकर वाक्य बनाओ",
                instructionOlchiki = "ᱟᱹᱲᱟᱹ ᱠᱚ ᱥᱟᱹᱨᱤ ᱛᱷᱟᱨ ᱨᱮ ᱥᱟᱡᱟᱣ ᱠᱟᱛᱮ ᱟᱭᱟᱛ ᱵᱮᱱᱟᱣ ᱢᱮ",
                motifAsset = "images/motifs/motif_slate_pencil.png", templateType = "SENTENCE_SCRAMBLE"
            ),
            NipunCompetency(
                code = "L3.3", grade = 3, domain = "LITERACY",
                title = "L3.3: Dialogue & Expression Reading",
                titleEn = "L3.3: Dialogue & Expression Reading",
                titleHi = "L3.3: संवाद व भावपूर्ण पठन",
                subtitle = "Differentiate questions and exclamations in reading",
                subtitleEn = "Differentiate questions and exclamations in reading",
                subtitleHi = "प्रश्न और विस्मयबोधक भावों के साथ संवाद पहचानें",
                instructionHi = "उचित भाव के साथ संवाद को पहचानो",
                instructionOlchiki = "ᱥᱟᱹᱨᱤ ᱵᱷᱟᱵᱽ ᱥᱟᱞᱟᱜ ᱨᱚᱯᱚᱲ ᱪᱤᱱᱦᱟᱹᱣ ᱢᱮ",
                motifAsset = "images/motifs/motif_drum_tamak.png", templateType = "FILL_IN_BLANK"
            ),
            NipunCompetency(
                code = "L3.4", grade = 3, domain = "LITERACY",
                title = "L3.4: Bilingual Paragraph Verification",
                titleEn = "L3.4: Bilingual Paragraph Verification",
                titleHi = "L3.4: द्विभाषी अनुच्छेद सत्यापन",
                subtitle = "Verify translated sentences against Hindi meaning",
                subtitleEn = "Verify translated sentences against Hindi meaning",
                subtitleHi = "हिंदी अर्थ देखकर संथाली अनुवाद का सत्यापन करें",
                instructionHi = "हिंदी अर्थ देखकर सही संथाली वाक्य चुनें",
                instructionOlchiki = "ᱦᱤᱱᱫᱤ ᱢᱮᱱᱮᱛ ᱧᱮᱞ ᱠᱟᱛᱮ ᱥᱟᱹᱨᱤ ᱥᱟᱱᱛᱟᱲᱤ ᱟᱭᱟᱛ ᱵᱟᱪᱷᱟᱣ ᱢᱮ",
                motifAsset = "images/motifs/motif_slate_pencil.png", templateType = "MATCH_THE_COLUMNS"
            ),
            NipunCompetency(
                code = "M3.1", grade = 3, domain = "NUMERACY",
                title = "M3.1: Two-Digit Addition in Daily Life",
                titleEn = "M3.1: Two-Digit Addition in Daily Life",
                titleHi = "M3.1: दैनिक जीवन में दो-अंकीय जोड़",
                subtitle = "Solve village Haat/Bazaar buying scenarios up to 99",
                subtitleEn = "Solve village Haat/Bazaar buying scenarios up to 99",
                subtitleHi = "हाट-बाजार के क्रय-विक्रय व दैनिक लेन-देन हल करें",
                instructionHi = "बाजार के हिसाब को जोड़कर कुल खर्च निकालो",
                instructionOlchiki = "ᱦᱟᱴ ᱨᱮᱱᱟᱜ ᱞᱮᱠᱷᱟ ᱡᱚᱲᱟᱣ ᱠᱟᱛᱮ ᱢᱩᱴᱷᱟᱹᱱ ᱠᱷᱚᱨᱪᱟ ᱚᱞ ᱢᱮ",
                motifAsset = "images/motifs/motif_earthen_pot.png", templateType = "WORD_PROBLEM"
            ),
            NipunCompetency(
                code = "M3.2", grade = 3, domain = "NUMERACY",
                title = "M3.2: Three-Digit Number Sense (up to 999)",
                titleEn = "M3.2: Three-Digit Number Sense (up to 999)",
                titleHi = "M3.2: तीन-अंकीय संख्या बोध (999 तक)",
                subtitle = "Identify hundreds, tens, and ones in 3-digit figures",
                subtitleEn = "Identify hundreds, tens, and ones in 3-digit figures",
                subtitleHi = "सैकड़ा, दहाई और इकाई पहचान कर 999 तक तुलना करें",
                instructionHi = "सैकड़ा, दहाई और इकाई पहचान कर संख्या लिखें",
                instructionOlchiki = "ᱥᱟᱭ, ᱜᱮᱞ ᱟᱨ ᱢᱤᱫ ᱪᱤᱱᱦᱟᱹᱣ ᱠᱟᱛᱮ ᱮᱞ ᱚᱞ ᱢᱮ",
                motifAsset = "images/motifs/motif_earthen_pot.png", templateType = "COUNT_AND_WRITE"
            ),
            NipunCompetency(
                code = "M3.3", grade = 3, domain = "NUMERACY",
                title = "M3.3: Equal Grouping & Repeated Addition",
                titleEn = "M3.3: Equal Grouping & Repeated Addition",
                titleHi = "M3.3: समान समूहीकरण व बार-बार जोड़",
                subtitle = "Group Sal leaves into equal bundles for multiplication",
                subtitleEn = "Group Sal leaves into equal bundles for multiplication",
                subtitleHi = "सखुआ पत्तों के समान गट्ठर बनाकर गुणा की नींव समझें",
                instructionHi = "बराबर समूहों को गिनो और कुल संख्या लिखो",
                instructionOlchiki = "ᱥᱚᱢᱟᱱ ᱜᱟᱫᱮᱞ ᱠᱚ ᱞᱮᱠᱷᱟᱭ ᱢᱮ ᱟᱨ ᱮᱞ ᱚᱞ ᱢᱮ",
                motifAsset = "images/motifs/motif_sal_leaf.png", templateType = "GROUPING_GRID"
            ),
            NipunCompetency(
                code = "M3.4", grade = 3, domain = "NUMERACY",
                title = "M3.4: Simple Measurement with Non-Standard Units",
                titleEn = "M3.4: Simple Measurement with Non-Standard Units",
                titleHi = "M3.4: अमानक इकाइयों से सरल मापन",
                subtitle = "Compare lengths and weights using local village references",
                subtitleEn = "Compare lengths and weights using local village references",
                subtitleHi = "स्थानीय परिवेशीय संदर्भों से लंबाई व वजन की तुलना करें",
                instructionHi = "सही माप पहचानकर मिलान करो",
                instructionOlchiki = "ᱥᱟᱹᱨᱤ ᱡᱚᱠᱷᱟ ᱪᱤᱱᱦᱟᱹᱣ ᱠᱟᱛᱮ ᱡᱚᱲᱟᱣ ᱢᱮ",
                motifAsset = "images/motifs/motif_slate_pencil.png", templateType = "MATCH_THE_COLUMNS"
            )
        )
    }
}
