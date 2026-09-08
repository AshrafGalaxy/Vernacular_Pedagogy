package com.example.palashsetu.data.model

/**
 * Pedagogical Categories for Cultural Realia Motifs.
 */
enum class MotifCategory(
    val code: String,
    val labelHi: String,
    val labelEn: String,
    val labelOlChiki: String
) {
    ALL("ALL", "सभी", "All", "ᱡᱚᱛᱚ"),
    ANIMALS("ANIMALS", "पशु व पक्षी", "Animals & Birds", "ᱡᱤᱵᱽ ᱡᱤᱭᱟᱹᱞᱤ"),
    BODY_PARTS("BODY_PARTS", "शरीर के अंग", "Body Parts", "ᱦᱚᱲᱢᱚ ᱨᱮᱭᱟᱜ ᱦᱟᱹᱴᱤᱧ"),
    CLASSROOM("CLASSROOM", "कक्षा व विद्यालय", "Classroom & School", "ᱟᱥᱲᱟ ᱥᱟᱯᱟᱵᱽ"),
    FRUITS_VEG_FOOD("FRUITS_VEG_FOOD", "फल, सब्जी व भोजन", "Fruits & Food", "ᱡᱚ-ᱥᱟᱠᱟᱢ ᱟᱨ ᱡᱚᱢᱟᱜ"),
    NATURE("NATURE", "प्रकृति व परिवेश", "Nature", "ᱡᱟᱦᱮᱨ ᱟᱨ ᱯᱩᱨᱠᱨᱤᱛᱤ"),
    PEOPLE_ACTIONS("PEOPLE_ACTIONS", "परिवार व क्रियाएं", "People & Actions", "ᱜᱷᱟᱨᱚᱸᱡᱽ ᱟᱨ ᱠᱟᱹᱢᱤ"),
    NUMERACY_SHAPES("NUMERACY_SHAPES", "गिनती व आकृतियां", "Numeracy & Shapes", "ᱮᱞᱠᱷᱟ ᱟᱨ ᱞᱮᱠᱷᱟ");

    companion object {
        fun fromCode(code: String): MotifCategory {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ALL
        }
    }
}

/**
 * Model representing an authentic cultural visual motif for flashcards,
 * worksheets, and workbooks.
 */
data class MotifItem(
    val id: String,
    val filename: String,
    val assetPath: String,
    val nameHi: String,
    val nameOlchiki: String,
    val phoneticDeva: String,
    val category: MotifCategory,
    val grade: Int,
    val aspectRatio: Float,
    val compressedSizeKb: Float,
    val tags: List<String>,
    val contextPrompts: List<String>
) {
    /**
     * Returns true if any keyword or context prompt matches the search query.
     */
    fun matchesQuery(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return nameHi.lowercase().contains(q) ||
                nameOlchiki.contains(q) ||
                phoneticDeva.lowercase().contains(q) ||
                category.labelHi.contains(q) ||
                category.labelEn.lowercase().contains(q) ||
                tags.any { it.lowercase().contains(q) } ||
                contextPrompts.any { it.lowercase().contains(q) }
    }
}
