package com.example.palashsetu.ui.onboarding

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect

/**
 * A shared reactive registry where each screen's composables register their
 * on-screen bounding boxes by tag. Backed by [mutableStateMapOf] so that
 * [OnboardingOverlay] and [SpotlightOverlay] recompose automatically the instant
 * any target composable completes layout measurement.
 */
class OnboardingTargetRegistry {
    private val bounds = mutableStateMapOf<String, Rect>()

    /** Called by [Modifier.onboardingTarget] during GloballyPositioned callbacks. */
    fun register(tag: String, rect: Rect) {
        bounds[tag] = rect
    }

    /** Returns the latest known bounds for [tag], or null if not yet laid out. */
    fun getBounds(tag: String): Rect? = bounds[tag]

    /** Clears all registrations (used on screen change). */
    fun clear() = bounds.clear()
}
