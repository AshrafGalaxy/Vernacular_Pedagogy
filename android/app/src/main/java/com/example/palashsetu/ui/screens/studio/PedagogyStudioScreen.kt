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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palashsetu.R
import com.example.palashsetu.data.local.UserSessionManager
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
    currentLanguage: String = UserSessionManager.getLanguage(LocalContext.current),
    onLanguageToggle: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isHindi = currentLanguage == "hi"

    var selectedGrade by remember { mutableStateOf(2) }
    var isFlashcardMode by remember { mutableStateOf(false) }
    var isCardFlipped by remember { mutableStateOf(false) }
    var showPdfDownloadedNotification by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val audioEngine = remember { PedagogicalAudioEngine(context) }

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
                            .clickable { selectedGrade = grade },
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = cardCornerShape,
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                            text = if (isHindi) "अभ्यास पत्रक (कक्षा $selectedGrade)" else "Worksheet Canvas (Grade $selectedGrade)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        Text(
                            text = if (isHindi) "A4 प्रिंट हेतु तैयार" else "A4 B&W Ready",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Secondary,
                            modifier = Modifier
                                .clip(controlCornerShape)
                                .background(SurfaceContainerLow)
                                .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Date & Student Name Header (Authentic Vernacular Primary Format)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "दिनांक: ०८/०९/२०२६",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "छात्र का नाम: ____________",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(controlCornerShape)
                            .background(SurfaceContainerLow)
                            .border(1.dp, Color(0xFFE2E8F0), controlCornerShape)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "ᱢᱟᱹᱦᱩᱣᱟᱹ ᱡᱚ ᱟᱨ ᱥᱟᱨᱡᱚᱢ ᱥᱟᱠᱟᱢ ᱞᱮᱠᱷᱟ ᱠᱟᱛᱮ ᱡᱚᱛᱚ ᱮᱞ ᱚᱞ ᱢᱮ᱾",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                lineHeight = 23.sp
                            )
                            Text(
                                text = "महुआ के फल और सखुआ के पत्ते गिनकर कुल संख्या लिखें।",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B),
                                lineHeight = 22.sp
                            )
                        }
                    }

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
                                text = "सखुआ",
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
                                text = "महुआ",
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
                                text = "पाँच",
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

                    Button(
                        onClick = { showPdfDownloadedNotification = true },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = controlCornerShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_print),
                                contentDescription = "Print PDF",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isHindi) "प्रिंट योग्य B&W PDF डाउनलोड" else "Download Printable B&W PDF",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { isFlashcardMode = !isFlashcardMode },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = controlCornerShape
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_studio),
                                contentDescription = "Flashcards",
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isFlashcardMode) {
                                    if (isHindi) "फ्लैशकार्ड बंद करें" else "Close Flashcard"
                                } else {
                                    if (isHindi) "इंटरएक्टिव ऑडियो फ्लैशकार्ड खोलें" else "Open Audio Flashcards"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    AnimatedVisibility(visible = showPdfDownloadedNotification) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(controlCornerShape)
                                .background(Color(0xFFE8F5E9))
                                .border(1.dp, Color(0xFFC8E6C9), controlCornerShape)
                                .padding(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check_circle),
                                contentDescription = "Success",
                                tint = Color(0xFF1B5E20),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isHindi) "PDF सफलतापूर्वक स्थानीय मेमोरी में डाउनलोड हो गया (Ready for Offline Print)" else "PDF downloaded to local storage (Ready for Offline Print)",
                                fontSize = 11.sp,
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isFlashcardMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardCornerShape)
                        .clickable { isCardFlipped = !isCardFlipped },
                    shape = cardCornerShape,
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isCardFlipped) {
                                if (isHindi) "कार्ड पृष्ठ - क्लिक करके पलटें" else "Card Back - Tap to flip"
                            } else {
                                if (isHindi) "कार्ड सम्मुख - क्लिक करके पलटें" else "Card Front - Tap to flip"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        if (!isCardFlipped) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_park),
                                contentDescription = "Sal Tree",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = "ᱥᱟᱨᱡᱚᱢ ᱫᱟᱨᱮ",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                            Text(
                                text = if (isHindi) "[सारजोम दारे - सखुआ का पेड़]" else "[Sarjom Dare - Sal Tree]",
                                fontSize = 13.sp,
                                color = Secondary
                            )
                        } else {
                            Text(
                                text = if (isHindi) "सखुआ / साल का पेड़" else "Sal Tree (Shorea robusta)",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (isHindi) "Sal Tree (Shorea robusta)" else "सखुआ / साल का पेड़",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = if (isHindi) "झारखंड का राज्य वृक्ष" else "State Tree of Jharkhand",
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
                            shape = controlCornerShape,
                            modifier = Modifier.height(44.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_volume_up),
                                    contentDescription = "Pronounce",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isHindi) "संथाली उच्चारण सुनें" else "Listen to Santali Audio",
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
    }
}
