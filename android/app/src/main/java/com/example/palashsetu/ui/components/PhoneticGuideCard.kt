package com.example.palashsetu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palashsetu.theme.Secondary
import com.example.palashsetu.theme.SecondaryFixed
import com.example.palashsetu.theme.SurfaceContainerLowest

@Composable
fun PhoneticGuideCard(
    phoneticText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SecondaryFixed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
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
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "🗣️", fontSize = 14.sp)
                Text(
                    text = "उच्चारण मार्गदर्शिका (Teacher Phonetic Guide)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Secondary
                )
            }
            Text(
                text = "देवनागरी लिपि",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF663500),
                modifier = Modifier
                    .background(Color(0xFFFFE0B2), RoundedCornerShape(6.dp))
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
                .background(SurfaceContainerLowest.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .padding(10.dp)
        )
    }
}
