package com.example.palashsetu.ui.screens.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.example.palashsetu.R
import com.example.palashsetu.data.local.NipunCurriculumRepository
import com.example.palashsetu.data.local.UserSessionManager
import com.example.palashsetu.data.model.NipunCompetency
import com.example.palashsetu.domain.engine.AudioPlayerState
import com.example.palashsetu.domain.engine.PedagogicalAudioEngine
import com.example.palashsetu.domain.pdf.WorksheetPdfGenerator
import com.example.palashsetu.theme.Background
import com.example.palashsetu.theme.Primary
import com.example.palashsetu.theme.PrimaryContainer
import com.example.palashsetu.theme.Secondary
import com.example.palashsetu.theme.SurfaceContainerHigh
import com.example.palashsetu.theme.SurfaceContainerLow
import com.example.palashsetu.theme.SurfaceContainerLowest
import com.example.palashsetu.ui.components.PalashTopBar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

enum class StudioMode {
    WORKSHEET, FLASHCARDS
}

private fun openPdfFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open PDF"))
    } catch (e: Exception) {
        Log.e("PedagogyStudio", "Error opening PDF: ${e.message}")
    }
}

@Composable
fun PedagogyStudioScreen(
    currentLanguage: String = UserSessionManager.getLanguage(LocalContext.current),
    onLanguageToggle: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isHindi = currentLanguage == "hi"

    // Master list of all 24 NIPUN Bharat competencies
    val allCompetencies = remember {
        NipunCurriculumRepository.getAllCompetencies(context)
    }

    var selectedGrade by remember { mutableStateOf(2) }
    var selectedCompetency by remember {
        mutableStateOf(NipunCurriculumRepository.getDefaultCompetency(context, 2))
    }

    // Modal dialog state for advanced competency selection
    var isSelectorDialogOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterGrade by remember { mutableStateOf<Int?>(null) } // null = All grades
    var filterDomain by remember { mutableStateOf<String?>(null) } // null = All domains

    val filteredCompetencies = remember(allCompetencies, filterGrade, filterDomain, searchQuery) {
        NipunCurriculumRepository.filterCompetencies(
            context = context,
            grade = filterGrade,
            domain = filterDomain,
            searchQuery = searchQuery
        )
    }

    // When top grade button is toggled, update competency if current is not in that grade
    LaunchedEffect(selectedGrade) {
        if (selectedCompetency.grade != selectedGrade) {
            selectedCompetency = NipunCurriculumRepository.getDefaultCompetency(context, selectedGrade)
        }
    }

    var selectedStudioMode by remember { mutableStateOf(StudioMode.WORKSHEET) }
    var isInstructionAudioPlaying by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val audioEngine = remember { PedagogicalAudioEngine(context) }

    LaunchedEffect(Unit) {
        audioEngine.warmUp()
    }

    val controlCornerShape = RoundedCornerShape(4.dp)
    val cardCornerShape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        PalashTopBar(
            currentLanguage = currentLanguage,
            onLanguageToggle = onLanguageToggle
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Screen Header
            Column {
                Text(
                    text = if (isHindi) "स्टूडियो" else "Studio",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary
                )
                Text(
                    text = if (isHindi) "ऑफलाइन प्रिंटेबल वर्कशीट व शिक्षण स्टूडियो" else "Pedagogy Studio & Offline Printable Worksheets",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            // Studio Mode Switcher (Worksheet vs Flashcards)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    StudioMode.WORKSHEET to if (isHindi) "कार्यपत्रक (Worksheets)" else "Worksheets",
                    StudioMode.FLASHCARDS to if (isHindi) "फ्लैशकार्ड (Flashcards)" else "Flashcards"
                ).forEach { (mode, label) ->
                    val isSelected = selectedStudioMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(controlCornerShape)
                            .background(if (isSelected) Primary else SurfaceContainerLowest)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Primary else Color(0xFFCBD5E1),
                                shape = controlCornerShape
                            )
                            .clickable { selectedStudioMode = mode },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF1E293B),
                                maxLines = 1,
                                softWrap = false
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = "Active",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (selectedStudioMode == StudioMode.WORKSHEET) {

            // Section 1: Grade Selection Buttons (Grade 1, Grade 2, Grade 3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(1 to "कक्षा 1", 2 to "कक्षा 2", 3 to "कक्षा 3").forEach { (grade, label) ->
                    val isSelected = selectedGrade == grade
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(controlCornerShape)
                            .background(if (isSelected) Primary else SurfaceContainerLowest)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Primary else Color(0xFFCBD5E1),
                                shape = controlCornerShape
                            )
                            .clickable {
                                selectedGrade = grade
                                filterGrade = grade // Synchronize dialog filter default
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isHindi) label else "Grade $grade",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF1E293B),
                                maxLines = 1,
                                softWrap = false
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Active Vernacular Script Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(controlCornerShape)
                        .background(PrimaryContainer)
                        .border(1.dp, Primary, controlCornerShape),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check_circle),
                            contentDescription = "Active Language",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isHindi) "ᱥᱟᱱᱛᱟᱲᱤ • संथाली (ओल चिकी)" else "ᱥᱟᱱᱛᱟᱲᱤ • Santali (Ol Chiki)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Section 3: NIPUN Bharat Competency Level Selector (All 24 Outcomes)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "निपुण भारत दक्षता स्तर:" else "NIPUN Bharat Competency:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .clip(controlCornerShape)
                            .background(SurfaceContainerLow)
                            .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isHindi) "24 लक्ष्य उपलब्ध" else "24 Goals Available",
                            fontSize = 10.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Dropdown Trigger Container (Sharp 4dp, 52dp height, bilingual layout)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(controlCornerShape)
                        .background(SurfaceContainerLowest)
                        .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                        .clickable {
                            filterGrade = selectedGrade // Default filter to current grade for quick access
                            filterDomain = null
                            searchQuery = ""
                            isSelectorDialogOpen = true
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Domain Code Badge (strict 20dp height, 4dp sharp corners)
                            Box(
                                modifier = Modifier
                                    .height(20.dp)
                                    .clip(controlCornerShape)
                                    .background(if (selectedCompetency.isNumeracy) Color(0xFFECFDF5) else Color(0xFFEFF6FF))
                                    .border(
                                        1.dp,
                                        if (selectedCompetency.isNumeracy) Color(0xFFA7F3D0) else Color(0xFFBFDBFE),
                                        controlCornerShape
                                    )
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedCompetency.code,
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selectedCompetency.isNumeracy) Color(0xFF047857) else Color(0xFF1D4ED8),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // Grade Badge (strict 20dp height, 4dp sharp corners)
                            Box(
                                modifier = Modifier
                                    .height(20.dp)
                                    .clip(controlCornerShape)
                                    .background(Color(0xFFF1F5F9))
                                    .border(1.dp, Color(0xFFE2E8F0), controlCornerShape)
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedCompetency.getGradeLabel(isHindi),
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // Title & Subtitle (Bilingual)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text(
                                    text = selectedCompetency.getTitle(isHindi),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = selectedCompetency.getSubtitle(isHindi),
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Trailing Action Pill (strict 26dp height, 4dp sharp corners)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .height(26.dp)
                                .clip(controlCornerShape)
                                .background(Primary.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = if (isHindi) "बदलें" else "Change",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_expand_more),
                                contentDescription = "Open Selector",
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Section 4: Live Interactive Printable Worksheet Canvas
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = cardCornerShape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = if (isHindi) "अभ्यास पत्रक (कक्षा $selectedGrade)" else "Worksheet Canvas (Grade $selectedGrade)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                maxLines = 1,
                                softWrap = false
                            )
                            // Code badge: strict 20dp height, 4dp sharp corners, single line
                            Box(
                                modifier = Modifier
                                    .height(20.dp)
                                    .clip(controlCornerShape)
                                    .background(if (selectedCompetency.isNumeracy) Color(0xFFECFDF5) else Color(0xFFEFF6FF))
                                    .border(
                                        1.dp,
                                        if (selectedCompetency.isNumeracy) Color(0xFFA7F3D0) else Color(0xFFBFDBFE),
                                        controlCornerShape
                                    )
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedCompetency.code,
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selectedCompetency.isNumeracy) Color(0xFF047857) else Color(0xFF1D4ED8),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // A4 B&W Ready Badge: strict 20dp height, 4dp sharp corners, single line
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .clip(controlCornerShape)
                                .background(SurfaceContainerLow)
                                .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isHindi) "A4 प्रिंट रेडी" else "A4 B&W Ready",
                                fontSize = 10.sp,
                                lineHeight = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Secondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Date & Student Name Header (Bilingual)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "दिनांक: ०८/०९/२०२६" else "Date: 08/09/2026",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = if (isHindi) "छात्र का नाम: ____________" else "Student Name: ____________",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Vernacular Ol Chiki & Hindi Instruction Box with Piper Audio Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(controlCornerShape)
                            .background(SurfaceContainerLow)
                            .border(1.dp, Color(0xFFE2E8F0), controlCornerShape)
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = selectedCompetency.instructionOlchiki.ifBlank { "ᱢᱟᱹᱦᱩᱣᱟᱹ ᱡᱚ ᱟᱨ ᱥᱟᱨᱡᱚᱢ ᱥᱟᱠᱟᱢ ᱞᱮᱠᱷᱟ ᱠᱟᱛᱮ ᱡᱚᱛᱚ ᱮᱞ ᱚᱞ ᱢᱮ᱾" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary,
                                    lineHeight = 23.sp
                                )
                                Text(
                                    text = selectedCompetency.instructionHi.ifBlank { "महुआ के फल और सखुआ के पत्ते गिनकर कुल संख्या लिखें।" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B),
                                    lineHeight = 21.sp
                                )
                            }

                            // Compact Piper Speech Playback Button (28dp, 4dp sharp corners)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(controlCornerShape)
                                    .background(if (isInstructionAudioPlaying) Color(0xFFDC2626) else Primary)
                                    .clickable {
                                        if (!isInstructionAudioPlaying) {
                                            val textToSpeak = selectedCompetency.instructionOlchiki.ifBlank {
                                                "ᱢᱟᱹᱦᱩᱣᱟᱹ ᱡᱚ ᱟᱨ ᱥᱟᱨᱡᱚᱢ ᱥᱟᱠᱟᱢ ᱞᱮᱠᱷᱟ ᱠᱟᱛᱮ ᱡᱚᱛᱚ ᱮᱞ ᱚᱞ ᱢᱮ᱾"
                                            }
                                            coroutineScope.launch {
                                                isInstructionAudioPlaying = true
                                                audioEngine.playSynthesizedAudio(textToSpeak).collect { state ->
                                                    if (state is AudioPlayerState.Finished || state is AudioPlayerState.Idle) {
                                                        isInstructionAudioPlaying = false
                                                    }
                                                }
                                            }
                                        } else {
                                            audioEngine.stopPlayback()
                                            isInstructionAudioPlaying = false
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isInstructionAudioPlaying) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 1.5.dp
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_volume_up),
                                        contentDescription = "Speak Instruction",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Concrete Realia Visual Area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(controlCornerShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), controlCornerShape)
                            .padding(horizontal = 10.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(3) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_eco),
                                        contentDescription = "Sal Leaf",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = "ᱥᱟᱨᱡᱚᱢ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = if (isHindi) "सखुआ" else "Sal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "[ 3 ]",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Secondary
                            )
                        }
                        Text(
                            text = "+",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(2) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_grain),
                                        contentDescription = "Mahua Fruit",
                                        tint = Color(0xFF8D6E63),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = "ᱢᱟᱹᱦᱩᱣᱟᱹ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = if (isHindi) "महुआ" else "Mahua",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "[ 2 ]",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Secondary
                            )
                        }
                        Text(
                            text = "=",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_help_outline),
                                contentDescription = "Question",
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "ᱢᱚᱬᱮ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = if (isHindi) "पाँच" else "Five",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "[ ? ]",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }

                    // Printable Worksheet PDF Download Button
                    Button(
                        onClick = {
                            if (!isGeneratingPdf) {
                                coroutineScope.launch {
                                    isGeneratingPdf = true
                                    val file = WorksheetPdfGenerator.generateWorksheetPdf(
                                        context = context,
                                        grade = selectedGrade,
                                        isHindi = isHindi,
                                        competency = selectedCompetency
                                    )
                                    isGeneratingPdf = false
                                    if (file != null) {
                                        openPdfFile(context, file)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = controlCornerShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isGeneratingPdf) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_print),
                                    contentDescription = "Download Worksheet",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = if (isHindi) "कार्यपत्रक डाउनलोड करें (A4 PDF)" else "Download Worksheet (A4 PDF)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        } else {
            // Interactive Realia Flashcard Studio Mode
            FlashcardStudioSection(
                isHindi = isHindi,
                audioEngine = audioEngine
            )
        }
        }
    }

    // =========================================================================
    // ADVANCED NIPUN BHARAT COMPETENCY SELECTION MODAL DIALOG (24 OUTCOMES)
    // =========================================================================
    if (isSelectorDialogOpen) {
        Dialog(
            onDismissRequest = {
                isSelectorDialogOpen = false
                searchQuery = ""
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.86f),
                shape = cardCornerShape, // Stitch 8dp container standard
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Modal Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "निपुण भारत दक्षता चयन (24 लक्ष्य)" else "NIPUN Bharat Competencies (24 Outcomes)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Primary
                            )
                            Text(
                                text = if (isHindi) "कक्षा 1 से 3 • जेसीईआरटी पलाश पाठ्यक्रम" else "Grades 1–3 • JCERT PALASH Primary Curriculum",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(controlCornerShape)
                                .background(Color(0xFFF1F5F9))
                                .clickable {
                                    isSelectorDialogOpen = false
                                    searchQuery = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Close",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 2. Compact Search Input (BasicTextField, 44dp, Sharp 4dp Corners)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(controlCornerShape)
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = "Search",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = if (isHindi) "कोड या विषय खोजें (उदा. M2.4, L1.1, जोड़)..." else "Search code or topic (e.g. M2.4, Addition)...",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(Primary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = "Clear",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { searchQuery = "" }
                                )
                            }
                        }
                    }

                    // 3. Quick Grade Filter Chips (All 24, Grade 1, Grade 2, Grade 3)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val gradeFilters = listOf(
                            null to if (isHindi) "सभी (24)" else "All (24)",
                            1 to if (isHindi) "कक्षा 1 (8)" else "Grade 1 (8)",
                            2 to if (isHindi) "कक्षा 2 (8)" else "Grade 2 (8)",
                            3 to if (isHindi) "कक्षा 3 (8)" else "Grade 3 (8)"
                        )
                        gradeFilters.forEach { (g, label) ->
                            val isSelected = filterGrade == g
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(controlCornerShape)
                                    .background(if (isSelected) Primary else Color(0xFFF1F5F9))
                                    .border(
                                        1.dp,
                                        if (isSelected) Primary else Color(0xFFE2E8F0),
                                        controlCornerShape
                                    )
                                    .clickable { filterGrade = g },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF475569),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Domain Filter Sub-Chips (All Domains, Literacy, Numeracy)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val domainFilters = listOf(
                            null to if (isHindi) "सभी विषय" else "All Domains",
                            "LITERACY" to if (isHindi) "भाषा • साक्षरता (12)" else "Literacy (12)",
                            "NUMERACY" to if (isHindi) "गणित • संख्या ज्ञान (12)" else "Numeracy (12)"
                        )
                        domainFilters.forEach { (d, label) ->
                            val isSelected = filterDomain == d
                            val activeColor = if (d == "NUMERACY") Color(0xFF047857) else Primary
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .clip(controlCornerShape)
                                    .background(if (isSelected) activeColor.copy(alpha = 0.12f) else Color.White)
                                    .border(
                                        1.dp,
                                        if (isSelected) activeColor else Color(0xFFE2E8F0),
                                        controlCornerShape
                                    )
                                    .clickable { filterDomain = d },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) activeColor else Color(0xFF64748B),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // 4. Results Count Label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "उपलब्ध दक्षता परिणाम:" else "Available Competency Outcomes:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = if (isHindi) "${filteredCompetencies.size} लक्ष्य" else "${filteredCompetencies.size} Goals",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }

                    // 5. Scrollable List (LazyColumn - Smooth scrolling across all 24 competencies)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (filteredCompetencies.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isHindi) "कोई दक्षता नहीं मिली। कृपया खोज शब्द बदलें।" else "No competencies found. Try another search query.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        } else {
                            items(filteredCompetencies, key = { it.code }) { comp ->
                                val isSelected = comp.code == selectedCompetency.code
                                val domainColor = if (comp.isNumeracy) Color(0xFF047857) else Color(0xFF1D4ED8)
                                val domainBg = if (comp.isNumeracy) Color(0xFFECFDF5) else Color(0xFFEFF6FF)
                                val domainBorder = if (comp.isNumeracy) Color(0xFFA7F3D0) else Color(0xFFBFDBFE)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(controlCornerShape)
                                        .background(if (isSelected) Primary.copy(alpha = 0.05f) else Color.White)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) Primary else Color(0xFFE2E8F0),
                                            shape = controlCornerShape
                                        )
                                        .clickable {
                                            selectedCompetency = comp
                                            selectedGrade = comp.grade
                                            isSelectorDialogOpen = false
                                            searchQuery = ""
                                        }
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Code & Grade Badges Column
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .height(20.dp)
                                                    .clip(controlCornerShape)
                                                    .background(domainBg)
                                                    .border(1.dp, domainBorder, controlCornerShape)
                                                    .padding(horizontal = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = comp.code,
                                                    fontSize = 10.sp,
                                                    lineHeight = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = domainColor,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                            Text(
                                                text = comp.getGradeLabel(isHindi),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B)
                                            )
                                        }

                                        // Details Column (Bilingual Title, Subtitle, Instructions)
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = comp.getTitle(isHindi),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Primary else Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = comp.getSubtitle(isHindi),
                                                fontSize = 11.sp,
                                                color = Color(0xFF475569),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            // Vernacular Ol Chiki Instruction Preview
                                            Text(
                                                text = "ᱚᱞ ᱪᱤᱠᱤ: ${comp.instructionOlchiki}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Selection Checkmark
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(controlCornerShape)
                                                    .background(Primary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_check),
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
