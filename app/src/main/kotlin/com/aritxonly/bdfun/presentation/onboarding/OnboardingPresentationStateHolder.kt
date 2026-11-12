package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class OnboardingHeroPresentationStateHolder {
    val wave = Animatable(0f)
    var wavePhase by mutableFloatStateOf(0f)
    var titleDistance by mutableFloatStateOf(0f)
    var doneDistance by mutableFloatStateOf(0f)
}
internal class OnboardingMarkPresentationStateHolder {
    var boosted by mutableStateOf(false)
    var pressed by mutableStateOf(false)
}
