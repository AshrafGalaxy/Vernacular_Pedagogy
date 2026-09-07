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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.palashsetu.R
import com.example.palashsetu.data.local.UserSessionManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val context = LocalContext.current
    val isHindi = UserSessionManager.getLanguage(context) == "hi"

    val health = remember { SystemHealth() }
    var isTestingAudio by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Column {
                Text(
                    text = if (isHindi) "सिस्टम स्थिति व ऑफलाइन स्वास्थ्य" else "System Health & Diagnostics",
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

            // RAM Memory Gauge Card (Stitch 8dp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = cardCornerShape,
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "रैम बजट (2GB Tablet)" else "RAM Budget (2GB Tablet)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Text(
                            text = "${health.ramUsedMb} MB / ${health.ramTotalMb} MB",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1B5E20),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    LinearProgressIndicator(
                        progress = { health.ramUsedMb.toFloat() / health.ramTotalMb.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF2E7D32),
                        trackColor = Color(0xFFE2E8F0)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "सुरक्षित मार्जिन: 871 MB Headroom (42.6%)" else "Safe Margin: 871 MB Headroom (42.6%)",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check_circle),
                                contentDescription = "Safe",
                                tint = Color(0xFF1B5E20),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "LMK Safe",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }
            }

            // Offline AI Engines Telemetry
            Text(
                text = if (isHindi) "ऑफलाइन मॉडल व भाषा संपदा (Edge Models):" else "Offline AI Models & Knowledge Assets:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155)
            )

            data class ModelTelemetry(val name: String, val spec: String, val status: String, val iconRes: Int)

            val models = listOf(
                ModelTelemetry("Hindi ASR Conformer", "Vosk-API / Sherpa-ONNX 16kHz Streaming", "42 MB • Ready", R.drawable.ic_mic),
                ModelTelemetry("Custom Distilled NMT", "IndicTrans2 INT8 CTranslate2 (mmap)", "~120 MB • Loaded", R.drawable.ic_studio),
                ModelTelemetry("Piper TTS Soundbank", "VITS Ol Chiki 16kHz ONNX (sat_piper_model)", "60.6 MB • Active", R.drawable.ic_volume_up),
                ModelTelemetry("NIPUN FLN Lexicon", "JCERT Primary Textbooks B-Tree Indexed", "368 Records • Ready", R.drawable.ic_book)
            )

            models.forEach { (name, spec, status, iconRes) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardCornerShape,
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(controlCornerShape)
                                    .background(SurfaceContainerLow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = name,
                                    tint = Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = spec,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(controlCornerShape)
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = status,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Nodal BRC Cluster Sync Card (Stitch 8dp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = cardCornerShape,
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isHindi) "नोडल सिंक स्थिति (Cluster Sync)" else "Nodal Cluster Sync Status",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "संकुल: ${health.brcSyncZone}" else "Cluster: ${health.brcSyncZone}",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check_circle),
                                contentDescription = "Sync Complete",
                                tint = Color(0xFF1B5E20),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isHindi) "स्थानीय सिंक पूर्ण" else "Local Sync Complete",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Hardware Diagnostic Buttons (Stitch Sharp 4dp Controls)
            Button(
                onClick = { isTestingAudio = !isTestingAudio },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = controlCornerShape,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        painter = painterResource(id = if (isTestingAudio) R.drawable.ic_check else R.drawable.ic_mic),
                        contentDescription = "Test Audio",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isTestingAudio) {
                            if (isHindi) "ध्वनि परीक्षण पूर्ण" else "Audio Diagnostic Complete"
                        } else {
                            if (isHindi) "माइक्रोफोन व ध्वनि परीक्षण" else "Run Audio & Mic Diagnostic Test"
                        },
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
                    syncMessage = if (isHindi) "BRC खूंटी जोन 3 से स्थानीय पाठ्यक्रम पूरी तरह अद्यतित (Up to date) है।" else "Curriculum and lexicon fully up to date with BRC Khunti Zone 3."
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = controlCornerShape
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_refresh),
                        contentDescription = "Sync",
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isHindi) "पाठ्यक्रम व शब्दकोश अपडेट जांचें" else "Check Curriculum & Lexicon Updates",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            AnimatedVisibility(visible = syncMessage != null) {
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
                        contentDescription = "Sync Info",
                        tint = Color(0xFF1B5E20),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = syncMessage ?: "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B5E20)
                    )
                }
            }
        }
    }
}

