package com.example.palashsetu.ui.screens.studio

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palashsetu.R
import com.example.palashsetu.data.local.FlashcardMotifRepository
import com.example.palashsetu.data.model.MotifCategory
import com.example.palashsetu.data.model.MotifItem
import com.example.palashsetu.domain.engine.AudioPlayerState
import com.example.palashsetu.domain.engine.DynamicMotifEngine
import com.example.palashsetu.domain.engine.PedagogicalAudioEngine
import com.example.palashsetu.theme.Primary
import com.example.palashsetu.theme.Secondary
import com.example.palashsetu.theme.SurfaceContainerLow
import com.example.palashsetu.theme.SurfaceContainerLowest
import kotlinx.coroutines.launch

@Composable
fun FlashcardStudioSection(
    isHindi: Boolean,
    audioEngine: PedagogicalAudioEngine,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var promptQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MotifCategory.ALL) }
    var selectedGradeFilter by remember { mutableStateOf<Int?>(null) } // null = all

    val motifs = remember(promptQuery, selectedCategory, selectedGradeFilter) {
        DynamicMotifEngine.resolveMotifsForPrompt(
            context = context,
            prompt = promptQuery,
            selectedCategory = selectedCategory,
            grade = selectedGradeFilter,
            limit = 60
        )
    }

    var cardIndex by remember { mutableStateOf(0) }
    var isCardFlipped by remember { mutableStateOf(false) }
    var isAudioPlaying by remember { mutableStateOf(false) }

    // Bounds check on cardIndex
    LaunchedEffect(motifs) {
        if (cardIndex >= motifs.size) {
            cardIndex = 0
        }
        isCardFlipped = false
    }

    val controlCornerShape = RoundedCornerShape(4.dp)
    val cardCornerShape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Title & Counter Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHindi) "पारस्परिक फ्लैशकार्ड स्टूडियो" else "Interactive Flashcard Studio",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary
                )
                Text(
                    text = if (isHindi) "१५० सांस्कृतिक रियलिया चित्र • ३D फ्लिप व संथाली ऑडियो" else "150 Cultural Realia Motifs • 3D Flip & Santali Audio",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Box(
                modifier = Modifier
                    .height(24.dp)
                    .clip(controlCornerShape)
                    .background(Primary.copy(alpha = 0.08f))
                    .border(1.dp, Primary.copy(alpha = 0.3f), controlCornerShape)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (motifs.isNotEmpty()) "${cardIndex + 1} / ${motifs.size}" else "0 / 0",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary,
                    maxLines = 1
                )
            }
        }

        // 1. Dynamic Context/Prompt Search Bar (BasicTextField, 44dp height, 4dp sharp corners)
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
                    contentDescription = "Search Prompt",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (promptQuery.isEmpty()) {
                        Text(
                            text = if (isHindi) "प्रॉम्प्ट या विषय (उदा. जंगली जानवर, ५ फल, शरीर के अंग)..." else "Prompt context (e.g. wild animals, fruits, body parts)...",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = promptQuery,
                        onValueChange = { promptQuery = it },
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
                if (promptQuery.isNotEmpty()) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Clear",
                        tint = Color(0xFF64748B),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { promptQuery = "" }
                    )
                }
            }
        }

        // 2. Grade Filter Chips (Intrinsic height Row, 32dp height, 4dp sharp corners)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                null to if (isHindi) "सभी कक्षाएं" else "All Grades",
                1 to if (isHindi) "कक्षा 1" else "Grade 1",
                2 to if (isHindi) "कक्षा 2" else "Grade 2",
                3 to if (isHindi) "कक्षा 3" else "Grade 3"
            ).forEach { (grade, label) ->
                val isSelected = selectedGradeFilter == grade
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
                        .clickable { selectedGradeFilter = grade },
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

        // 3. Category Filter Horizontal Pills (32dp height, 4dp sharp corners)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(MotifCategory.entries.toTypedArray()) { cat ->
                val isSelected = selectedCategory == cat
                val label = if (isHindi) cat.labelHi else cat.labelEn
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
                        .clickable {
                            selectedCategory = cat
                            cardIndex = 0
                        }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${cat.labelOlChiki} • $label",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF334155),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // 4. Interactive 3D Flip Card
        if (motifs.isNotEmpty()) {
            val currentMotif = motifs[cardIndex.coerceIn(0, motifs.size - 1)]

            val flipRotation by animateFloatAsState(
                targetValue = if (isCardFlipped) 180f else 0f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                label = "flashcard3dFlip"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .graphicsLayer {
                        rotationY = flipRotation
                        cameraDistance = 14f * density
                    }
                    .clickable { isCardFlipped = !isCardFlipped },
                shape = cardCornerShape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                if (flipRotation <= 90f) {
                    // FRONT FACE: Realia Image + Badges + Tap Hint
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        // Top Badges Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopStart),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(20.dp)
                                        .clip(controlCornerShape)
                                        .background(Primary.copy(alpha = 0.08f))
                                        .border(1.dp, Primary.copy(alpha = 0.2f), controlCornerShape)
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentMotif.category.labelOlChiki,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                    )
                                }

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
                                        text = "Grade ${currentMotif.grade}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // Audio Play Pill (Speaker)
                            Box(
                                modifier = Modifier
                                    .height(26.dp)
                                    .clip(controlCornerShape)
                                    .background(if (isAudioPlaying) Color(0xFFDC2626) else Secondary)
                                    .clickable {
                                        if (!isAudioPlaying) {
                                            coroutineScope.launch {
                                                isAudioPlaying = true
                                                audioEngine.playSynthesizedAudio(currentMotif.nameOlchiki, context = context).collect { state ->
                                                    if (state is AudioPlayerState.Finished || state is AudioPlayerState.Idle) {
                                                        isAudioPlaying = false
                                                    }
                                                }
                                            }
                                        } else {
                                            audioEngine.stopPlayback()
                                            isAudioPlaying = false
                                        }
                                    }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isAudioPlaying) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 1.5.dp)
                                    } else {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_volume_up),
                                            contentDescription = "Pronounce",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isHindi) "सुनें" else "Listen",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Realia Motif Bitmap Image (Aspect-Ratio Safe)
                        val bitmap = rememberAssetBitmap(currentMotif.assetPath)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.78f)
                                .align(Alignment.Center)
                                .padding(horizontal = 10.dp, vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = currentMotif.nameHi,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Primary)
                            }
                        }

                        // Bottom Flip Prompt
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_swap_horiz),
                                contentDescription = "Flip",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isHindi) "संथाली नाम व उच्चारण देखने के लिए कार्ड पर टैप करें" else "Tap card to reveal Ol Chiki name & pronunciation",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // BACK FACE: Counter-rotated so text is forward
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }
                            .padding(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Category Tag Badge
                            Box(
                                modifier = Modifier
                                    .height(22.dp)
                                    .clip(controlCornerShape)
                                    .background(Primary.copy(alpha = 0.08f))
                                    .border(1.dp, Primary.copy(alpha = 0.2f), controlCornerShape)
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${currentMotif.category.labelOlChiki} • ${if (isHindi) currentMotif.category.labelHi else currentMotif.category.labelEn}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            }

                            // Center Content: Ol Chiki + Devanagari Pronunciation + Hindi Meaning
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Prominent Ol Chiki Text
                                Text(
                                    text = currentMotif.nameOlchiki,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Primary,
                                    textAlign = TextAlign.Center
                                )

                                // Terracotta Devanagari Pronunciation Guide Badge
                                Box(
                                    modifier = Modifier
                                        .clip(controlCornerShape)
                                        .background(Color(0xFFFEF2F2))
                                        .border(1.dp, Color(0xFFFCA5A5), controlCornerShape)
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "उच्चारण: ${currentMotif.phoneticDeva}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB91C1C)
                                    )
                                }

                                // Hindi Definition
                                Text(
                                    text = currentMotif.nameHi,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B),
                                    textAlign = TextAlign.Center
                                )

                                // Search tags / keywords
                                Text(
                                    text = currentMotif.tags.take(4).joinToString(", "),
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Piper TTS Pronounce Action Button
                            Button(
                                onClick = {
                                    if (!isAudioPlaying) {
                                        coroutineScope.launch {
                                            isAudioPlaying = true
                                            audioEngine.playSynthesizedAudio(currentMotif.nameOlchiki, context = context).collect { state ->
                                                if (state is AudioPlayerState.Finished || state is AudioPlayerState.Idle) {
                                                    isAudioPlaying = false
                                                }
                                            }
                                        }
                                    } else {
                                        audioEngine.stopPlayback()
                                        isAudioPlaying = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(42.dp),
                                shape = controlCornerShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isAudioPlaying) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_volume_up),
                                            contentDescription = "Speak",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isHindi) "संथाली उच्चारण सुनें" else "Listen Santhali Speech",
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
                }
            }

            // 5. Carousel Navigation Controls (Intrinsic height Row, 44dp height, 4dp sharp corners)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Button
                OutlinedButton(
                    onClick = {
                        if (cardIndex > 0) cardIndex-- else cardIndex = motifs.size - 1
                        isCardFlipped = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = controlCornerShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF334155))
                ) {
                    Text(
                        text = if (isHindi) "◀ पिछला" else "◀ Prev",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Shuffle Button
                OutlinedButton(
                    onClick = {
                        cardIndex = (0 until motifs.size).random()
                        isCardFlipped = false
                    },
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight(),
                    shape = controlCornerShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF334155))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = "Shuffle",
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isHindi) "रैंडम" else "Shuffle",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Next Button
                Button(
                    onClick = {
                        if (cardIndex < motifs.size - 1) cardIndex++ else cardIndex = 0
                        isCardFlipped = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = controlCornerShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(
                        text = if (isHindi) "अगला ▶" else "Next ▶",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        } else {
            // No matching cards found for prompt
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = cardCornerShape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isHindi) "कोई मेल खाता रियलिया कार्ड नहीं मिला" else "No matching realia cards found",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = if (isHindi) "फ़िल्टर रीसेट करने के लिए प्रॉम्प्ट साफ़ करें" else "Clear prompt to reset filters",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Button(
                            onClick = {
                                promptQuery = ""
                                selectedCategory = MotifCategory.ALL
                                selectedGradeFilter = null
                            },
                            shape = controlCornerShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(text = if (isHindi) "सभी कार्ड देखें" else "View All Cards", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lightweight asset bitmap loader caching ImageBitmap.
 */
@Composable
fun rememberAssetBitmap(assetPath: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(assetPath) {
        try {
            context.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }
}
