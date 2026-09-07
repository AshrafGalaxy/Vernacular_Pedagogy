package com.example.palashsetu.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Standard NIPUN Bharat Foundational Literacy & Numeracy (FLN) Competency Outcome.
 *
 * Mapped against the Ministry of Education's official NIPUN Lakshyas
 * and the Jharkhand JCERT PALASH vernacular primary curriculum.
 */
@Serializable
data class NipunCompetency(
    val code: String,
    val grade: Int,
    val domain: String, // "LITERACY" or "NUMERACY"
    val title: String,
    val subtitle: String,
    @SerialName("instruction_hi")
    val instructionHi: String,
    @SerialName("instruction_olchiki")
    val instructionOlchiki: String,
    @SerialName("motif_asset")
    val motifAsset: String = "",
    @SerialName("template_type")
    val templateType: String = ""
) {
    val isNumeracy: Boolean
        get() = domain.equals("NUMERACY", ignoreCase = true)

    val isLiteracy: Boolean
        get() = domain.equals("LITERACY", ignoreCase = true)
}
