package com.example.palashsetu.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palashsetu.data.local.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Controls the onboarding walkthrough lifecycle.
 *
 * Responsibilities:
 *  - Expose [uiState] StateFlow consumed by [OnboardingOverlay].
 *  - Advance / retreat steps, saving progress to SharedPrefs on each step
 *    so a power-off mid-walkthrough resumes from the last viewed step.
 *  - Mark walkthrough complete via [UserSessionManager].
 *  - Support [replay] so teachers can re-watch from the Help icon in PalashTopBar.
 */
class OnboardingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /**
     * Must be called once from MainAppContainer after authentication.
     * Checks SharedPrefs: if walkthrough not yet completed, auto-starts
     * from the last saved step (supports mid-walkthrough power-off recovery).
     */
    fun initIfNeeded(context: Context) {
        if (UserSessionManager.hasCompletedOnboarding(context)) return
        val lastStep = UserSessionManager.getOnboardingLastStep(context)
        val language = UserSessionManager.getLanguage(context)
        _uiState.update {
            it.copy(
                isActive = true,
                currentStepIndex = lastStep.coerceIn(0, ALL_ONBOARDING_STEPS.lastIndex),
                language = language
            )
        }
    }

    /** Advance to the next step. Saves progress. Finishes automatically on the last step. */
    fun next(context: Context) {
        val current = _uiState.value
        if (current.isLastStep) {
            finish(context)
            return
        }
        val newIndex = current.currentStepIndex + 1
        UserSessionManager.saveOnboardingProgress(context, newIndex)
        _uiState.update { it.copy(currentStepIndex = newIndex) }
    }

    /** Go back to the previous step. */
    fun previous() {
        val current = _uiState.value
        if (current.isFirstStep) return
        _uiState.update { it.copy(currentStepIndex = current.currentStepIndex - 1) }
    }

    /**
     * Dismiss / complete the walkthrough.
     * Marks [UserSessionManager.markOnboardingComplete] so it never auto-triggers again.
     */
    fun finish(context: Context) {
        UserSessionManager.markOnboardingComplete(context)
        _uiState.update { it.copy(isActive = false, currentStepIndex = 0) }
    }

    /**
     * Replay the walkthrough from step 0.
     * Called from the "?" icon in PalashTopBar.
     */
    fun replay(context: Context) {
        val language = UserSessionManager.getLanguage(context)
        _uiState.update {
            it.copy(isActive = true, currentStepIndex = 0, language = language)
        }
    }

    /** Sync language if the teacher toggles it mid-walkthrough. */
    fun updateLanguage(language: String) {
        _uiState.update { it.copy(language = language) }
    }
}
