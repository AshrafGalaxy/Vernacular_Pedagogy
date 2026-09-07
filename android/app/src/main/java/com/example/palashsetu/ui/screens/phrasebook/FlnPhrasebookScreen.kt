package com.example.palashsetu.ui.screens.phrasebook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palashsetu.R
import com.example.palashsetu.data.local.FlnRepository
import com.example.palashsetu.data.local.UserSessionManager
import com.example.palashsetu.data.model.FlnPhrase
import com.example.palashsetu.domain.engine.AudioPlayerState
import com.example.palashsetu.domain.engine.PedagogicalAudioEngine
import com.example.palashsetu.theme.Background
import com.example.palashsetu.theme.Primary
import com.example.palashsetu.theme.PrimaryContainer
import com.example.palashsetu.theme.Secondary
import com.example.palashsetu.theme.SecondaryFixed
import com.example.palashsetu.theme.SurfaceContainerLow
import com.example.palashsetu.theme.SurfaceContainerLowest
import com.example.palashsetu.ui.components.PalashTopBar
import kotlinx.coroutines.launch

data class PhraseCategory(val id: String, val hiLabel: String, val enLabel: String)

@Composable
fun FlnPhrasebookScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isHindi = UserSessionManager.getLanguage(context) == "hi"

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("ALL") }
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val audioEngine = remember { PedagogicalAudioEngine() }

    val categories = listOf(
        PhraseCategory("ALL", "सभी", "All"),
        PhraseCategory("कक्षा प्रबंधन", "कक्षा प्रबंधन", "Classroom"),
        PhraseCategory("प्रशंसा व प्रोत्साहन", "प्रशंसा", "Praise"),
        PhraseCategory("अनुशासन", "अनुशासन", "Discipline"),
        PhraseCategory("गतिविधि", "गतिविधि", "Activity"),
        PhraseCategory("गिनती व गणित", "गिनती व गणित", "Math & Numbers"),
        PhraseCategory("अभिवादन", "अभिवादन", "Greetings")
    )

    val phrases = remember(searchQuery, selectedCategoryId) {
        val base = if (selectedCategoryId == "ALL") {
            FlnRepository.getAllPhrases()
        } else {
            FlnRepository.getPhrasesByCategory(selectedCategoryId)
        }
        if (searchQuery.isBlank()) {
            base
        } else {
            val q = searchQuery.trim().lowercase()
            base.filter {
                it.hindi.lowercase().contains(q) ||
                it.olchiki.contains(q) ||
                it.english.lowercase().contains(q) ||
                it.phoneticDevanagari.lowercase().contains(q)
            }
        }
    }

    fun playPhrase(phrase: FlnPhrase) {
        coroutineScope.launch {
            currentlyPlayingId = phrase.id
            audioEngine.playSynthesizedAudio(phrase.olchiki).collect { state ->
                if (state is AudioPlayerState.Finished || state is AudioPlayerState.Idle) {
                    currentlyPlayingId = null
                }
            }
        }
    }

    val controlCornerShape = RoundedCornerShape(4.dp)
    val cardCornerShape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        PalashTopBar()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Title Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = if (isHindi) "FLN त्वरित शब्दावली" else "FLN Rapid Phrasebook",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Primary
                    )
                    Text(
                        text = if (isHindi) "NIPUN भारत कक्षा 1–3 सत्यापित ध्वनि-बैंक" else "NIPUN Bharat Grade 1–3 Verified Soundbank",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Text(
                    text = if (isHindi) "${phrases.size} वाक्यांश" else "${phrases.size} Phrases",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Secondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .clip(controlCornerShape)
                        .background(SurfaceContainerLow)
                        .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            // Search input with Vector Icon & Sharp 4dp Geometry
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (isHindi) "खोजें (e.g. किताब, sit down, ᱯᱩᱛᱷᱤ...)" else "Search (e.g. book, sit down, ᱯᱩᱛᱷᱤ...)",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = "Search",
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = controlCornerShape
            )

            // Category pills row with Sharp 4dp Geometry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = cat.id == selectedCategoryId
                    Box(
                        modifier = Modifier
                            .clip(controlCornerShape)
                            .background(if (isSelected) Primary else SurfaceContainerLowest)
                            .border(1.dp, if (isSelected) Primary else Color(0xFFE2E8F0), controlCornerShape)
                            .clickable { selectedCategoryId = cat.id }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isHindi) cat.hiLabel else cat.enLabel,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF334155)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Phrases List (Stitch 8dp Cards + Vector Audio Controls)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(phrases, key = { it.id }) { phrase ->
                    val isPlaying = currentlyPlayingId == phrase.id
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardCornerShape,
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = phrase.category,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Secondary,
                                        modifier = Modifier
                                            .background(SecondaryFixed.copy(alpha = 0.4f), controlCornerShape)
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                    Text(
                                        text = "Grade ${phrase.grade}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = phrase.hindi,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = phrase.olchiki,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = phrase.phoneticDevanagari,
                                    fontSize = 12.sp,
                                    color = Color(0xFF663500)
                                )
                                Text(
                                    text = "EN: ${phrase.english}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            // Vector Audio Action Button (Sharp 4dp Geometry)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(controlCornerShape)
                                    .background(if (isPlaying) Color(0xFFDC2626) else Primary)
                                    .clickable { playPhrase(phrase) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
