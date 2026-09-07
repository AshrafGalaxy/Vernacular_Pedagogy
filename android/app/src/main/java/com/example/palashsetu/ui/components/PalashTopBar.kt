package com.example.palashsetu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.example.palashsetu.theme.Primary
import com.example.palashsetu.theme.SurfaceContainerLow

/**
 * Vaani-Setu Standard Top Navigation Bar.
 * Follows Stitch Architectural Geometry with 4dp sharp corners for the location chip,
 * official vector location icon, and interactive profile avatar.
 */
@Composable
fun PalashTopBar(
    currentLanguage: String = UserSessionManager.getLanguage(LocalContext.current),
    onLanguageToggle: ((String) -> Unit)? = null,
    locationName: String? = null,
    clusterName: String? = null,
    teacherName: String? = null,
    isOffline: Boolean = true,
    onProfileClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isHindi = currentLanguage == "hi"
    val resolvedLocation = locationName ?: clusterName ?: if (isHindi) "झारखंड" else "Jharkhand"
    val resolvedTeacherName = teacherName ?: UserSessionManager.getTeacherDisplayName(context)
    val initials = resolvedTeacherName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.firstOrNull()?.toString() ?: "" }
        .joinToString("")
        .ifBlank { "VS" }

    val controlCornerShape = RoundedCornerShape(4.dp)
    val barCornerShape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, barCornerShape)
            .border(
                width = 1.dp,
                color = Color(0xFFE2E8F0),
                shape = barCornerShape
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Location Chip (Stitch Sharp 4dp Geometry + Vector Icon)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(controlCornerShape)
                .background(SurfaceContainerLow)
                .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_location_on),
                contentDescription = "Location",
                tint = Primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = resolvedLocation,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Dynamic Language Switcher Pill (हिन्दी | English)
            Row(
                modifier = Modifier
                    .clip(controlCornerShape)
                    .background(SurfaceContainerLow)
                    .border(1.dp, Color(0xFFCBD5E1), controlCornerShape)
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(controlCornerShape)
                        .background(if (isHindi) Primary else Color.Transparent)
                        .clickable {
                            if (!isHindi) {
                                UserSessionManager.saveLanguage(context, "hi")
                                onLanguageToggle?.invoke("hi")
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "हिन्दी",
                        fontSize = 11.sp,
                        fontWeight = if (isHindi) FontWeight.Bold else FontWeight.Medium,
                        color = if (isHindi) Color.White else Color(0xFF475569)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(controlCornerShape)
                        .background(if (!isHindi) Primary else Color.Transparent)
                        .clickable {
                            if (isHindi) {
                                UserSessionManager.saveLanguage(context, "en")
                                onLanguageToggle?.invoke("en")
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "English",
                        fontSize = 11.sp,
                        fontWeight = if (!isHindi) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isHindi) Color.White else Color(0xFF475569)
                    )
                }
            }

            // Clickable Teacher Avatar (Initials circle)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Primary)
                    .clickable {
                        onProfileClick?.invoke()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
