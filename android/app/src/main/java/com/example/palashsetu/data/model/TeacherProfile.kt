package com.example.palashsetu.data.model

data class TeacherProfile(
    val salutation: String = "श्री", // "श्री", "श्रीमती", "शिक्षक"
    val name: String = "",
    val pin: String = "2604",
    val isConfigured: Boolean = false
) {
    val displayName: String
        get() = if (name.isNotBlank()) "$salutation $name" else "शिक्षक (Teacher)"

    val avatarEmoji: String
        get() = when (salutation) {
            "श्रीमती" -> "👩‍🏫"
            "शिक्षक" -> "🧑‍🏫"
            else -> "👨‍🏫"
        }
}
