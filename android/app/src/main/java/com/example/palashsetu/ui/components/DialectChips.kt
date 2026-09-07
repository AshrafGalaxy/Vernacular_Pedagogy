package com.example.palashsetu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.example.palashsetu.theme.Primary

/**
 * Stitch Architectural Geometry Dialect Selector.
 * Displays only the active Santali (Ol Chiki) vernacular channel with sharp 4dp styling.
 */
@Composable
fun DialectChips(
    selectedDialect: String = "ᱥᱟᱱᱛᱟᱲᱤ",
    onDialectSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val controlCornerShape = RoundedCornerShape(4.dp)

    Row(
        modifier = modifier.fillMaxWidth(),
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

        // Santali (Ol Chiki) Only - Sharp 4dp Stitch Geometry
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(controlCornerShape)
                .background(Primary)
                .clickable { onDialectSelect("ᱥᱟᱱᱛᱟᱲᱤ") }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "ᱥᱟᱱᱛᱟᱲᱤ • Santali (Ol Chiki) ✓",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
