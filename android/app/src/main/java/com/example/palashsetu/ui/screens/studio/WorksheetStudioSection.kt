package com.example.palashsetu.ui.screens.studio

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.palashsetu.R
import com.example.palashsetu.data.model.GeneratedWorksheet
import com.example.palashsetu.data.model.MotifCategory
import com.example.palashsetu.data.model.MotifItem
import com.example.palashsetu.data.model.NipunCompetency
import com.example.palashsetu.data.model.WorksheetActivityType
import com.example.palashsetu.data.model.WorksheetSpec
import com.example.palashsetu.domain.engine.AudioPlayerState
import com.example.palashsetu.domain.engine.DynamicMotifEngine
import com.example.palashsetu.domain.engine.PedagogicalAudioEngine
import com.example.palashsetu.domain.pdf.WorksheetPdfGenerator
import com.example.palashsetu.theme.Background
import com.example.palashsetu.theme.Primary
import com.example.palashsetu.theme.PrimaryContainer
import com.example.palashsetu.theme.Secondary
import com.example.palashsetu.theme.SurfaceContainerLow
import com.example.palashsetu.theme.SurfaceContainerLowest
import kotlinx.coroutines.launch
import java.io.File

/**
 * Interactive Dynamic Worksheet Studio:
 * Provides rich generation controls, an in-app live A4 worksheet canvas preview,
 * one-click instant regeneration, and synchronized PDF export.
 */
