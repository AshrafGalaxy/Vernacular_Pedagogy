package com.example.palashsetu.ui.onboarding

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.palashsetu.ui.components.BottomTab

/**
 * Master onboarding overlay composable.
 *
 * Renders:
 *  1. [SpotlightOverlay] — full-screen translucent frosted Canvas with an illuminated
 *     punch-out cutout around the current step's target composable.
 *  2. [OnboardingTooltip] — bilingual tooltip card positioned dynamically ABOVE or BELOW
 *     the target with guaranteed physical clearance, NEVER occluding or overlapping
 *     the spotlighted UI element.
 *
 * @param state          Live [OnboardingUiState] from [OnboardingViewModel].
 * @param registry       The reactive [OnboardingTargetRegistry] holding all measured bounds.
 * @param context        Android context for SharedPrefs persistence.
 * @param onNext         Advance to next step.
 * @param onPrevious     Go back one step.
 * @param onSkip         Dismiss the walkthrough.
 * @param onTabRequested Callback to switch the bottom tab when a step requires it.
 */
@Composable
fun BoxScope.OnboardingOverlay(
    state: OnboardingUiState,
    registry: OnboardingTargetRegistry,
    context: Context,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    onTabRequested: (BottomTab) -> Unit
) {
    if (!state.isActive) return

    val step = state.currentStep

    // If this step requires a tab switch, request it via callback
    val requiredTab = state.requiredTab
    if (requiredTab != null) {
        onTabRequested(requiredTab)
    }

    // Retrieve reactive bounds from the registry
    val targetBounds: Rect? = registry.getBounds(step.targetTag)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeightDp = maxHeight
        val density = LocalDensity.current

        // ── 1. Translucent frosted canvas with illuminated aperture ──────────
        SpotlightOverlay(
            targetBounds = targetBounds,
            shape = step.spotlightShape,
            visible = state.isActive,
            modifier = Modifier.fillMaxSize()
        )

        // ── 2. Intelligent, un-occluded modal tooltip positioning ────────────
        // Only render the modal card when target bounds are measured to avoid any frame flashing
        if (targetBounds != null) {
            val targetTopDp = with(density) { targetBounds.top.toDp() }
            val targetBottomDp = with(density) { targetBounds.bottom.toDp() }
            val targetCenterYDp = with(density) { targetBounds.center.y.toDp() }

            val spaceBelowDp = screenHeightDp - targetBottomDp

            // Determine whether to place tooltip ABOVE or BELOW the target:
            // Place ABOVE if the element is in the lower portion of the screen (mic, bottom tabs, etc.)
            // or if the remaining space below is less than 240dp.
            val tooltipGoesAbove = targetCenterYDp > screenHeightDp * 0.45f || spaceBelowDp < 240.dp

            if (tooltipGoesAbove) {
                // ANCHORED STRICTLY ABOVE THE TARGET
                // bottomPadding pushes the card's bottom edge to sit exactly (targetTopDp - 18dp).
                // This guarantees at least 18dp of clear space above the spotlighted element.
                val bottomPadding = (screenHeightDp - targetTopDp + 18.dp).coerceAtLeast(18.dp)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(top = 36.dp, bottom = bottomPadding),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    OnboardingTooltip(
                        state = state,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSkip = onSkip,
                        visible = state.isActive,
                        arrowDirection = ArrowDirection.BOTTOM,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // ANCHORED STRICTLY BELOW THE TARGET
                // topPadding pushes the card's top edge to sit exactly (targetBottomDp + 18dp).
                // This guarantees at least 18dp of clear space below the spotlighted element.
                val topPadding = (targetBottomDp + 18.dp).coerceAtLeast(18.dp)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(top = topPadding, bottom = 48.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    OnboardingTooltip(
                        state = state,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSkip = onSkip,
                        visible = state.isActive,
                        arrowDirection = ArrowDirection.TOP,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
