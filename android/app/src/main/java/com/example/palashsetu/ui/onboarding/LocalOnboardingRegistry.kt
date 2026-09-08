package com.example.palashsetu.ui.onboarding

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal that carries the [OnboardingTargetRegistry] down the Compose tree.
 * Provided once in [MainAppContainer]; consumed in every screen that registers targets.
 *
 * Usage in a screen composable:
 * ```kotlin
 * val registry = LocalOnboardingRegistry.current
 * Box(Modifier.onboardingTarget("mic_fab", registry)) { ... }
 * ```
 */
val LocalOnboardingRegistry = compositionLocalOf { OnboardingTargetRegistry() }
