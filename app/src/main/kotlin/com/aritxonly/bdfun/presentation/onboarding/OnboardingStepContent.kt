package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.feature.onboarding.application.*

@Composable
internal fun OnboardingStepContent(current: Int, state: OnboardingUiState, actions: OnboardingActions) {
    when (current) {
        STEP_WELCOME -> WelcomePage(onNext = { actions.go(STEP_NOTICE) })
        STEP_NOTICE -> NoticePage(
            accepted = state.accepted, termsViewed = state.termsViewed,
            onAcceptedChange = actions::setAccepted, onOpenTerms = actions::openTerms,
            onBack = { actions.go(STEP_WELCOME) }, onNext = { actions.go(STEP_BASIC) },
        )
        STEP_BASIC -> BasicPage(
            settings = state.settings, actions = actions,
            onBack = { actions.go(STEP_NOTICE) }, onStart = { actions.go(STEP_DONE) },
        )
        else -> CompletePage(onStart = actions::complete)
    }
}
