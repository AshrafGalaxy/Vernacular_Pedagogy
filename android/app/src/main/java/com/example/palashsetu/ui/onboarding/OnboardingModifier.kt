package com.example.palashsetu.ui.onboarding

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot

/**
 * Extension on [Modifier] that registers the composable's on-screen bounding
 * rectangle into the [OnboardingTargetRegistry] every time the layout changes.
 *
 * Add this to any composable that the onboarding walkthrough should spotlight:
 * ```kotlin
 * val registry = LocalOnboardingRegistry.current
 *
 * FloatingActionButton(
 *     modifier = Modifier.onboardingTarget("mic_fab", registry)
 * ) { ... }
 * ```
 *
 * @param tag      Unique string matching an [OnboardingStep.targetTag].
 * @param registry The [OnboardingTargetRegistry] from [LocalOnboardingRegistry.current].
 */
fun Modifier.onboardingTarget(tag: String, registry: OnboardingTargetRegistry): Modifier =
    this.onGloballyPositioned { layoutCoordinates ->
        registry.register(tag, layoutCoordinates.boundsInRoot())
    }
