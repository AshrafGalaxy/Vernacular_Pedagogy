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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palashsetu.data.local.FlnRepository
import com.example.palashsetu.data.model.FlnPhrase
import com.example.palashsetu.domain.engine.AudioPlayerState
import com.example.palashsetu.domain.engine.PedagogicalAudioEngine
import com.example.palashsetu.theme.Background
import com.example.palashsetu.theme.Primary
import com.example.palashsetu.theme.PrimaryContainer
import com.example.palashsetu.theme.Secondary
import com.example.palashsetu.theme.SecondaryFixed
import com.example.palashsetu.theme.SurfaceContainerHigh
import com.example.palashsetu.theme.SurfaceContainerLow
import com.example.palashsetu.theme.SurfaceContainerLowest
import com.example.palashsetu.ui.components.PalashTopBar
import kotlinx.coroutines.launch

@Composable
fun FlnPhrasebookScreen(
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("सभी (All)") }
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val audioEngine = remember { PedagogicalAudioEngine() }

    val categories = listOf(
        "सभी (All)",
        "कक्षा प्रबंधन",
        "प्रशंसा व प्रोत्साहन",
        "अनुशासन",
        "गतिविधि",
        "गिनती व गणित",
        "अभिवादन"
    )

    val phrases = remember(searchQuery, selectedCategory) {
        val base = if (selectedCategory == "सभी (All)") {
            FlnRepository.getAllPhrases()
        } else {
            FlnRepository.getPhrasesByCategory(selectedCategory)
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
                Column {
                    Text(
                        text = "FLN त्वरित शब्दावली",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Primary
                    )
                    Text(
                        text = "NIPUN Bharat Grade 1–3 Verified Soundbank",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Text(
                    text = "${phrases.size} वाक्यांश",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Secondary,
                    modifier = Modifier
                        .background(SurfaceContainerLow, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("खोजें (e.g. किताब, sit down, ᱯᱩᱛᱷᱤ...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Category pills row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Primary else SurfaceContainerLowest)
                            .border(1.dp, if (isSelected) Primary else Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF334155)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Phrases List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(phrases, key = { it.id }) { phrase ->
                    val isPlaying = currentlyPlayingId == phrase.id
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
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
                                            .background(SecondaryFixed.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
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

                            // Play button
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) Color(0xFFDC2626) else PrimaryContainer)
                                    .clickable { playPhrase(phrase) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isPlaying) "⏸" else "▶",
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
