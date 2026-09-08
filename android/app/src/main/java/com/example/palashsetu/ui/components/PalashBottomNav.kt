package com.example.palashsetu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.example.palashsetu.theme.Primary
import com.example.palashsetu.theme.SurfaceContainerLowest
import com.example.palashsetu.ui.onboarding.LocalOnboardingRegistry
import com.example.palashsetu.ui.onboarding.onboardingTarget

/**
 * Stitch Architectural Geometry Bottom Navigation Tabs.
 * Uses official Android vector icons instead of emojis and adapts to the active language.
 */
enum class BottomTab(
    val hiLabel: String,
    val enLabel: String,
    val iconRes: Int
) {
    LIVE("लाइव सेतु", "Live Bridge", R.drawable.ic_mic),
    PHRASEBOOK("शब्दावली", "FLN Bank", R.drawable.ic_book),
    STUDIO("स्टूडियो", "Studio", R.drawable.ic_studio);

    fun getLabel(isHindi: Boolean): String = if (isHindi) hiLabel else enLabel
}

@Composable
fun PalashBottomNav(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    currentLanguage: String = UserSessionManager.getLanguage(LocalContext.current),
    modifier: Modifier = Modifier
) {
    val isHindi = currentLanguage == "hi"

    val navBarShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
    val tabCornerShape = RoundedCornerShape(4.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest, navBarShape)
            .border(1.dp, Color(0xFFE2E8F0), navBarShape)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val onboardingRegistry = LocalOnboardingRegistry.current
            // Tag each tab for onboarding: phrasebook_tab, studio_tab
            val tabTag = when (tab) {
                BottomTab.PHRASEBOOK -> "phrasebook_tab"
                BottomTab.STUDIO     -> "studio_tab"
                else                 -> ""
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(tabCornerShape)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 4.dp)
                    .then(
                        if (tabTag.isNotEmpty()) Modifier.onboardingTarget(tabTag, onboardingRegistry)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(tabCornerShape)
                            .background(if (isSelected) Primary else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = tab.iconRes),
                            contentDescription = tab.getLabel(isHindi),
                            tint = if (isSelected) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Text(
                        text = tab.getLabel(isHindi),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Primary else Color(0xFF64748B),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
