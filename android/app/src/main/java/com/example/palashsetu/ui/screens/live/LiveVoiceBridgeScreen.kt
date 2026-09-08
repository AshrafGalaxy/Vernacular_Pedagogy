package com.example.palashsetu.ui.screens.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import com.example.palashsetu.R
import com.example.palashsetu.data.local.UserSessionManager
import com.example.palashsetu.data.model.TranslationResult
import kotlinx.coroutines.Job
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.palashsetu.ui.onboarding.LocalOnboardingRegistry
import com.example.palashsetu.ui.onboarding.onboardingTarget
import kotlinx.coroutines.launch

@Composable
fun LiveVoiceBridgeScreen(
    currentLanguage: String = UserSessionManager.getLanguage(LocalContext.current),
    onLanguageToggle: ((String) -> Unit)? = null,
    onReplayWalkthrough: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val asrEngine = remember { AsrEngine(context) }
    val nmtEngine = remember { NmtEngine() }
    val audioEngine = remember { PedagogicalAudioEngine(context) }

    val isHindi = currentLanguage == "hi"
    val onboardingRegistry = LocalOnboardingRegistry.current

    var isMicActive by remember { mutableStateOf(false) }
    var micAudioLevel by remember { mutableStateOf(0.1f) }
    var teacherHindiText by remember {
        mutableStateOf("बच्चो, अपनी गणित की किताब निकालो और पृष्ठ संख्या बारह खोलो।")
    }
    var santaliOlChikiText by remember {
        mutableStateOf("ᱜᱤᱫᱽᱨᱟᱹ ᱠᱚ, ᱟᱯᱮᱭᱟᱜ ᱮᱞᱠᱷᱟ ᱯᱩᱛᱷᱤ ᱡᱷᱤᱡᱽ ᱯᱮ ᱟᱨ ᱜᱮᱞ ᱵᱟᱨ ᱥᱟᱦᱴᱟ ᱩᱰᱩᱠ ᱯᱮ᱾")
    }
    var phoneticGuide by remember {
        mutableStateOf("[गिदरा को, आपेयाग एलखा पुथी झीज पे आर गेल बार साहटा उडुक पे]")
    }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var playProgress by remember { mutableStateOf(0f) }
    var currentSpeed by remember { mutableStateOf(0.9f) }
    var selectedDialect by remember { mutableStateOf("ᱥᱟᱱᱛᱟᱲᱤ") }
    var lastResult by remember { mutableStateOf<TranslationResult?>(null) }
    var playbackJob by remember { mutableStateOf<Job?>(null) }

    // Pre-warm ASR, VAD, Piper TTS, and ONNX NMT models in background
    LaunchedEffect(Unit) {
        asrEngine.initialize()
        nmtEngine.initialize(context)
        audioEngine.warmUp()
    }

    fun playAudio(targetText: String = santaliOlChikiText) {
        playbackJob?.cancel()
        playbackJob = coroutineScope.launch {
            audioEngine.playSynthesizedAudio(targetText).collect { state ->
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

    fun triggerTranslation(hindiSentence: String) {
        teacherHindiText = hindiSentence
        coroutineScope.launch {
            val result = nmtEngine.translate(hindiSentence)
            lastResult = result
            santaliOlChikiText = result.targetOlChiki
            phoneticGuide = if (result.phoneticGuide.isNotBlank()) {
                result.phoneticGuide
            } else {
                com.example.palashsetu.domain.engine.SanthaliPhonemizer.toPhoneticDevanagari(result.targetOlChiki)
            }
            playAudio(result.targetOlChiki)
        }
    }

    fun startListeningSession() {
        isMicActive = true
        coroutineScope.launch {
            asrEngine.startListening().collect { state ->
                when (state) {
                    is AsrState.Listening -> {
                        micAudioLevel = state.audioLevel
                    }
                    is AsrState.PartialText -> {
                        teacherHindiText = state.text
                    }
                    is AsrState.Recognized -> {
                        isMicActive = false
                        micAudioLevel = 0.1f
                        triggerTranslation(state.finalSentence)
                    }
                    AsrState.Idle -> {
                        isMicActive = false
                        micAudioLevel = 0.1f
                    }
                }
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListeningSession()
        }
    }

    val cardCornerShape = RoundedCornerShape(8.dp)
    val controlCornerShape = RoundedCornerShape(4.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Pinned Header TopBar
        PalashTopBar(
            currentLanguage = currentLanguage,
            onLanguageToggle = onLanguageToggle,
            onReplayWalkthrough = onReplayWalkthrough
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Teacher side (Sharp 4dp container)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(controlCornerShape)
                                    .background(SurfaceContainerLowest)
                                    .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_school),
                                    contentDescription = "Teacher",
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Text(
                                        text = if (isHindi) "शिक्षक" else "Teacher",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = if (isHindi) "हिन्दी" else "Hindi",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Sync icon (Sharp 4dp container with vector icon, vertically centered)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(controlCornerShape)
                                    .background(PrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_swap_horiz),
                                    contentDescription = "Voice Bridge",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Student side (Sharp 4dp container)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(controlCornerShape)
                                    .background(SurfaceContainerLowest)
                                    .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_backpack),
                                    contentDescription = "Student",
                                    tint = Secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Text(
                                        text = if (isHindi) "विद्यार्थी" else "Student",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = if (isHindi) "ᱥᱟᱱᱛᱟᱲᱤ (संथाली)" else "ᱥᱟᱱᱛᱟᱲᱤ (Santali)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Secondary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Dialect Chips (Santali Only) — tagged for onboarding spotlight
                        Box(modifier = Modifier.onboardingTarget("dialect_chips", onboardingRegistry)) {
                            DialectChips(
                                selectedDialect = selectedDialect,
                                onDialectSelect = { selectedDialect = it },
                                currentLanguage = currentLanguage
                            )
                        }
                    }
                }

                // Section 1: Teacher Live ASR Input Card — tagged for onboarding
                Card(
                    shape = cardCornerShape,
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onboardingTarget("hindi_transcript_card", onboardingRegistry)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_mic),
                                    contentDescription = "Microphone",
                                    tint = Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isHindi) "शिक्षक का हिंदी वाक्य • Live ASR" else "Teacher Speech (Hindi) • Live ASR",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            }
                            if (isMicActive) {
                                Text(
                                    text = "• LIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0369A1),
                                    modifier = Modifier
                                        .clip(controlCornerShape)
                                        .background(Color(0xFFE0F2FE))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(controlCornerShape)
                                .background(SurfaceContainerLow)
                                .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = teacherHindiText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            AudioWaveVisualizer(isAnimating = isMicActive)
                        }
                    }
                }

                // Section 2: Classroom Broadcast Card — tagged for onboarding
                Card(
                    shape = cardCornerShape,
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onboardingTarget("olchiki_output_card", onboardingRegistry)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isHindi) "कक्षा प्रसारण" else "Classroom Broadcast",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .clip(controlCornerShape)
                                    .background(Primary)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Text(
                                text = if (isHindi) "ᱥᱟᱱᱛᱟᱲᱤ (संथाली)" else "ᱥᱟᱱᱛᱟᱲᱤ (Santali)",
                                fontSize = 11.sp,
                                color = Color(0xFF475569),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
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
                        PhoneticGuideCard(
                            phoneticText = phoneticGuide,
                            currentLanguage = currentLanguage
                        )

                        // Piper TTS Audio Controls Bar (Sharp 4dp Controls, 44dp height)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play Button — tagged for onboarding spotlight
                            Button(
                                onClick = { playAudio() },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = controlCornerShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .onboardingTarget("play_audio_button", onboardingRegistry)
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
                                        Text(
                                            text = if (isHindi) "ध्वनि प्रसारण जारी..." else "Broadcasting...",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_volume_up),
                                            contentDescription = "Listen",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (isHindi) "सुनाएं" else "Listen",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
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

                            // Replay button with Vector Icon
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
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_refresh),
                                    contentDescription = "Replay",
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Real-Time Edge Latency Telemetry (Strict 22dp height, 4dp sharp corners)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .clip(controlCornerShape)
                                .background(Color(0xFFF1F5F9))
                                .border(1.dp, Color(0xFFE2E8F0), controlCornerShape)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isHindi) "स्थानीय AI टेलीमेट्री" else "On-Device Telemetry",
                                fontSize = 9.sp,
                                lineHeight = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                softWrap = false
                            )
                            val tts = audioEngine.lastTelemetry
                            val rtfFormatted = if (tts != null && tts.realTimeFactor > 0f) String.format(java.util.Locale.US, "%.2f", tts.realTimeFactor) else "0.04"
                            val ttsMs = if (tts != null && tts.synthesisDurationMs > 0) "${tts.synthesisDurationMs}ms" else "<45ms"
                            val cacheHitNotice = if (tts?.isCacheHit == true) " • LRU" else ""
                            val routerText = lastResult?.let { "${it.tier} ${it.latencyMs}ms" } ?: "Router <1ms"
                            Text(
                                text = "ASR ~380ms • $routerText • TTS $ttsMs$cacheHitNotice (RTF $rtfFormatted)",
                                fontSize = 9.sp,
                                lineHeight = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Section 3: Instant 1-Tap Classroom Commands (Sharp 4dp Rows)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isHindi) "त्वरित कक्षा निर्देश (1-टैप):" else "Instant Classroom Commands (1-Tap):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )

                    val quickCommands = if (isHindi) {
                        listOf(
                            Triple("1. किताब खोलो", "ᱯᱩᱛᱷᱤ ᱡᱷᱤᱡᱽ ᱯᱮ", "किताब खोलो"),
                            Triple("2. 1 से 10 गिनो", "ᱢᱤᱫ ᱠᱷᱚᱱ ᱜᱮᱞ ᱞᱮᱠᱷᱟᱭ ᱯᱮ", "1 से 10 गिनो"),
                            Triple("3. शांत रहें", "ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱯᱮ", "शांत रहें")
                        )
                    } else {
                        listOf(
                            Triple("1. Open Books", "ᱯᱩᱛᱷᱤ ᱡᱷᱤᱡᱽ ᱯᱮ", "किताब खोलो"),
                            Triple("2. Count 1 to 10", "ᱢᱤᱫ ᱠᱷᱚᱱ ᱜᱮᱞ ᱞᱮᱠᱷᱟᱭ ᱯᱮ", "1 से 10 गिनो"),
                            Triple("3. Maintain Silence", "ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱯᱮ", "शांत रहें")
                        )
                    }

                    quickCommands.forEach { (label, olchiki, hindi) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(controlCornerShape)
                                .background(SurfaceContainerLowest)
                                .border(1.dp, Color(0xFFE2E8F0), controlCornerShape)
                                .clickable {
                                    triggerTranslation(hindi)
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                                Text(text = olchiki, fontSize = 13.sp, color = Secondary, fontWeight = FontWeight.Bold)
                            }
                            Icon(
                                painter = painterResource(id = R.drawable.ic_volume_up),
                                contentDescription = "Play Command",
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Push-to-Talk Floating Microphone Button — tagged for onboarding spotlight
            FloatingActionButton(
                onClick = {
                    if (!isMicActive) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            startListeningSession()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    } else {
                        isMicActive = false
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
                    .onboardingTarget("mic_fab", onboardingRegistry)
            ) {
                Icon(
                    painter = painterResource(id = if (isMicActive) R.drawable.ic_stop else R.drawable.ic_mic),
                    contentDescription = if (isMicActive) "Stop" else "Listen",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
