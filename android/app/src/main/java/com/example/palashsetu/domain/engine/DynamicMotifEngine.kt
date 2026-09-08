package com.example.palashsetu.domain.engine

import android.content.Context
import com.example.palashsetu.data.local.FlashcardMotifRepository
import com.example.palashsetu.data.model.MotifCategory
import com.example.palashsetu.data.model.MotifItem
import com.example.palashsetu.data.model.NipunCompetency

/**
 * Dynamic Context-Based Resolution Engine for Flashcards, Worksheets & Workbooks.
 * Resolves appropriate visual realia motifs from prompts, competencies, and categories.
 */
object DynamicMotifEngine {

    /**
     * Resolves a curated deck of motifs based on a free-text prompt or category.
     */
    fun resolveMotifsForPrompt(
        context: Context,
        prompt: String,
        selectedCategory: MotifCategory = MotifCategory.ALL,
        grade: Int? = null,
        limit: Int = 150
    ): List<MotifItem> {
        val allMotifs = FlashcardMotifRepository.getMotifs(context)
        if (prompt.isBlank() && selectedCategory == MotifCategory.ALL && grade == null) {
            return allMotifs.take(limit)
        }

        val cleanPrompt = prompt.trim().lowercase()
        val tokens = cleanPrompt.split(Regex("[\\s,;।?!]+")).filter { it.length > 1 }

        val scored = allMotifs.mapNotNull { motif ->
            var score = 0

            // 1. Grade filtering
            if (grade != null && motif.grade == grade) {
                score += 3
            } else if (grade != null && motif.grade != grade) {
                score -= 1
            }

            // 2. Category filtering
            if (selectedCategory != MotifCategory.ALL) {
                if (motif.category == selectedCategory) {
                    score += 15
                } else {
                    return@mapNotNull null // Strict category filtering when explicitly selected
                }
            }

            // 3. Prompt context matching
            if (cleanPrompt.isNotEmpty()) {
                // Exact prompt hit in contextPrompts
                if (motif.contextPrompts.any { it.lowercase().contains(cleanPrompt) }) {
                    score += 25
                }
                // Token matches in name, tags, and prompts
                for (token in tokens) {
                    if (motif.nameHi.lowercase().contains(token)) score += 10
                    if (motif.nameOlchiki.contains(token)) score += 10
                    if (motif.phoneticDeva.lowercase().contains(token)) score += 8
                    if (motif.tags.any { it.lowercase().contains(token) }) score += 6
                    if (motif.contextPrompts.any { it.lowercase().contains(token) }) score += 8
                    if (motif.category.labelHi.contains(token)) score += 5
                }
            } else {
                score += 5 // Baseline for browse mode
            }

            if (score > 0) motif to score else null
        }

        return scored.sortedByDescending { it.second }.map { it.first }.take(limit)
    }

    /**
     * Resolves motifs suitable for a specific NIPUN Bharat FLN competency.
     */
    fun resolveMotifsForCompetency(
        context: Context,
        competency: NipunCompetency,
        limit: Int = 10
    ): List<MotifItem> {
        val grade = competency.grade
        val isMath = competency.isNumeracy
        val defaultCategory = if (isMath) MotifCategory.NUMERACY_SHAPES else MotifCategory.ANIMALS

        val searchPrompt = "${competency.titleHi} ${competency.instructionHi} ${competency.getDomainLabel(true)}"
        val results = resolveMotifsForPrompt(context, searchPrompt, MotifCategory.ALL, grade, limit)

        if (results.isEmpty()) {
            return FlashcardMotifRepository.getRandomMotifs(context, limit, defaultCategory, grade)
        }
        return results
    }

    /**
     * Dynamic Math Exercise Dataset for Worksheets:
     * Alias to com.example.palashsetu.data.model.RealiaMathExercise.
     */
    typealias MathExerciseData = com.example.palashsetu.data.model.RealiaMathExercise

    fun generateMathExercise(context: Context, grade: Int): MathExerciseData {
        val foodItems = FlashcardMotifRepository.filterMotifs(context, category = MotifCategory.FRUITS_VEG_FOOD, grade = grade)
            .ifEmpty { FlashcardMotifRepository.getMotifs(context) }
            .shuffled()

        val item1 = foodItems.getOrNull(0) ?: FlashcardMotifRepository.getMotifs(context).first()
        val item2 = foodItems.getOrNull(1) ?: FlashcardMotifRepository.getMotifs(context).last()

        val (c1, c2) = when (grade) {
            1 -> (1..4).random() to (1..4).random()
            2 -> (2..6).random() to (2..5).random()
            else -> (3..9).random() to (3..8).random()
        }

        return MathExerciseData(
            item1 = item1,
            count1 = c1,
            item2 = item2,
            count2 = c2,
            total = c1 + c2
        )
    }

