package com.example.palashsetu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palashsetu.theme.OnPrimaryContainer
import com.example.palashsetu.theme.PrimaryContainer
import com.example.palashsetu.theme.Secondary
import com.example.palashsetu.theme.SurfaceContainerHigh

@Composable
fun DialectChips(
    selectedDialect: String = "ᱥᱟᱱᱛᱟᱲᱤ",
    onDialectSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "त्वरित बोली:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(end = 4.dp)
        )

        // Santhali (Active)
        val isSanthali = selectedDialect.contains("ᱥᱟᱱᱛᱟᱲᱤ")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isSanthali) PrimaryContainer else SurfaceContainerHigh)
                .clickable { onDialectSelect("ᱥᱟᱱᱛᱟᱲᱤ") }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "ᱥᱟᱱᱛᱟᱲᱤ (Active) ✓",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSanthali) Color.White else Color(0xFF1E293B)
            )
        }

        // Ho (Warang Chiti)
        val isHo = selectedDialect.contains("हो")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isHo) PrimaryContainer else SurfaceContainerHigh)
                .clickable { onDialectSelect("हो (Warang Chiti)") }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "हो (Warang Chiti) ⚡",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isHo) Color.White else Color(0xFF1E293B)
            )
        }

        // Mundari
        val isMundari = selectedDialect.contains("मुंडारी")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isMundari) PrimaryContainer else SurfaceContainerHigh)
                .clickable { onDialectSelect("मुंडारी (Mundari)") }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "मुंडारी (Mundari) ⚡",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isMundari) Color.White else Color(0xFF1E293B)
            )
        }
    }
}
