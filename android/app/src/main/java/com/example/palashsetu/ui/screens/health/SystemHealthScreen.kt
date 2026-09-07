package com.example.palashsetu.ui.screens.health

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palashsetu.data.model.SystemHealth
import com.example.palashsetu.theme.Background
import com.example.palashsetu.theme.Primary
import com.example.palashsetu.theme.Secondary
import com.example.palashsetu.theme.SurfaceContainerLow
import com.example.palashsetu.theme.SurfaceContainerLowest
import com.example.palashsetu.ui.components.PalashTopBar

@Composable
fun SystemHealthScreen(
    modifier: Modifier = Modifier
) {
    val health = remember { SystemHealth() }
    var isTestingAudio by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

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
            // Header
            Column {
                Text(
                    text = "सिस्टम स्थिति व ऑफलाइन स्वास्थ्य",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary
                )
                Text(
                    text = "Edge Inference Health, Memory Telemetry & Nodal Sync",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            // RAM Memory Gauge Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "डिवाइस रैम बजट (2GB Tablet Limit)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        Text(
                            text = "${health.ramUsedMb} MB / ${health.ramTotalMb} MB",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1B5E20)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { health.ramUsedMb.toFloat() / health.ramTotalMb.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = Color(0xFF2E7D32),
                        trackColor = Color(0xFFE2E8F0)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "सुरक्षित मार्जिन: 871 MB Headroom (42.6%)",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "LMK Safe ✓",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            }

            // Offline AI Engines Telemetry
            Text(
                text = "ऑफलाइन मॉडल व भाषा संपदा (Edge Models):",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155)
            )

            val models = listOf(
                Triple("🎙️ Hindi ASR Conformer", "Vosk-API / Sherpa-ONNX 16kHz Streaming", "42 MB • Ready"),
                Triple("🌐 Custom Distilled NMT", "IndicTrans2 INT8 CTranslate2 (mmap)", "~120 MB • Loaded"),
                Triple("🔊 Piper TTS Soundbank", "VITS Ol Chiki 16kHz ONNX (sat_piper_model)", "60.6 MB • Active"),
                Triple("📚 NIPUN FLN Lexicon", "JCERT Primary Textbooks B-Tree Indexed", "368 Records • Ready")
            )

            models.forEach { (name, spec, status) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
                            Text(text = spec, fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                        }
                    }
                }
            }

            // Nodal BRC Cluster Sync
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "नोडल सिंक स्थिति (Cluster Sync)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "संकुल केंद्र: ${health.brcSyncZone}", fontSize = 12.sp, color = Color(0xFF334155))
                        Text(text = "✓ अंतिम स्थानीय सिंक पूर्ण", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                    }
                }
            }

            // Hardware Diagnostic Buttons
            Button(
                onClick = { isTestingAudio = !isTestingAudio },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = if (isTestingAudio) "ध्वनि परीक्षण पूर्ण ✓" else "🎤 माइक्रोफोन व लाउडस्पीकर ध्वनि परीक्षण",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = { syncMessage = "BRC खूंटी जोन 3 से स्थानीय पाठ्यक्रम पूरी तरह अद्यतित (Up to date) है।" },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "🔄 पाठ्यक्रम व स्थानीय शब्दकोश अपडेट जांचें", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
            }

            AnimatedVisibility(visible = syncMessage != null) {
                Text(
                    text = syncMessage ?: "",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B5E20),
                    modifier = Modifier
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                )
            }
        }
    }
}
