package com.example.palashsetu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.palashsetu.data.local.UserSessionManager
import com.example.palashsetu.theme.Secondary
import com.example.palashsetu.theme.SecondaryFixed
import com.example.palashsetu.theme.SurfaceContainerLowest

/**
 * Stitch Architectural Geometry Phonetic Guide Card.
 * Uses 8dp container corners and 4dp sharp controls.
 */
@Composable
fun PhoneticGuideCard(
    phoneticText: String,
    currentLanguage: String = UserSessionManager.getLanguage(LocalContext.current),
    modifier: Modifier = Modifier
) {
    val isHindi = currentLanguage == "hi"
    val cardCornerShape = RoundedCornerShape(8.dp)
    val controlCornerShape = RoundedCornerShape(4.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardCornerShape)
            .background(SecondaryFixed.copy(alpha = 0.45f))
            .border(1.dp, Secondary.copy(alpha = 0.25f), cardCornerShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_record_voice_over),
                    contentDescription = "Voice Guide",
                    tint = Secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isHindi) "शिक्षक उच्चारण मार्गदर्शिका" else "Teacher Phonetic Guide",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Secondary
                )
            }
            Text(
                text = if (isHindi) "देवनागरी लिपि" else "Devanagari Script",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF663500),
                modifier = Modifier
                    .clip(controlCornerShape)
                    .background(Color(0xFFFFE0B2))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Text(
            text = phoneticText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2F1500),
            modifier = Modifier
                .fillMaxWidth()
                .clip(controlCornerShape)
                .background(SurfaceContainerLowest.copy(alpha = 0.95f))
                .border(1.dp, Color(0xFFE2E8F0), controlCornerShape)
                .padding(10.dp)
        )
    }
}
