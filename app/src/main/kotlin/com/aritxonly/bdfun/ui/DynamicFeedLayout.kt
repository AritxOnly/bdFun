package com.aritxonly.bdfun.ui

/** Two readable 354dp cards plus a 12dp gutter and 16dp outer padding fit from 736dp. */
internal fun dynamicFeedColumns(viewportWidthDp: Float): Int = if (viewportWidthDp >= 736f) 2 else 1
