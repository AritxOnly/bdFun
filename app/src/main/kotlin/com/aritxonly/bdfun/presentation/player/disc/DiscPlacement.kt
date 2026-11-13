package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.navigation.TeakFloatingTabBarDefaults
import com.aritxonly.teak.common.theme.TeakUiStyle
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings

internal data class DiscPlacement(
    val endPadding: Dp,
    val bottomOffset: Dp,
    val size: Dp,
)

@Composable
internal fun rememberDiscPlacement(settings: SettingsBindings, tabCount: Int = 3): DiscPlacement {
    val windowWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val classic = TeakUiStyle.isClassic
    val tabBarWidth = minOf(
        TeakFloatingTabBarDefaults.MaximumWidth,
        TeakFloatingTabBarDefaults.MaximumItemWidth * tabCount,
        (windowWidthDp - 32.dp).coerceAtLeast(0.dp),
    )
    val discWidth = TeakFloatingTabBarDefaults.Height
    val discGap = 8.dp
    val groupLeft = ((windowWidthDp - tabBarWidth - discGap - discWidth) / 2)
        .coerceAtLeast(0.dp)
    if (TeakUiStyle.isHyperOs4) {
        return DiscPlacement(endPadding = 0.dp, bottomOffset = (-70).dp, size = discWidth)
    }
    val endPadding = if (classic) 8.dp else (groupLeft - 8.dp).coerceAtLeast(0.dp)
    val bottomOffset = if (classic) {
        8.dp
    } else {
        4.dp - if (settings.bottomBarRaise) {
            TeakChromeDefaults.RaisedBottomBarExtra
        } else {
            0.dp
        }
    }
    return DiscPlacement(
        endPadding = endPadding,
        bottomOffset = bottomOffset,
        size = discWidth,
    )
}
