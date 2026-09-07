package com.example.palashsetu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.example.palashsetu.theme.Secondary
import com.example.palashsetu.theme.SurfaceContainerLow
import androidx.compose.ui.platform.LocalContext
import com.example.palashsetu.data.local.UserSessionManager
import com.example.palashsetu.theme.TertiaryFixed

@Composable
fun PalashTopBar(
    clusterName: String = "खूंटी (Hasada)",
    teacherName: String? = null,
    isOffline: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resolvedTeacherName = teacherName ?: UserSessionManager.getTeacherDisplayName(context)
    val initials = resolvedTeacherName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.firstOrNull()?.toString() ?: "" }
        .joinToString("")
        .ifBlank { "PS" }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Location Cluster Chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(SurfaceContainerLow, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "📍",
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = clusterName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }

        // Teacher Avatar & Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF2E7D32), CircleShape)
                )
                Text(
                    text = " 100% Offline",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            }

            // Avatar circle
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
