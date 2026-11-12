package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.aritxonly.bdfun.feature.onboarding.application.*

@Composable
internal fun rememberOnboardingController(): OnboardingStateHolder {
    val scope = rememberCoroutineScope()
    val saver = remember(scope) {
        androidx.compose.runtime.saveable.Saver<OnboardingStateHolder, Int>(
            save = { it.state.value.step },
            restore = { com.aritxonly.bdfun.AppGraph.onboardingStateHolder(scope, it) },
        )
    }
    val controller = rememberSaveable(saver = saver) {
        com.aritxonly.bdfun.AppGraph.onboardingStateHolder(scope, STEP_WELCOME)
    }
    androidx.compose.runtime.DisposableEffect(controller) { onDispose { controller.close() } }
    return controller
}
