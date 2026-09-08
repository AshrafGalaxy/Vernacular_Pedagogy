package com.example.palashsetu.data.model

/**
 * Activity types supported by the Dynamic Worksheet Engine.
 */
enum class WorksheetActivityType(
    val labelHi: String,
    val labelEn: String,
    val descriptionHi: String
) {
    COMPREHENSIVE_FLN(
        labelHi = "मिश्रित अभ्यास (A4)",
        labelEn = "Comprehensive FLN",
        descriptionHi = "जोड़, चित्र मिलान व शब्द लेखन का पूर्ण अभ्यास"
    ),
    MATH_ADDITION(
        labelHi = "चित्र जोड़ व गिनती",
        labelEn = "Realia Math Addition",
        descriptionHi = "वास्तविक चित्रों को गिनकर जोड़ने का अभ्यास"
    ),
    MATCH_COLUMN(
        labelHi = "चित्र-शब्द मिलान",
        labelEn = "Realia Column Match",
        descriptionHi = "चित्रों को सही संथाली (ओल चिकी) शब्दों से मिलाना"
    ),
    VOCAB_TRACING(
        labelHi = "शब्द व अक्षर लेखन",
        labelEn = "Tracing & Vocabulary",
        descriptionHi = "ओल चिकी अक्षरों व शब्दों के लेखन का अभ्यास"
    )
}

/**
 * Configuration specification for dynamically generating a worksheet.
 */
data class WorksheetSpec(
    val grade: Int = 2,
    val competency: NipunCompetency,
    val activityType: WorksheetActivityType = WorksheetActivityType.COMPREHENSIVE_FLN,
    val topicTheme: MotifCategory = MotifCategory.ALL,
    val maxNumber: Int = 10,
    val seed: Long = System.currentTimeMillis()
)

/**
 * Dynamic realia math addition problem dataset.
 */
data class RealiaMathExercise(
    val item1: MotifItem,
    val count1: Int,
    val item2: MotifItem,
    val count2: Int,
    val total: Int
)

/**
 * Dynamic column matching item.
 */
data class MatchPairItem(
    val motif: MotifItem,
    val labelHi: String,
    val labelOlchiki: String,
    val phoneticDeva: String
)

/**
 * Item used in vocabulary tracing exercise.
 */
data class TracingItem(
    val motif: MotifItem,
    val olchikiWord: String,
    val devaPhonetic: String,
    val hindiMeaning: String
)

/**
 * Fully generated worksheet payload ready for live in-app preview and synchronized PDF rendering.
 */
data class GeneratedWorksheet(
    val spec: WorksheetSpec,
    val generatedDate: String,
    val titleHi: String,
    val titleEn: String,
    val subtitleHi: String,
    val instructionOlchiki: String,
    val instructionHi: String,
    val mathExercise: RealiaMathExercise?,
    val mathExercise2: RealiaMathExercise? = null,
    val matchingPairs: List<MatchPairItem>?,
    val scrambledLabels: List<MatchPairItem>?,
    val tracingItems: List<TracingItem>?
)
