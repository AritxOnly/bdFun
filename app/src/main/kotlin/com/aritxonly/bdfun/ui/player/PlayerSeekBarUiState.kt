package com.aritxonly.bdfun.ui

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

internal class InlineSeekBarUiStateHolder {
    val pressed = mutableStateOf(false)
    val dragRatio = mutableFloatStateOf(-1f)
}
