package com.example.palashsetu.ui.onboarding

import com.example.palashsetu.ui.components.BottomTab

/**
 * Immutable snapshot of the onboarding overlay's current state,
 * consumed by [OnboardingOverlay] from [OnboardingViewModel].
 */
data class OnboardingUiState(
    /** Whether the overlay is currently visible. */
    val isActive: Boolean = false,
    /** 0-based index into [ALL_ONBOARDING_STEPS]. */
    val currentStepIndex: Int = 0,
    /** Current display language — "hi" or "en". */
    val language: String = "hi"
) {
    val currentStep: OnboardingStep
        get() = ALL_ONBOARDING_STEPS[currentStepIndex.coerceIn(0, ALL_ONBOARDING_STEPS.lastIndex)]

    val totalSteps: Int
        get() = ALL_ONBOARDING_STEPS.size

    val isFirstStep: Boolean
        get() = currentStepIndex == 0

    val isLastStep: Boolean
        get() = currentStepIndex == ALL_ONBOARDING_STEPS.lastIndex

    /** The tab that must be active for the current step; null = no tab switch needed. */
    val requiredTab: BottomTab?
        get() = currentStep.targetScreen

    /** Human-readable step counter string, e.g. "3 / 16" */
    fun stepCounter(): String = "${currentStepIndex + 1} / $totalSteps"
}
