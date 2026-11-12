package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.Scaffold

/** 首次引导与设置中的回放共用原页面顺序、宿主与切换动画。 */
@Composable
fun OnboardingScreen(onAccept: () -> Unit, onExit: () -> Unit) {
    val controller = rememberOnboardingController()
    val state by controller.state.collectAsState()
    val actions = OnboardingActions(controller, onAccept)
    BackHandler { if (controller.back()) onExit() }
    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    val dir = if (state.backwards) -1 else 1
                    (slideInHorizontally(tween(durationMillis = 360, easing = FastOutSlowInEasing)) { it * dir } + fadeIn(tween(200)))
                        .togetherWith(slideOutHorizontally(tween(durationMillis = 360, easing = FastOutSlowInEasing)) { -it * dir } + fadeOut(tween(160)))
                },
                label = "onboarding-step",
            ) { current -> OnboardingStepContent(current, state, actions) }
            if (state.showTerms) TermsSheet(show = true, onDismiss = actions::dismissTerms)
        }
    }
}
