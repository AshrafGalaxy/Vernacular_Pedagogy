package com.example.palashsetu.ui.screens.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palashsetu.domain.engine.AsrEngine
import com.example.palashsetu.domain.engine.AsrState
import com.example.palashsetu.domain.engine.AudioPlayerState
import com.example.palashsetu.domain.engine.NmtEngine
import com.example.palashsetu.domain.engine.PedagogicalAudioEngine
import com.example.palashsetu.theme.Background
import com.example.palashsetu.theme.Primary
import com.example.palashsetu.theme.PrimaryContainer
import com.example.palashsetu.theme.Secondary
import com.example.palashsetu.theme.SurfaceContainer
import com.example.palashsetu.theme.SurfaceContainerHigh
import com.example.palashsetu.theme.SurfaceContainerLow
import com.example.palashsetu.theme.SurfaceContainerLowest
import com.example.palashsetu.ui.components.AudioWaveVisualizer
import com.example.palashsetu.ui.components.DialectChips
import com.example.palashsetu.ui.components.PalashTopBar
import com.example.palashsetu.ui.components.PhoneticGuideCard
import kotlinx.coroutines.launch

@Composable
fun LiveVoiceBridgeScreen(
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val asrEngine = remember { AsrEngine() }
    val nmtEngine = remember { NmtEngine() }
    val audioEngine = remember { PedagogicalAudioEngine() }

    var isMicActive by remember { mutableStateOf(false) }
    var teacherHindiText by remember {
        mutableStateOf("बच्चो, अपनी गणित की किताब निकालो और पृष्ठ संख्या बारह खोलो।")
    }
    var santaliOlChikiText by remember {
        mutableStateOf("ᱜᱤᱫᱽᱨᱟᱹ ᱠᱚ, ᱟᱯᱮᱭᱟᱜ ᱮᱞᱠᱷᱟ ᱯᱩᱛᱷᱤ ᱡᱷᱤᱡᱽ ᱯᱮ ᱟᱨ ᱜᱮᱞ ᱵᱟᱨ ᱥᱟᱦᱴᱟ ᱩᱰᱩᱠ ᱯᱮ᱾")
    }
    var phoneticGuide by remember {
        mutableStateOf("[गिदरा को, आपेयाग एलखा पुथी झीज पे आर गेल बार साहटा उडुक पे]")
    }
    var latencyDisplay by remember { mutableStateOf("⏱ 420ms विलंबता") }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var playProgress by remember { mutableStateOf(0f) }
    var currentSpeed by remember { mutableStateOf(0.9f) }
    var selectedDialect by remember { mutableStateOf("ᱥᱟᱱᱛᱟᱲᱤ") }

    fun triggerTranslation(hindiSentence: String) {
        teacherHindiText = hindiSentence
        coroutineScope.launch {
            val result = nmtEngine.translate(hindiSentence)
            santaliOlChikiText = result.targetOlChiki
            phoneticGuide = result.phoneticGuide
            latencyDisplay = if (result.isTier1FastPath) "⏱ 21ms (Tier-1 Cache)" else "⏱ 420ms (INT8 CT2)"
        }
    }

    fun playAudio() {
        coroutineScope.launch {
            audioEngine.playSynthesizedAudio(santaliOlChikiText).collect { state ->
                when (state) {
                    is AudioPlayerState.Synthesizing -> isAudioPlaying = true
                    is AudioPlayerState.Playing -> {
                        isAudioPlaying = true
                        playProgress = state.progress
                    }
                    is AudioPlayerState.Finished, AudioPlayerState.Idle -> {
                        isAudioPlaying = false
                        playProgress = 0f
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header TopBar
            PalashTopBar()

            val cardCornerShape = RoundedCornerShape(8.dp)
            val controlCornerShape = RoundedCornerShape(4.dp)

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Language Bridge Indicator Card (Sharp Stitch Geometry)
                Card(
                    shape = cardCornerShape,
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Teacher side (Sharp 4dp container)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(controlCornerShape)
                                    .background(SurfaceContainerLowest)
                                    .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "🏫", fontSize = 16.sp)
                                Column {
                                    Text(text = "शिक्षक", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                    Text(text = "हिन्दी (Hindi)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
                                }
                            }

                            // Sync icon (Sharp 4dp container)
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(32.dp)
                                    .clip(controlCornerShape)
                                    .background(PrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "⇄", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }

                            // Student side (Sharp 4dp container)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(controlCornerShape)
                                    .background(SurfaceContainerLowest)
                                    .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "🎒", fontSize = 16.sp)
                                Column {
                                    Text(text = "विद्यार्थी", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                    Text(text = "ᱥᱟᱱᱛᱟᱲᱤ (Ol Chiki)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Secondary)
                                }
                            }
                        }

                        // Dialect Chips (Santali Only)
                        DialectChips(
                            selectedDialect = selectedDialect,
                            onDialectSelect = { selectedDialect = it }
                        )
                    }
                }

                // Section 1: Teacher Live ASR Input Card (Sharp Stitch Geometry)
                Card(
                    shape = cardCornerShape,
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "🎙️", fontSize = 14.sp)
                                Text(
                                    text = "शिक्षक का हिंदी वाक्य • Live ASR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(controlCornerShape)
                                    .background(Color(0xFFE0F2FE))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(text = "• LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "\"$teacherHindiText\"",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            AudioWaveVisualizer(isAnimating = isMicActive)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(controlCornerShape)
                                .background(SurfaceContainerLow)
                                .border(1.dp, Color(0xFFE2E8F0), controlCornerShape)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "⚡ स्थानीय Vosk ASR", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(text = latencyDisplay, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                        }
                    }
                }

                // Section 2: Classroom Broadcast Card (Ol Chiki + Phonetic Guide + Audio)
                Card(
                    shape = cardCornerShape,
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "कक्षा प्रसारण",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .clip(controlCornerShape)
                                        .background(Primary)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                Text(text = "संथाली (Santali - Ol Chiki)", fontSize = 11.sp, color = Color(0xFF475569))
                            }
                            Text(
                                text = "✓ JCERT अनुमोदित",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Secondary,
                                modifier = Modifier
                                    .clip(controlCornerShape)
                                    .background(SurfaceContainerLowest)
                                    .border(1.dp, Color(0xFFE2E8F0), controlCornerShape)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // Large Ol Chiki Script Text (Sharp 4dp Box)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(controlCornerShape)
                                .background(SurfaceContainerLowest)
                                .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = santaliOlChikiText,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                lineHeight = 30.sp
                            )
                        }

                        // Devanagari Teacher Phonetic Guide
                        PhoneticGuideCard(phoneticText = phoneticGuide)

                        // Piper TTS Audio Controls Bar (Sharp 4dp Controls, 44dp height)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play Button
                            Button(
                                onClick = { playAudio() },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = controlCornerShape,
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isAudioPlaying) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Text("ध्वनि प्रसारण जारी...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    } else {
                                        Text("▶ सुनाएं (Piper TTS)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }

                            // Cadence speed toggle (0.9x / 1.0x)
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(controlCornerShape)
                                    .background(SurfaceContainerLowest)
                                    .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                                    .clickable { currentSpeed = audioEngine.toggleSpeed() }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${currentSpeed}x",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Secondary
                                )
                            }

                            // Replay button
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(controlCornerShape)
                                    .background(SurfaceContainerLowest)
                                    .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                                    .clickable { playAudio() }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "↺", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Primary)
                            }
                        }
                    }
                }

                // Section 3: Instant 1-Tap Classroom Commands (Sharp 4dp Rows)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "त्वरित कक्षा निर्देश (Instant 1-Tap Commands):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )

                    val quickCommands = listOf(
                        Triple("📖 1. किताब खोलो (Open Books)", "ᱯᱩᱛᱷᱤ ᱡᱷᱤᱡᱽ ᱯᱮ", "किताब खोलो"),
                        Triple("🔢 2. 1 से 10 गिनो (Count 1-10)", "ᱢᱤᱫ ᱠᱷᱚᱱ ᱜᱮᱞ ᱞᱮᱠᱷᱟᱭ ᱯᱮ", "1 से 10 गिनो"),
                        Triple("🤫 3. शांत रहें (Maintain Silence)", "ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱯᱮ", "शांत रहें")
                    )

                    quickCommands.forEach { (label, olchiki, hindi) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(controlCornerShape)
                                .background(SurfaceContainerLowest)
                                .border(1.dp, Color(0xFFE2E8F0), controlCornerShape)
                                .clickable {
                                    triggerTranslation(hindi)
                                    playAudio()
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                                Text(text = olchiki, fontSize = 13.sp, color = Secondary, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "🔊", fontSize = 18.sp)
                        }
                    }
                }
            }

        }

        // Push-to-Talk Floating Microphone Button
        FloatingActionButton(
            onClick = {
                isMicActive = !isMicActive
                if (isMicActive) {
                    coroutineScope.launch {
                        asrEngine.startListening().collect { state ->
                            when (state) {
                                is AsrState.Listening -> {}
                                is AsrState.PartialText -> teacherHindiText = state.text
                                is AsrState.Recognized -> {
                                    isMicActive = false
                                    triggerTranslation(state.finalSentence)
                                    playAudio()
                                }
                                AsrState.Idle -> isMicActive = false
                            }
                        }
                    }
                } else {
                    asrEngine.stopListening()
                }
            },
            containerColor = if (isMicActive) Color(0xFFDC2626) else Primary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .size(64.dp)
        ) {
            Text(
                text = if (isMicActive) "⏹" else "🎙️",
                fontSize = 24.sp
            )
        }
    }
}
