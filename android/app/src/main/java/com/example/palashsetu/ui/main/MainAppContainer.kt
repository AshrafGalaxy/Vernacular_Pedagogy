package com.example.palashsetu.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palashsetu.data.local.UserSessionManager
import com.example.palashsetu.ui.components.BottomTab
import com.example.palashsetu.ui.components.PalashBottomNav
import com.example.palashsetu.ui.onboarding.LocalOnboardingRegistry
import com.example.palashsetu.ui.onboarding.OnboardingOverlay
import com.example.palashsetu.ui.onboarding.OnboardingTargetRegistry
import com.example.palashsetu.ui.onboarding.OnboardingViewModel
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

    // ── Onboarding infrastructure ──────────────────────────────────────────────
    val onboardingVm: OnboardingViewModel = viewModel()
    val onboardingState by onboardingVm.uiState.collectAsState()
    // Single shared registry for all screens — lives as long as MainAppContainer
    val onboardingRegistry = remember { OnboardingTargetRegistry() }

    // Auto-start walkthrough once after the teacher completes first login
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            onboardingVm.initIfNeeded(context)
        }
    }

    // Keep ViewModel language in sync with user language toggles
    LaunchedEffect(currentLanguage) {
        onboardingVm.updateLanguage(currentLanguage)
    }

    if (!isAuthenticated) {
        TeacherLoginScreen(
            currentLanguage = currentLanguage,
            onLanguageChanged = { currentLanguage = it },
            onLoginSuccess = { isAuthenticated = true },
            modifier = modifier
        )
    } else {
        // Provide the registry to all child composables via CompositionLocal
        CompositionLocalProvider(LocalOnboardingRegistry provides onboardingRegistry) {
            // The outer Box allows the OnboardingOverlay to stack above the Scaffold
            Box(modifier = modifier.fillMaxSize()) {

                Scaffold(
                    bottomBar = {
                        PalashBottomNav(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            currentLanguage = currentLanguage
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            BottomTab.LIVE -> LiveVoiceBridgeScreen(
                                currentLanguage = currentLanguage,
                                onLanguageToggle = { currentLanguage = it },
                                onReplayWalkthrough = { onboardingVm.replay(context) }
                            )
                            BottomTab.PHRASEBOOK -> FlnPhrasebookScreen(
                                currentLanguage = currentLanguage,
                                onLanguageToggle = { currentLanguage = it },
                                onReplayWalkthrough = { onboardingVm.replay(context) }
                            )
                            BottomTab.STUDIO -> PedagogyStudioScreen(
                                currentLanguage = currentLanguage,
                                onLanguageToggle = { currentLanguage = it },
                                onReplayWalkthrough = { onboardingVm.replay(context) }
                            )
                        }
                    }
                }

                // ── Onboarding overlay — drawn ABOVE the Scaffold ──────────────
                if (onboardingState.isActive) {
                    OnboardingOverlay(
                        state = onboardingState,
                        registry = onboardingRegistry,
                        context = context,
                        onNext = { onboardingVm.next(context) },
                        onPrevious = { onboardingVm.previous() },
                        onSkip = { onboardingVm.finish(context) },
                        onTabRequested = { tab ->
                            // Only switch tab if not already on the right one
                            if (selectedTab != tab) selectedTab = tab
                        }
                    )
                }
            }
        }
    }
}