@Composable
fun WorksheetStudioSection(
    currentLanguage: String,
    isHindi: Boolean,
    selectedGrade: Int,
    selectedCompetency: NipunCompetency,
    onGradeChange: (Int) -> Unit,
    onChangeCompetencyClick: () -> Unit,
    audioEngine: PedagogicalAudioEngine,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val controlCornerShape = RoundedCornerShape(4.dp)
    val cardCornerShape = RoundedCornerShape(8.dp)

    // Dynamic Generation Controls State
    var selectedActivityType by remember { mutableStateOf(WorksheetActivityType.COMPREHENSIVE_FLN) }
    var selectedTheme by remember { mutableStateOf(MotifCategory.ALL) }
    var selectedMaxNumber by remember { mutableStateOf(if (selectedGrade == 1) 5 else if (selectedGrade == 2) 10 else 20) }
    var seed by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var isInstructionAudioPlaying by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var isRegeneratingAnim by remember { mutableStateOf(false) }

    // Master dynamically generated worksheet payload
    val worksheet: GeneratedWorksheet = remember(
        selectedGrade,
        selectedCompetency,
        selectedActivityType,
        selectedTheme,
        selectedMaxNumber,
        seed
    ) {
        val spec = WorksheetSpec(
            grade = selectedGrade,
            competency = selectedCompetency,
            activityType = selectedActivityType,
            topicTheme = selectedTheme,
            maxNumber = selectedMaxNumber,
            seed = seed
        )
        DynamicMotifEngine.generateWorksheet(context, spec)
    }

    // Animation rotation for regenerate button
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRegeneratingAnim) 360f else 0f,
        label = "RegenerateRotation"
    )

    fun reRollExercises() {
        seed = System.currentTimeMillis() + (1..10000).random()
        isRegeneratingAnim = !isRegeneratingAnim
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // =====================================================================
        // Section 1: Grade Selection Buttons (Grade 1, 2, 3)
        // =====================================================================
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
                        .clickable { onGradeChange(grade) },
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

        // =====================================================================
        // Section 2: Active Competency Selector Card
        // =====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChangeCompetencyClick() },
            shape = cardCornerShape,
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(22.dp)
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
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selectedCompetency.isNumeracy) Color(0xFF047857) else Color(0xFF1D4ED8),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = selectedCompetency.getTitle(isHindi),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selectedCompetency.getSubtitle(isHindi),
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

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

        // =====================================================================
        // Section 3: Dynamic Generation Controls Panel
        // =====================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = cardCornerShape,
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header of Control Panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "गतिशील कार्यपत्रक विकल्प" else "Dynamic Worksheet Controls",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        maxLines = 1,
                        softWrap = false
                    )

                    // Instant Re-Roll / Regenerate Button
                    Row(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(controlCornerShape)
                            .background(Primary)
                            .clickable { reRollExercises() }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = "Regenerate",
                            tint = Color.White,
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(rotationAngle)
                        )
                        Text(
                            text = if (isHindi) "पुनः उत्पन्न करें" else "Regenerate",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 1. Topic Selection Pills (Select Topic - No typing required!)
                Text(
                    text = if (isHindi) "विषय चुनें (Select Topic):" else "Select Topic:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        MotifCategory.ALL to (if (isHindi) "🎲 सभी विषय" else "🎲 All Topics"),
                        MotifCategory.NATURE to (if (isHindi) "🌿 प्रकृति व वनोपज" else "🌿 Nature & Forest"),
                        MotifCategory.ANIMALS to (if (isHindi) "🐾 पशु व पक्षी" else "🐾 Animals & Birds"),
                        MotifCategory.FRUITS_VEG_FOOD to (if (isHindi) "🍎 फल, सब्जी व भोजन" else "🍎 Fruits & Food"),
                        MotifCategory.CLASSROOM to (if (isHindi) "🏫 कक्षा व विद्यालय" else "🏫 School"),
                        MotifCategory.BODY_PARTS to (if (isHindi) "🖐️ शरीर के अंग" else "🖐️ Body Parts"),
                        MotifCategory.NUMERACY_SHAPES to (if (isHindi) "🔢 गिनती व आकार" else "🔢 Numeracy"),
                        MotifCategory.PEOPLE_ACTIONS to (if (isHindi) "👨‍👩‍👧 परिवार व क्रियाएं" else "👨‍👩‍👧 People & Actions")
                    ).forEach { (cat, label) ->
                        val isSelected = selectedTheme == cat
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(controlCornerShape)
                                .background(if (isSelected) Secondary else SurfaceContainerLowest)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Secondary else Color(0xFFCBD5E1),
                                    shape = controlCornerShape
                                )
                                .clickable { selectedTheme = cat }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF334155),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // 2. Activity Type Selection Pills (32dp height, 4dp sharp corners)
                Text(
                    text = if (isHindi) "अभ्यास का प्रकार (Activity Type):" else "Activity Type:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WorksheetActivityType.values().forEach { actType ->
                        val isSelected = selectedActivityType == actType
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(controlCornerShape)
                                .background(if (isSelected) Primary else Color(0xFFF1F5F9))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Primary else Color(0xFFCBD5E1),
                                    shape = controlCornerShape
                                )
                                .clickable { selectedActivityType = actType }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isHindi) actType.labelHi else actType.labelEn,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF334155),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // 3. Number Range Pills (when math is active)
                if (selectedActivityType == WorksheetActivityType.MATH_ADDITION || selectedActivityType == WorksheetActivityType.COMPREHENSIVE_FLN) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isHindi) "संख्या सीमा:" else "Max Sum:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        listOf(5, 10, 20).forEach { limitVal ->
                            val isSelected = selectedMaxNumber == limitVal
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(controlCornerShape)
                                    .background(if (isSelected) Color(0xFF047857) else Color(0xFFF1F5F9))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF047857) else Color(0xFFCBD5E1),
                                        shape = controlCornerShape
                                    )
                                    .clickable { selectedMaxNumber = limitVal }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isHindi) "$limitVal तक" else "Up to $limitVal",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF334155),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // Section 4: In-App Live A4 Worksheet Canvas Preview
        // =====================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = cardCornerShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // JCERT Live Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "झारखंड शैक्षिक अनुसंधान एवं प्रशिक्षण परिषद (JCERT)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isHindi) worksheet.titleHi else worksheet.titleEn,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // A4 Ready Badge
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .clip(controlCornerShape)
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A4 Preview",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Secondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Date & Student Line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "दिनांक: ${worksheet.generatedDate}" else "Date: ${worksheet.generatedDate}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = if (isHindi) "नाम: __________________" else "Name: __________________",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Bilingual Instruction Box with Piper Audio Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(controlCornerShape)
                        .background(SurfaceContainerLow)
                        .border(1.dp, Color(0xFFE2E8F0), controlCornerShape)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = worksheet.instructionOlchiki,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                lineHeight = 21.sp
                            )
                            Text(
                                text = worksheet.instructionHi,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B),
                                lineHeight = 18.sp
                            )
                        }

                        // Compact Audio Button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(controlCornerShape)
                                .background(if (isInstructionAudioPlaying) Color(0xFFDC2626) else Primary)
                                .clickable {
                                    if (!isInstructionAudioPlaying) {
                                        coroutineScope.launch {
                                            isInstructionAudioPlaying = true
                                            audioEngine.playSynthesizedAudio(worksheet.instructionOlchiki).collect { state ->
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

                // =============================================================
                // Dynamic Exercises Canvas Area
                // =============================================================

                // 1. Math Addition Exercise (Problem 1)
                if (worksheet.mathExercise != null) {
                    Text(
                        text = if (isHindi) "अभ्यास १: गिनो और जोड़ो (Realia Addition)" else "Exercise 1: Realia Count and Add",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    RealiaMathPreviewRow(context, worksheet.mathExercise, isHindi)
                }

                // 2. Math Addition Exercise (Problem 2)
                if (worksheet.mathExercise2 != null) {
                    Text(
                        text = if (isHindi) "अभ्यास २: गिनो और जोड़ो (Problem 2)" else "Exercise 2: Realia Count and Add",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    RealiaMathPreviewRow(context, worksheet.mathExercise2, isHindi)
                }

                // 3. Match the Column Exercise
                if (worksheet.matchingPairs != null && worksheet.scrambledLabels != null) {
                    Text(
                        text = if (isHindi) "अभ्यास: चित्र पहचान कर सही नाम से मिलान करें" else "Exercise: Match Picture to Word",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    MatchingColumnsPreview(
                        context = context,
                        pairs = worksheet.matchingPairs,
                        scrambled = worksheet.scrambledLabels,
                        isHindi = isHindi
                    )
                }

                // 4. Vocabulary Tracing Exercise
                if (worksheet.tracingItems != null) {
                    Text(
                        text = if (isHindi) "अभ्यास: चित्र पहचान कर शब्द व अक्षर सुंदर लिखें" else "Exercise: Word & Letter Tracing",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    VocabTracingPreview(context, worksheet.tracingItems, isHindi)
                }

                // Footer Teacher Evaluation & Signature
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "शिक्षक हस्ताक्षर: ____________" else "Teacher Sign: ____________",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = if (isHindi) "मूल्यांकन: [ A ]  [ B ]  [ C ]" else "Grade: [ A ]  [ B ]  [ C ]",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        }

        // =====================================================================
        // Section 5: Action Buttons (A4 PDF Download & Share)
        // =====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (!isGeneratingPdf) {
                        coroutineScope.launch {
                            isGeneratingPdf = true
                            val file = WorksheetPdfGenerator.generateWorksheetPdf(
                                context = context,
                                worksheet = worksheet
                            )
                            isGeneratingPdf = false
                            if (file != null) {
                                openPdfFile(context, file)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = controlCornerShape,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
            ) {
                if (isGeneratingPdf) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "तैयार हो रहा है..." else "Generating...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_print),
                        contentDescription = "Download PDF",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "A4 कार्यपत्रक डाउनलोड करें" else "Download A4 PDF",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        val file = WorksheetPdfGenerator.generateWorksheetPdf(
                            context = context,
                            worksheet = worksheet
                        )
                        if (file != null) {
                            sharePdfFile(context, file)
                        }
                    }
                },
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
                shape = controlCornerShape,
                border = BorderStroke(1.dp, Primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_share),
                    contentDescription = "Share",
                    tint = Primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isHindi) "साझा करें" else "Share",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

// =============================================================================
// Helper Composables for Preview Canvas
// =============================================================================

@Composable
private fun RealiaMathPreviewRow(
    context: Context,
    mathData: DynamicMotifEngine.MathExerciseData,
    isHindi: Boolean
) {
    val controlCornerShape = RoundedCornerShape(4.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(controlCornerShape)
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), controlCornerShape)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Card 1
        RealiaCountItem(context, mathData.item1, mathData.count1, isHindi)

        Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))

        // Card 2
        RealiaCountItem(context, mathData.item2, mathData.count2, isHindi)

        Text(text = "=", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))

        // Answer Box
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(controlCornerShape)
                    .background(Color.White)
                    .border(1.5.dp, Primary, controlCornerShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "?", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Primary)
            }
            Text(
                text = if (isHindi) "कुल योग" else "Total",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun RealiaCountItem(
    context: Context,
    item: MotifItem,
    count: Int,
    isHindi: Boolean
) {
    val bitmap = remember(item.assetPath) {
        loadBitmapFromAssets(context, item.assetPath)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(count.coerceAtMost(5)) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = item.nameHi,
                        modifier = Modifier.size(20.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_eco),
                        contentDescription = item.nameHi,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Text(
            text = item.nameOlchiki,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = "${if (isHindi) item.nameHi else item.id.replace("motif_", "")} [ $count ]",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF475569),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun MatchingColumnsPreview(
    context: Context,
    pairs: List<DynamicMotifEngine.MatchingPair>,
    scrambled: List<DynamicMotifEngine.MatchingPair>,
    isHindi: Boolean
) {
    val controlCornerShape = RoundedCornerShape(4.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(controlCornerShape)
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), controlCornerShape)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (i in pairs.indices) {
            val p = pairs[i]
            val s = scrambled[i]
            val bmp = remember(p.motif.assetPath) {
                loadBitmapFromAssets(context, p.motif.assetPath)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column A: Picture
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(controlCornerShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFCBD5E1), controlCornerShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = p.motif.nameHi,
                                modifier = Modifier.size(30.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155))
                    )
                }

                // Connector Spacer
                Text(
                    text = "• • •",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Column B: Ol Chiki Word & Devanagari guide
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = s.motif.nameOlchiki,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "${s.motif.nameHi} ${s.motif.phoneticDeva}",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VocabTracingPreview(
    context: Context,
    items: List<com.example.palashsetu.data.model.TracingItem>,
    isHindi: Boolean
) {
    val controlCornerShape = RoundedCornerShape(4.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(controlCornerShape)
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), controlCornerShape)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.take(3).forEach { item ->
            val bmp = remember(item.motif.assetPath) {
                loadBitmapFromAssets(context, item.motif.assetPath)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(controlCornerShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), controlCornerShape)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = item.hindiMeaning,
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Column {
                        Text(
                            text = item.olchikiWord,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "${item.hindiMeaning} ${item.devaPhonetic}",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Dotted Writing Box
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(26.dp)
                        .clip(controlCornerShape)
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFCBD5E1), controlCornerShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✍️ ᱚᱞ ᱢᱮ",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun loadBitmapFromAssets(context: Context, assetPath: String): ImageBitmap? {
    return try {
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    } catch (e: Exception) {
        null
    }
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
        Log.e("WorksheetStudio", "Error opening PDF: ${e.message}")
    }
}

private fun sharePdfFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Worksheet PDF"))
    } catch (e: Exception) {
        Log.e("WorksheetStudio", "Error sharing PDF: ${e.message}")
    }
}
