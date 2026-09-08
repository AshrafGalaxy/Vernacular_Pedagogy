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
     * Returns 2 realia items with counts for an addition or counting problem (e.g. 3 mangoes + 2 bananas).
     */
    data class MathExerciseData(
        val item1: MotifItem,
        val count1: Int,
        val item2: MotifItem,
        val count2: Int,
        val total: Int
    )

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
     * Returns 4 paired motifs with their scrambled bilingual labels.
     */
    data class MatchingPair(
        val motif: MotifItem,
        val labelHi: String,
        val labelOlchiki: String,
        val phoneticDeva: String
    )

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
}
