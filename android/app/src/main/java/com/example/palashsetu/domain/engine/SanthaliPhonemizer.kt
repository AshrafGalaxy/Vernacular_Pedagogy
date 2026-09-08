package com.example.palashsetu.domain.engine

import org.json.JSONObject

/**
 * Santhali (sat_Olck) Ol Chiki to IPA Grapheme-to-Phoneme (G2P) Converter
 * and Piper VITS Tokenizer.
 *
 * Implements canonical linguistic rules:
 * 1. Base vowels (ᱚ, ᱟ, ᱤ, ᱩ, ᱮ, ᱳ)
 * 2. Low vowels (Gahla Tundag ᱹ)
 * 3. Nasalization (Mu Tundag ᱸ)
 * 4. Combined Nasal-Low (Mu-Gahla Tundag ᱺ)
 * 5. Vowel Prolongation (Relah ᱻ)
 * 6. Deglottalization (Ohod ᱽ)
 * 7. Aspiration (Oh ᱷ)
 * 8. Numerals (᱐-᱙ -> Santhali number words)
 */
object SanthaliPhonemizer {

    private val OL_CHIKI_NUMERALS = mapOf(
        "᱐" to "ᱥᱩᱱ",      // sun (zero)
        "᱑" to "ᱢᱤᱫ",      // mid (one)
        "᱒" to "ᱵᱟᱨ",      // bar (two)
        "᱓" to "ᱯᱮ",       // pe (three)
        "᱔" to "ᱯᱳᱱ",      // pon (four)
        "᱕" to "ᱢᱚᱬᱮ",     // mone (five)
        "᱖" to "ᱛᱩᱨᱩᱭ",    // turui (six)
        "᱗" to "ᱮᱭᱟᱭ",     // eyae (seven)
        "᱘" to "ᱤᱨᱟᱹᱞ",    // irəl (eight)
        "᱙" to "ᱟᱨᱮ"       // are (nine)
    )

    private val COMPOUND_MAP = listOf(
        // Deglottalized plosives with Ohod (ᱽ)
        "ᱜᱽ" to "ɡ",
        "ᱡᱽ" to "ɟ",
        "ᱫᱽ" to "d",
        "ᱵᱽ" to "b",

        // Aspirated plosives with Oh (ᱷ)
        "ᱛᱷ" to "tʰ",
        "ᱠᱷ" to "kʰ",
        "ᱪᱷ" to "cʰ",
        "ᱯᱷ" to "pʰ",
        "ᱴᱷ" to "ʈʰ",
        "ᱫᱷ" to "dʰ",
        "ᱜᱷ" to "ɡʰ",
        "ᱡᱷ" to "ɟʰ",
        "ᱵᱷ" to "bʰ",
        "ᱰᱷ" to "ɖʰ",

        // Vowels with Mu-Gahla Tundag (ᱺ) - Nasal + Low
        "ᱚᱺ" to "ə̃",
        "ᱟᱺ" to "ə̃",
        "ᱮᱺ" to "ɛ̃",

        // Vowels with Gahla Tundag (ᱹ) - Low/Centralized
        "ᱚᱹ" to "ə",
        "ᱟᱹ" to "ə",
        "ᱮᱹ" to "ɛ",
        "ᱩᱹ" to "ʊ",
        "ᱤᱹ" to "ɪ",
        "ᱳᱹ" to "ɔ",

        // Vowels with Mu Tundag (ᱸ) - Nasalization
        "ᱚᱸ" to "ɔ̃",
        "ᱟᱸ" to "ã",
        "ᱤᱸ" to "ĩ",
        "ᱩᱸ" to "ũ",
        "ᱮᱸ" to "ẽ",
        "ᱳᱸ" to "õ",

        // Vowels with Relah (ᱻ) - Lengthening
        "ᱚᱻ" to "ɔː",
        "ᱟᱻ" to "aː",
        "ᱤᱻ" to "iː",
        "ᱩᱻ" to "uː",
        "ᱮᱻ" to "eː",
        "ᱳᱻ" to "oː"
    )

    private val SINGLE_CHAR_MAP = mapOf(
        // 6 Base Vowels
        "ᱚ" to "ɔ",
        "ᱟ" to "a",
        "ᱤ" to "i",
        "ᱩ" to "u",
        "ᱮ" to "e",
        "ᱳ" to "o",

        // Consonants - Plosives & Affricates
        "ᱛ" to "t",
        "ᱠ" to "k",
        "ᱪ" to "c",
        "ᱯ" to "p",
        "ᱴ" to "ʈ",
        "ᱰ" to "ɖ",

        // Consonants - Checked/Voiced Stops
        "ᱜ" to "ɡ",
        "ᱡ" to "ɟ",
        "ᱫ" to "d",
        "ᱵ" to "b",

        // Nasals
        "ᱝ" to "ŋ",
        "ᱢ" to "m",
        "ᱧ" to "ɲ",
        "ᱬ" to "ɳ",
        "ᱱ" to "n",
        "ᱶ" to "w̃",

        // Liquids, Fricatives & Glides
        "ᱞ" to "l",
        "ᱣ" to "w",
        "ᱥ" to "s",
        "ᱦ" to "h",
        "ᱨ" to "r",
        "ᱭ" to "j",
        "ᱲ" to "ɽ",
        "ᱷ" to "ʰ",

        // Diacritics and separators
        "ᱸ" to "̃",
        "ᱹ" to "ə",
        "ᱺ" to "ə̃",
        "ᱻ" to "ː",
        "ᱼ" to "ʔ",
        "ᱽ" to "",

        // Punctuation
        "᱾" to ".",
        "᱿" to "."
    )

