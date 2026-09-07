package com.example.palashsetu.data.model

data class FlnPhrase(
    val id: String,
    val hindi: String,
    val olchiki: String,
    val english: String = "",
    val phoneticDevanagari: String = "",
    val grade: Int = 2,
    val category: String = "कक्षा प्रबंधन",
    val domain: String = "classroom_command"
)
