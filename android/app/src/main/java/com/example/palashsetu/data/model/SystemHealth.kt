package com.example.palashsetu.data.model

data class SystemHealth(
    val asrStatus: String = "INT8 Sherpa-ONNX / Vosk • Ready",
    val nmtStatus: String = "IndicTrans2 CT2 (mmap) • Loaded (~120MB RAM)",
    val ttsStatus: String = "Piper VITS 16kHz ONNX • Active (60.6 MB)",
    val flnStatus: String = "JCERT Grade 1-3 Lexicon • 368 Entries Indexed",
    val ramUsedMb: Int = 185,
    val ramTotalMb: Int = 2048,
    val brcSyncZone: String = "BRC Khunti Zone 3",
    val isMeshSyncActive: Boolean = true
)