    /**
     * Dynamic Matching Column Dataset for Worksheets:
     * Alias to com.example.palashsetu.data.model.MatchPairItem.
     */
    typealias MatchingPair = com.example.palashsetu.data.model.MatchPairItem

    fun generateMatchingColumn(context: Context, category: MotifCategory = MotifCategory.ALL, grade: Int = 1): List<MatchingPair> {
        val selected = FlashcardMotifRepository.filterMotifs(context, category = category, grade = grade)
            .ifEmpty { FlashcardMotifRepository.getMotifs(context) }
            .shuffled()
            .take(4)

        return selected.map { motif ->
            MatchingPair(
                motif = motif,
                labelHi = motif.nameHi,
                labelOlchiki = motif.nameOlchiki,
                phoneticDeva = motif.phoneticDeva
            )
        }
    }

    /**
     * Master Dynamic Worksheet Generation Pipeline:
     * Transforms a WorksheetSpec into a complete, reproducible GeneratedWorksheet payload.
     */
    fun generateWorksheet(context: Context, spec: com.example.palashsetu.data.model.WorksheetSpec): com.example.palashsetu.data.model.GeneratedWorksheet {
        val rng = kotlin.random.Random(spec.seed)
        val allMotifs = FlashcardMotifRepository.getMotifs(context)

        // 1. Resolve candidate motifs directly based on the chosen topic theme and grade
        val candidates: List<MotifItem> = if (spec.topicTheme != MotifCategory.ALL) {
            FlashcardMotifRepository.filterMotifs(
                context = context,
                category = spec.topicTheme,
                grade = spec.grade
            ).ifEmpty {
                FlashcardMotifRepository.filterMotifs(context, category = spec.topicTheme)
            }.ifEmpty { allMotifs }
        } else {
            resolveMotifsForCompetency(
                context = context,
                competency = spec.competency,
                limit = 50
            ).ifEmpty { allMotifs }
        }

        val shuffledPool = candidates.shuffled(rng).toMutableList()
        if (shuffledPool.size < 8) {
            val filler = allMotifs.shuffled(rng).filter { m -> !shuffledPool.any { it.id == m.id } }
            shuffledPool.addAll(filler)
        }

        // 2. Math Addition Generation (bounded by spec.maxNumber)
        val math1: MathExerciseData? = if (
            spec.activityType == com.example.palashsetu.data.model.WorksheetActivityType.MATH_ADDITION ||
            spec.activityType == com.example.palashsetu.data.model.WorksheetActivityType.COMPREHENSIVE_FLN ||
            spec.competency.isNumeracy
        ) {
            val itemA = shuffledPool.getOrNull(0) ?: allMotifs.first()
            val itemB = shuffledPool.getOrNull(1) ?: allMotifs.last()
            val bound = (spec.maxNumber / 2).coerceIn(2, 6)
            val c1 = (1..bound).random(rng)
            val c2 = (1..bound).random(rng)
            MathExerciseData(item1 = itemA, count1 = c1, item2 = itemB, count2 = c2, total = c1 + c2)
        } else null

        val math2: MathExerciseData? = if (spec.activityType == com.example.palashsetu.data.model.WorksheetActivityType.MATH_ADDITION) {
            val itemC = shuffledPool.getOrNull(2) ?: allMotifs.first()
            val itemD = shuffledPool.getOrNull(3) ?: allMotifs.last()
            val bound = (spec.maxNumber / 2).coerceIn(2, 6)
            val c1 = (1..bound).random(rng)
            val c2 = (1..bound).random(rng)
            MathExerciseData(item1 = itemC, count1 = c1, item2 = itemD, count2 = c2, total = c1 + c2)
        } else null

        // 3. Matching Pairs Generation (4 distinct motifs)
        val matchPairs: List<MatchingPair>? = if (
            spec.activityType == com.example.palashsetu.data.model.WorksheetActivityType.MATCH_COLUMN ||
            spec.activityType == com.example.palashsetu.data.model.WorksheetActivityType.COMPREHENSIVE_FLN ||
            spec.competency.isLiteracy
        ) {
            val poolStart = if (math1 != null) 4 else 0
            val selected = (poolStart until (poolStart + 4)).mapNotNull { idx ->
                shuffledPool.getOrNull(idx % shuffledPool.size)
            }.distinctBy { it.id }.take(4)

            val effective = if (selected.size < 4) {
                (selected + allMotifs.shuffled(rng)).distinctBy { it.id }.take(4)
            } else selected

            effective.map { m ->
                MatchingPair(
                    motif = m,
                    labelHi = m.nameHi,
                    labelOlchiki = m.nameOlchiki,
                    phoneticDeva = m.phoneticDeva
                )
            }
        } else null

        val scrambledLabels = matchPairs?.shuffled(rng)

        // 4. Tracing Items Generation
        val tracingItems: List<com.example.palashsetu.data.model.TracingItem>? = if (
            spec.activityType == com.example.palashsetu.data.model.WorksheetActivityType.VOCAB_TRACING ||
            spec.activityType == com.example.palashsetu.data.model.WorksheetActivityType.COMPREHENSIVE_FLN
        ) {
            val tracingPool = shuffledPool.takeLast(4)
            tracingPool.map { m ->
                com.example.palashsetu.data.model.TracingItem(
                    motif = m,
                    olchikiWord = m.nameOlchiki,
                    devaPhonetic = m.phoneticDeva,
                    hindiMeaning = m.nameHi
                )
            }
        } else null

        // 5. Instruction resolution
        val (instOlchiki, instHi) = when (spec.activityType) {
            com.example.palashsetu.data.model.WorksheetActivityType.MATH_ADDITION ->
                "ᱪᱤᱛᱟᱹᱨ ᱞᱮᱠᱷᱟ ᱠᱟᱛᱮ ᱡᱚᱲ ᱮᱞ ᱚᱞ ᱢᱮ᱾" to "चित्रों को गिनकर जोड़ का सही उत्तर लिखें।"
            com.example.palashsetu.data.model.WorksheetActivityType.MATCH_COLUMN ->
                "ᱪᱤᱛᱟᱹᱨ ᱧᱮᱞ ᱠᱟᱛᱮ ᱥᱟᱹᱦᱤ ᱟᱹᱲᱟᱹ ᱥᱟᱶ ᱡᱚᱲ ᱢᱮᱲ ᱢᱮ᱾" to "चित्र पहचान कर सही संथाली (ओल चिकी) शब्द से मिलान करें।"
            com.example.palashsetu.data.model.WorksheetActivityType.VOCAB_TRACING ->
                "ᱟᱹᱲᱟᱹ ᱪᱮᱛᱟᱱ ᱨᱮ ᱯᱮᱱᱥᱤᱞ ᱪᱟᱞᱟᱣ ᱠᱟᱛᱮ ᱪᱮᱫ ᱢᱮ᱾" to "ओल चिकी अक्षरों व शब्दों पर पेंसिल चलाकर सुंदर लेखन करें।"
            com.example.palashsetu.data.model.WorksheetActivityType.COMPREHENSIVE_FLN -> {
                if (spec.competency.instructionOlchiki.isNotBlank()) {
                    spec.competency.instructionOlchiki to spec.competency.instructionHi
                } else {
                    "ᱪᱤᱛᱟᱹᱨ ᱧᱮᱞ ᱠᱟᱛᱮ ᱮᱞᱠᱷᱟ ᱯᱩᱨᱟᱹᱣ ᱢᱮ ᱟᱨ ᱥᱟᱹᱦᱤ ᱡᱚᱲ ᱵᱮᱱᱟᱣ ᱢᱮ᱾" to "चित्र देखकर गणना करें और सही मिलान करें।"
                }
            }
        }

        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val currentDate = dateFormat.format(java.util.Date())

        return com.example.palashsetu.data.model.GeneratedWorksheet(
            spec = spec,
            generatedDate = currentDate,
            titleHi = "बुनियादी साक्षरता एवं संख्याज्ञान (FLN) अभ्यास पत्रक",
            titleEn = "Foundational Literacy & Numeracy (FLN) Worksheet",
            subtitleHi = "${spec.competency.code}: ${spec.competency.getTitle(true)}",
            instructionOlchiki = instOlchiki,
            instructionHi = instHi,
            mathExercise = math1,
            mathExercise2 = math2,
            matchingPairs = matchPairs,
            scrambledLabels = scrambledLabels,
            tracingItems = tracingItems
        )
    }
}

