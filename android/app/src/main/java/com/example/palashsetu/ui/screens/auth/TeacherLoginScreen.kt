package com.example.palashsetu.ui.screens.auth

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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palashsetu.R
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

    // Session state
    val savedProfile = remember { UserSessionManager.getProfile(context) }
    var currentLanguage by remember { mutableStateOf(UserSessionManager.getLanguage(context)) }
    val isHindi = currentLanguage == "hi"

    var selectedSalutation by remember { mutableStateOf(savedProfile.salutation.ifBlank { if (isHindi) "श्री" else "Sir" }) }
    var teacherName by remember { mutableStateOf(savedProfile.name) }
    var pin by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }

    val salutationOptions = if (isHindi) {
        listOf(
            Triple("श्री", "Sir", "👨‍🏫"),
            Triple("श्रीमती", "Ma'am", "👩‍🏫"),
            Triple("शिक्षक", "Teacher", "🧑‍🏫")
        )
    } else {
        listOf(
            Triple("Sir", "श्री", "👨‍🏫"),
            Triple("Ma'am", "श्रीमती", "👩‍🏫"),
            Triple("Teacher", "शिक्षक", "🧑‍🏫")
        )
    }

    val currentAvatar = when {
        selectedSalutation.contains("श्रीमती") || selectedSalutation.equals("Ma'am", ignoreCase = true) -> "👩‍🏫"
        selectedSalutation.contains("शिक्षक") || selectedSalutation.equals("Teacher", ignoreCase = true) -> "🧑‍🏫"
        else -> "👨‍🏫"
    }

    // Stitch Sharp Corners Design System (4dp for controls, 8dp for cards)
    val controlCornerShape = RoundedCornerShape(4.dp)
    val cardCornerShape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Language Toggle (Hindi / English)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(controlCornerShape)
                    .background(SurfaceContainerLow)
                    .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                    .padding(2.dp)
            ) {
                Row {
                    listOf("hi" to "🇮🇳 हिन्दी", "en" to "English").forEach { (code, label) ->
                        val isSelected = currentLanguage == code
                        val pillBg by animateColorAsState(
                            targetValue = if (isSelected) Primary else Color.Transparent,
                            label = "langPillBg"
                        )
                        val pillText by animateColorAsState(
                            targetValue = if (isSelected) Color.White else Color(0xFF475569),
                            label = "langPillText"
                        )

                        Box(
                            modifier = Modifier
                                .clip(controlCornerShape)
                                .background(pillBg)
                                .clickable {
                                    currentLanguage = code
                                    UserSessionManager.saveLanguage(context, code)
                                    selectedSalutation = if (code == "hi") "श्री" else "Sir"
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = pillText
                            )
                        }
                    }
                }
            }
        }

        // Prominent Vaani-Setu Brand Emblem & Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(cardCornerShape)
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ᱵᱥ",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = "VAANI-SETU",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Primary,
                letterSpacing = 1.2.sp
            )
            Text(
                text = if (isHindi) "झारखंड प्राथमिक मातृभाषा सेतु • NIPUN FLN" else "Jharkhand Primary Vernacular Bridge • NIPUN FLN",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Secondary
            )
        }

        // Teacher Profile Setup Card (Centered Layout + Stitch Sharp 4dp Controls)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = cardCornerShape,
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Centered Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = currentAvatar, fontSize = 36.sp)
                }

                // Centered Profile Title & Identity Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isHindi) "शिक्षक प्रोफाइल सेटअप" else "Teacher Profile Setup",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (teacherName.isNotBlank()) "$selectedSalutation ${teacherName.trim()}" else if (isHindi) "विवरण दर्ज करें" else "Enter credentials",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                // 1. Salutation Selector (Symmetric 3 Pills with Sharp 4dp Corners)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isHindi) "संबोधन (Salutation):" else "Salutation:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        salutationOptions.forEach { (salutation, _, emoji) ->
                            val isSelected = selectedSalutation.equals(salutation, ignoreCase = true)
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
                                    .height(36.dp)
                                    .clip(controlCornerShape)
                                    .background(backgroundColor)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Primary else Color(0xFFCBD5E1),
                                        shape = controlCornerShape
                                    )
                                    .clickable {
                                        selectedSalutation = salutation
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(text = emoji, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = salutation,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Teacher Name Field (Sharp 4dp Corners)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isHindi) "शिक्षक का नाम (Teacher Name):" else "Teacher Name:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    OutlinedTextField(
                        value = teacherName,
                        onValueChange = {
                            teacherName = it
                            if (nameError != null && it.isNotBlank()) nameError = null
                        },
                        placeholder = {
                            Text(
                                text = if (isHindi) "उदा. पूजा सोरेन / रमेश मुर्मू" else "e.g. Ramesh Soren",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        isError = nameError != null,
                        supportingText = {
                            if (nameError != null) {
                                Text(text = nameError!!, color = Color(0xFFBA1A1A), fontSize = 10.sp)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = controlCornerShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                }

                // 3. 4-Digit Security PIN (Sharp 4dp Corners + Clean Vector Eye Toggle)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isHindi) "4-अंकीय सुरक्षा पिन सेट करें (PIN):" else "Set 4-Digit Security PIN:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pin = it
                                if (pinError != null) pinError = null
                            }
                        },
                        placeholder = {
                            Text(
                                text = if (isHindi) "4 अंकों का पिन दर्ज करें (उदा. 2604)" else "4-digit PIN (e.g. 2604)",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = pinError != null,
                        supportingText = {
                            if (pinError != null) {
                                Text(text = pinError!!, color = Color(0xFFBA1A1A), fontSize = 10.sp)
                            } else {
                                Text(
                                    text = if (isHindi) "ऑफलाइन सुरक्षा हेतु 4 अंकों का पिन" else "4-digit PIN for offline access",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPinVisible = !isPinVisible }) {
                                Icon(
                                    painter = painterResource(id = if (isPinVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                                    contentDescription = "Toggle PIN Visibility",
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(20.dp)
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
                        shape = controlCornerShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 4. Enter Classroom Action Button (Sharp 4dp Corners, Single Line)
                Button(
                    onClick = {
                        focusManager.clearFocus()

                        var hasError = false
                        if (teacherName.isBlank()) {
                            nameError = if (isHindi) "कृपया शिक्षक का नाम दर्ज करें" else "Please enter teacher name"
                            hasError = true
                        }

                        if (pin.length != 4) {
                            pinError = if (isHindi) "कृपया 4 अंकों का पिन दर्ज करें" else "Please enter a 4-digit PIN"
                            hasError = true
                        } else if (savedProfile.isConfigured && !UserSessionManager.verifyPin(context, pin)) {
                            pinError = if (isHindi) "गलत पिन! पुनः प्रयास करें" else "Incorrect PIN! Try again"
                            hasError = true
                        }

                        if (!hasError) {
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
                        .height(44.dp),
                    shape = controlCornerShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(
                        text = if (isHindi) "कक्षा में प्रवेश करें" else "Enter Classroom",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Bottom Guarantee Label
        Text(
            text = if (isHindi) "⚡ 100% स्थानीय गणना • इंटरनेट की आवश्यकता नहीं" else "⚡ 100% On-Device AI • No Internet Required",
            fontSize = 10.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )
    }
}
