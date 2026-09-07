package com.example.palashsetu.data.model

data class TranslationResult(
    val sourceHindi: String,
    val targetOlChiki: String,
    val phoneticGuide: String,
    val isTier1FastPath: Boolean = true,
    val latencyMs: Long = 420,
    val verifiedByJcert: Boolean = true
)
