package com.example.palashsetu.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.palashsetu.data.local.UserSessionManager
import com.example.palashsetu.ui.components.BottomTab
import com.example.palashsetu.ui.components.PalashBottomNav
import com.example.palashsetu.ui.screens.auth.TeacherLoginScreen
import com.example.palashsetu.ui.screens.live.LiveVoiceBridgeScreen
import com.example.palashsetu.ui.screens.phrasebook.FlnPhrasebookScreen
import com.example.palashsetu.ui.screens.studio.PedagogyStudioScreen

@Composable
fun MainAppContainer(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isAuthenticated by remember { mutableStateOf(UserSessionManager.getProfile(context).isConfigured) }
    var selectedTab by remember { mutableStateOf(BottomTab.LIVE) }
    var currentLanguage by remember { mutableStateOf(UserSessionManager.getLanguage(context)) }

    if (!isAuthenticated) {
        TeacherLoginScreen(
            currentLanguage = currentLanguage,
            onLanguageChanged = { currentLanguage = it },
            onLoginSuccess = { isAuthenticated = true },
            modifier = modifier
        )
    } else {
        Scaffold(
            bottomBar = {
                PalashBottomNav(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    currentLanguage = currentLanguage
                )
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    BottomTab.LIVE -> LiveVoiceBridgeScreen(
                        currentLanguage = currentLanguage,
                        onLanguageToggle = { currentLanguage = it }
                    )
                    BottomTab.PHRASEBOOK -> FlnPhrasebookScreen(
                        currentLanguage = currentLanguage,
                        onLanguageToggle = { currentLanguage = it }
                    )
                    BottomTab.STUDIO -> PedagogyStudioScreen(
                        currentLanguage = currentLanguage,
                        onLanguageToggle = { currentLanguage = it }
                    )
                }
            }
        }
    }
}
