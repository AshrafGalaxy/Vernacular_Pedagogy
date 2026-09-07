package com.example.palashsetu.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palashsetu.data.local.UserSessionManager
import com.example.palashsetu.theme.Background
import com.example.palashsetu.theme.Primary
import com.example.palashsetu.theme.PrimaryContainer
import com.example.palashsetu.theme.Secondary
import com.example.palashsetu.theme.SurfaceContainerLow
import com.example.palashsetu.theme.SurfaceContainerLowest

@Composable
fun TeacherLoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Load initial profile from session manager
    val savedProfile = remember { UserSessionManager.getProfile(context) }

    var selectedSalutation by remember { mutableStateOf(savedProfile.salutation.ifBlank { "श्री" }) }
    var teacherName by remember { mutableStateOf(savedProfile.name) }
    var pin by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }

    val salutationOptions = listOf(
        Triple("श्री", "Sir", "👨‍🏫"),
        Triple("श्रीमती", "Ma'am", "👩‍🏫"),
        Triple("शिक्षक", "Teacher", "🧑‍🏫")
    )

    val currentAvatar = when (selectedSalutation) {
        "श्रीमती" -> "👩‍🏫"
        "शिक्षक" -> "🧑‍🏫"
        else -> "👨‍🏫"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // App Header Brand
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ᱯᱥ",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "PALASH-SETU",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Primary,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "झारखंड प्राथमिक मातृभाषा सेतु • NIPUN FLN",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Secondary
            )
        }

        // Teacher Setup Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive Avatar
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = currentAvatar, fontSize = 40.sp)
                }

                // Dynamic Display Header
                Text(
                    text = if (teacherName.isNotBlank()) "$selectedSalutation ${teacherName.trim()}" else "शिक्षक प्रोफाइल सेटअप",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    textAlign = TextAlign.Center
                )

                // 1. Salutation Selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "संबोधन चुनें (Select Salutation):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        salutationOptions.forEach { (salutation, english, emoji) ->
                            val isSelected = selectedSalutation == salutation
                            val backgroundColor by animateColorAsState(
                                targetValue = if (isSelected) Primary else SurfaceContainerLow,
                                label = "salutationBg"
                            )
                            val textColor by animateColorAsState(
                                targetValue = if (isSelected) Color.White else Color(0xFF1E293B),
                                label = "salutationText"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(backgroundColor)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Primary else Color(0xFFCBD5E1),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedSalutation = salutation
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(text = emoji, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$salutation\n($english)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Teacher Name Input Field
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "शिक्षक का नाम (Teacher Name):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    OutlinedTextField(
                        value = teacherName,
                        onValueChange = {
                            teacherName = it
                            if (nameError != null && it.isNotBlank()) nameError = null
                        },
                        placeholder = { Text("उदा. पूजा सोरेन / Ramesh Soren", fontSize = 14.sp) },
                        isError = nameError != null,
                        supportingText = {
                            if (nameError != null) {
                                Text(text = nameError!!, color = Color(0xFFBA1A1A), fontSize = 11.sp)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                }

                // 3. 100% Functional PIN Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (savedProfile.isConfigured) "4-अंकीय शिक्षक पिन (Enter PIN):" else "4-अंकीय सुरक्षा पिन सेट करें (Set PIN):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                        TextButton(
                            onClick = {
                                pin = "2604"
                                pinError = null
                            }
                        ) {
                            Text(
                                text = "DEMO PIN भरें (2604)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Secondary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pin = it
                                if (pinError != null) pinError = null
                            }
                        },
                        placeholder = { Text("4 अंकों का पिन दर्ज करें (e.g. 2604)", fontSize = 14.sp) },
                        visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = pinError != null,
                        supportingText = {
                            if (pinError != null) {
                                Text(text = pinError!!, color = Color(0xFFBA1A1A), fontSize = 11.sp)
                            } else {
                                Text(
                                    text = if (savedProfile.isConfigured) "अपने पहले से सेट पिन या डेमो पिन 2604 से प्रवेश करें" else "भविष्य के ऑफलाइन एक्सेस हेतु 4 अंक निर्धारित करें",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPinVisible = !isPinVisible }) {
                                Text(
                                    text = if (isPinVisible) "🙈" else "👁️",
                                    fontSize = 16.sp
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Enter Classroom Action Button
                Button(
                    onClick = {
                        focusManager.clearFocus()

                        // Validation
                        var hasError = false
                        if (teacherName.isBlank()) {
                            nameError = "कृपया शिक्षक का नाम दर्ज करें (Enter Teacher Name)"
                            hasError = true
                        }

                        if (pin.length != 4) {
                            pinError = "कृपया 4 अंकों का संख्यात्मक पिन दर्ज करें (4 Digits Required)"
                            hasError = true
                        } else if (savedProfile.isConfigured && !UserSessionManager.verifyPin(context, pin)) {
                            pinError = "गलत पिन! पुनः प्रयास करें या डेमो पिन (2604) का उपयोग करें"
                            hasError = true
                        }

                        if (!hasError) {
                            // Save profile to session manager
                            UserSessionManager.saveProfile(
                                context = context,
                                salutation = selectedSalutation,
                                name = teacherName.trim(),
                                pin = pin
                            )
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
                        text = "कक्षा में प्रवेश करें (Enter Classroom) ➔",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Text(
            text = "⚡ 100% स्थानीय गणना • इंटरनेट की कोई आवश्यकता नहीं",
            fontSize = 11.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}
