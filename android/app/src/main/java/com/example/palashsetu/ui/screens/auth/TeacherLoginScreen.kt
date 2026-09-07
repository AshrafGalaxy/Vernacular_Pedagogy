package com.example.palashsetu.ui.screens.auth

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palashsetu.theme.Background
import com.example.palashsetu.theme.Primary
import com.example.palashsetu.theme.PrimaryContainer
import com.example.palashsetu.theme.Secondary
import com.example.palashsetu.theme.SecondaryContainer
import com.example.palashsetu.theme.SurfaceContainer
import com.example.palashsetu.theme.SurfaceContainerLow
import com.example.palashsetu.theme.SurfaceContainerLowest

@Composable
fun TeacherLoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableStateOf(2) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // App Header Brand
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "ᱯᱥ", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "PALASH-SETU",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Primary,
                letterSpacing = 1.sp
            )
            Text(
                text = "झारखंड प्राथमिक मातृभाषा सेतु • NIPUN FLN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Secondary
            )
        }

        // Teacher Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👩‍🏫", fontSize = 36.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Smt. Pooja Soren",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Text(
                    text = "Govt. Primary School, Khunti Cluster",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "Primary Wing (Class 1 to 5)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Secondary
                )
            }
        }

        // Target Grade Selector
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "कक्षा स्तर चुनें (Target FLN Grade):",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1 to "कक्षा 1 (Balvatika)", 2 to "कक्षा 2 (Active)", 3 to "कक्षा 3 (Bridge)").forEach { (grade, label) ->
                    val isSelected = selectedGrade == grade
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Primary else SurfaceContainerLow)
                            .border(1.dp, if (isSelected) Primary else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                            .clickable { selectedGrade = grade }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF1E293B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Vernacular Dialect Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLow, RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "प्राथमिक बोली (Vernacular)", fontSize = 11.sp, color = Color(0xFF64748B))
                Text(text = "ᱥᱟᱱᱛᱟᱲᱤ (Ol Chiki - Hasada)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Primary)
            }
            Text(
                text = "98.4% Edge Match ✓",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20),
                modifier = Modifier
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }

        // Numeric PIN Entry
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "4-अंकीय शिक्षक पिन (Classroom Unlock PIN):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
                TextButton(onClick = { pin = "2604" }) {
                    Text(text = "DEMO PIN भरें (2604)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Secondary)
                }
            }
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4) pin = it },
                placeholder = { Text("पिन दर्ज करें (e.g. 2604)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Enter Classroom Button
        Button(
            onClick = {
                if (pin.isNotBlank()) {
                    onLoginSuccess()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text(
                text = "कक्षा में प्रवेश करें (Enter Classroom)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            text = "⚡ 100% स्थानीय गणना • इंटरनेट की कोई आवश्यकता नहीं",
            fontSize = 11.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
    }
}
