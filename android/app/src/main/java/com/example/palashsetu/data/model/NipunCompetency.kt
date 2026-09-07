package com.example.palashsetu.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Standard NIPUN Bharat Foundational Literacy & Numeracy (FLN) Competency Outcome.
 *
 * Mapped against the Ministry of Education's official NIPUN Lakshyas
 * and the Jharkhand JCERT PALASH vernacular primary curriculum.
 * Provides bilingual support (Hindi & English) for all titles and subtitles.
 */
@Serializable
data class NipunCompetency(
    val code: String,
    val grade: Int,
    val domain: String, // "LITERACY" or "NUMERACY"
    val title: String = "",
    val subtitle: String = "",
    @SerialName("title_hi")
    val titleHi: String = "",
    @SerialName("title_en")
    val titleEn: String = "",
    @SerialName("subtitle_hi")
    val subtitleHi: String = "",
    @SerialName("subtitle_en")
    val subtitleEn: String = "",
    @SerialName("instruction_hi")
    val instructionHi: String = "",
    @SerialName("instruction_olchiki")
    val instructionOlchiki: String = "",
    @SerialName("motif_asset")
    val motifAsset: String = "",
    @SerialName("template_type")
    val templateType: String = ""
) {
    val isNumeracy: Boolean
        get() = domain.equals("NUMERACY", ignoreCase = true)

    val isLiteracy: Boolean
        get() = domain.equals("LITERACY", ignoreCase = true)

    /**
     * Resolves the localized title according to app language.
     */
    fun getTitle(isHindi: Boolean): String {
        return if (isHindi) {
            titleHi.ifBlank { title.ifBlank { titleEn } }
        } else {
            titleEn.ifBlank { title.ifBlank { titleHi } }
        }
    }

    /**
     * Resolves the localized subtitle according to app language.
     */
    fun getSubtitle(isHindi: Boolean): String {
        return if (isHindi) {
            subtitleHi.ifBlank { subtitle.ifBlank { subtitleEn } }
        } else {
            subtitleEn.ifBlank { subtitle.ifBlank { subtitleHi } }
        }
    }

    /**
     * Localized domain descriptor badge (e.g. "संख्या ज्ञान (Numeracy)").
     */
    fun getDomainLabel(isHindi: Boolean): String {
        return if (isNumeracy) {
            if (isHindi) "गणित (संख्या ज्ञान)" else "Numeracy"
        } else {
            if (isHindi) "भाषा (साक्षरता)" else "Literacy"
        }
    }

    /**
     * Localized grade level badge.
     */
    fun getGradeLabel(isHindi: Boolean): String {
        return if (isHindi) "कक्षा $grade" else "Grade $grade"
    }
}
