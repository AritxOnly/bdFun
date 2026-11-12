package com.aritxonly.bdfun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class SettingsAppearancePresentationStateHolder {
    var pendingDark by mutableStateOf<Int?>(null)
    var pendingColor by mutableStateOf<Int?>(null)
}
internal class SettingsFontScalePresentationStateHolder(value: Float) {
    var value by mutableFloatStateOf(value)
}
