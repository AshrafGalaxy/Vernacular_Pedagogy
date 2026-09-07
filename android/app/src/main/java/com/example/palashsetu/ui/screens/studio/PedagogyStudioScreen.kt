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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palashsetu.domain.engine.PedagogicalAudioEngine
import com.example.palashsetu.theme.Background
import com.example.palashsetu.theme.Primary
import com.example.palashsetu.theme.PrimaryContainer
import com.example.palashsetu.theme.Secondary
import com.example.palashsetu.theme.SurfaceContainerHigh
import com.example.palashsetu.theme.SurfaceContainerLow
import com.example.palashsetu.theme.SurfaceContainerLowest
import com.example.palashsetu.ui.components.PalashTopBar
import kotlinx.coroutines.launch

@Composable
fun PedagogyStudioScreen(
    modifier: Modifier = Modifier
) {
    var selectedGrade by remember { mutableStateOf(2) }
    var selectedLanguage by remember { mutableStateOf("Santhali") }
    var isFlashcardMode by remember { mutableStateOf(false) }
    var isCardFlipped by remember { mutableStateOf(false) }
    var showPdfDownloadedNotification by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val audioEngine = remember { PedagogicalAudioEngine() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        PalashTopBar()

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Title
            Column {
                Text(
                    text = "शिक्षण व वर्कशीट स्टूडियो",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary
                )
                Text(
                    text = "Pedagogy Studio & Offline Printable Worksheets",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            // Grade Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1 to "कक्षा 1", 2 to "कक्षा 2 ✓", 3 to "कक्षा 3").forEach { (grade, label) ->
                    val isSelected = selectedGrade == grade
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Primary else SurfaceContainerLowest)
                            .border(1.dp, if (isSelected) Primary else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                            .clickable { selectedGrade = grade }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF1E293B)
                        )
                    }
                }
            }

            // Language Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Santhali" to "संथाली (Ol Chiki)", "Ho" to "हो (Warang Chiti)", "Mundari" to "मुंडारी (Bani)").forEach { (lang, label) ->
                    val isSelected = selectedLanguage == lang
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) PrimaryContainer else SurfaceContainerLow)
                            .clickable { selectedLanguage = lang }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF334155),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Worksheet Canvas Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "अभ्यास पत्रक (Worksheet Canvas • Grade $selectedGrade)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        Text(
                            text = "A4 B&W Ready",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Secondary,
                            modifier = Modifier
                                .background(SurfaceContainerLow, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Exercise Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceContainerLow, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "EN: Count the Mahua fruits and Sal leaves, write the total in boxes.",
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "HI: महुआ के फल और सखुआ के पत्ते गिनकर कुल संख्या लिखें।",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "OL CHIKI: ᱢᱟᱹᱦᱩᱣᱟᱹ ᱡᱚ ᱟᱨ ᱥᱟᱨᱡᱚᱢ ᱥᱟᱠᱟᱢ ᱞᱮᱠᱷᱟ ᱠᱟᱛᱮ ᱡᱚᱛᱚ ᱮᱞ ᱚᱞ ᱢᱮ᱾",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }

                    // Visual Counting Simulation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🍃 🍃 🍃", fontSize = 20.sp)
                            Text(text = "ᱥᱟᱨᱡᱚᱢ (Sal)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Primary)
                            Text(text = "[ 3 ]", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Secondary)
                        }
                        Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🌰 🌰", fontSize = 20.sp)
                            Text(text = "ᱢᱟᱹᱦᱩᱣᱟᱹ (Mahua)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Primary)
                            Text(text = "[ 2 ]", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Secondary)
                        }
                        Text(text = "=", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "❓", fontSize = 20.sp)
                            Text(text = "ᱢᱚᱬᱮ (Five)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Primary)
                            Text(text = "[ ? ]", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
                        }
                    }

                    // Action Buttons
                    Button(
                        onClick = { showPdfDownloadedNotification = true },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text(text = "🖨️ प्रिंट योग्य B&W PDF डाउनलोड (Offline Canvas)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { isFlashcardMode = !isFlashcardMode },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isFlashcardMode) "बंद करें (Close Flashcard)" else "🎴 इंटरएक्टिव डिजिटल फ्लैशकार्ड खोलें (Audio Cards)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }

                    AnimatedVisibility(visible = showPdfDownloadedNotification) {
                        Text(
                            text = "✓ PDF सफलतापूर्वक स्थानीय मेमोरी में डाउनलोड हो गया (Ready for Offline Print)",
                            fontSize = 11.sp,
                            color = Color(0xFF1B5E20),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        )
                    }
                }
            }

            // Interactive Flashcard Mode
            AnimatedVisibility(visible = isFlashcardMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { isCardFlipped = !isCardFlipped },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isCardFlipped) "कार्ड पृष्ठ (Back Side) - क्लिक करके पलटें" else "कार्ड सम्मुख (Front Side) - क्लिक करके पलटें",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        if (!isCardFlipped) {
                            Text(text = "🌳", fontSize = 48.sp)
                            Text(
                                text = "ᱥᱟᱨᱡᱚᱢ ᱫᱟᱨᱮ",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                            Text(text = "[Sarjom Dare - Sal Tree]", fontSize = 13.sp, color = Secondary)
                        } else {
                            Text(
                                text = "सखुआ / साल का पेड़",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Sal Tree (Shorea robusta)",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "झारखंड का राज्य वृक्ष (State Tree of Jharkhand)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Secondary
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    audioEngine.playSynthesizedAudio("ᱥᱟᱨᱡᱚᱢ ᱫᱟᱨᱮ").collect {}
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "🔊 उच्चारण सुनें (Pronounce)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