    /**
     * Converts Ol Chiki text into IPA string.
     */
    fun santhaliToIpa(rawText: String): String {
        if (rawText.isBlank()) return ""
        var text = rawText.trim()

        // Replace Ol Chiki digits with Santhali number words
        for ((digit, word) in OL_CHIKI_NUMERALS) {
            text = text.replace(digit, " $word ")
        }

        // Replace Multi-character compounds
        for ((pattern, replacement) in COMPOUND_MAP) {
            text = text.replace(pattern, replacement)
        }

        // Replace single Ol Chiki characters
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            val charCount = Character.charCount(codePoint)
            val ch = text.substring(i, i + charCount)
            if (SINGLE_CHAR_MAP.containsKey(ch)) {
                sb.append(SINGLE_CHAR_MAP[ch])
            } else {
                sb.append(ch)
            }
            i += charCount
        }

        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Tokenizes Ol Chiki text into Piper VITS input IDs using phoneme_id_map from config JSON.
     */
    fun textToPhonemeIds(
        text: String,
        phonemeIdMap: Map<String, List<Long>>
    ): LongArray {
        val bos = phonemeIdMap["^"]?.firstOrNull() ?: 1L
        val eos = phonemeIdMap["$"]?.firstOrNull() ?: 2L
        val pad = phonemeIdMap["_"]?.firstOrNull() ?: 0L

        val ipa = santhaliToIpa(text)
        val ids = mutableListOf<Long>()
        ids.add(bos)

        var i = 0
        while (i < ipa.length) {
            val codePoint = ipa.codePointAt(i)
            val charCount = Character.charCount(codePoint)
            val ch = ipa.substring(i, i + charCount)

            if (phonemeIdMap.containsKey(ch)) {
                val tokenList = phonemeIdMap[ch] ?: emptyList()
                ids.addAll(tokenList)
                ids.add(pad) // Piper VITS inter-phoneme pad token
            } else if (ch.isBlank()) {
                val spaceList = phonemeIdMap[" "] ?: listOf(pad)
                ids.addAll(spaceList)
            }
            i += charCount
        }

        ids.add(eos)
        return ids.toLongArray()
    }

    /**
     * Parses phoneme_id_map from sat_piper_model.onnx.json string.
     */
    fun parsePhonemeIdMap(jsonString: String): Map<String, List<Long>> {
        val root = JSONObject(jsonString)
        val mapObj = root.optJSONObject("phoneme_id_map") ?: return emptyMap()
        val result = HashMap<String, List<Long>>()

        val keys = mapObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val array = mapObj.optJSONArray(key)
            if (array != null) {
                val list = mutableListOf<Long>()
                for (j in 0 until array.length()) {
                    list.add(array.getLong(j))
                }
                result[key] = list
            }
        }
        return result
    }

    /**
     * Converts Ol Chiki text into an authentic Devanagari phonetic reading guide
     * for Hindi-speaking primary school teachers.
     */
    fun toPhoneticDevanagari(olChikiText: String): String {
        if (olChikiText.isBlank()) return ""

        val compMap = listOf(
            "ᱛᱷ" to "थ", "ᱠᱷ" to "ख", "ᱜᱷ" to "घ", "ᱪᱷ" to "छ", "ᱡᱷ" to "झ",
            "ᱴᱷ" to "ठ", "ᱰᱷ" to "ढ", "ᱫᱷ" to "ध", "ᱯᱷ" to "फ", "ᱵᱷ" to "भ",
            "ᱜᱽ" to "ग", "ᱡᱽ" to "ज", "ᱫᱽ" to "द", "ᱵᱽ" to "ब",
            "ᱟᱹ" to "ा", "ᱚᱹ" to "ो", "ᱮᱹ" to "े", "ᱩᱹ" to "ु", "ᱤᱹ" to "ि"
        )

        val devaMap = mapOf(
            "ᱚ" to "ो", "ᱛ" to "त", "ᱜ" to "ग", "ᱝ" to "ङ", "ᱞ" to "ल",
            "ᱟ" to "ा", "ᱠ" to "क", "ᱡ" to "ज", "ᱢ" to "म", "ᱣ" to "व",
            "ᱤ" to "ि", "ᱥ" to "स", "ᱦ" to "ह", "ᱧ" to "ञ", "ᱨ" to "र",
            "ᱩ" to "ु", "ᱪ" to "च", "ᱫ" to "द", "ᱬ" to "ण", "ᱭ" to "य",
            "ᱮ" to "े", "ᱯ" to "प", "ᱰ" to "ड", "ᱱ" to "न", "ᱲ" to "ड़",
            "ᱳ" to "ो", "ᱴ" to "ट", "ᱵ" to "ब", "ᱶ" to "ँ", "᱾" to "।"
        )

        val vowelStart = mapOf(
            "ᱚ" to "ओ", "ᱟ" to "आ", "ᱤ" to "इ", "ᱩ" to "उ", "ᱮ" to "ए", "ᱳ" to "ओ"
        )

        var processed = olChikiText
        for ((k, v) in compMap) {
            processed = processed.replace(k, v)
        }

        val words = processed.split(Regex("\\s+"))
        val outWords = mutableListOf<String>()

        for (w in words) {
            val sb = java.lang.StringBuilder()
            for (idx in w.indices) {
                val ch = w[idx].toString()
                if (idx == 0 && vowelStart.containsKey(ch)) {
                    sb.append(vowelStart[ch])
                } else if (devaMap.containsKey(ch)) {
                    sb.append(devaMap[ch])
                } else if (ch !in listOf("ᱽ", "ᱹ", "ᱻ")) {
                    sb.append(ch)
                }
            }
            if (sb.isNotEmpty()) {
                outWords.add(sb.toString())
            }
        }

        val guide = outWords.joinToString(" ").trim()
        return if (guide.isNotBlank()) "[$guide]" else ""
    }
}

