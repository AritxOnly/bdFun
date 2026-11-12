package com.aritxonly.bdfun

import com.aritxonly.teak.common.chrome.TeakChromeDefaults

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.navigation.TeakFloatingTabBar
import com.aritxonly.teak.common.navigation.TeakFloatingTabItem
import com.aritxonly.teak.common.navigation.TeakFloatingTabLayout
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.blockTouches

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.MainFloatingNavigation(
    settings: SettingsBindings, tabItems: List<TeakFloatingTabItem>, selectedKey: String,
    onBarItem: (TeakFloatingTabItem) -> Unit, visual: MainBottomNavigationVisual,
) {

    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .graphicsLayer {
                alpha = visual.tabBarAlpha
                translationX = -visual.discSlot * visual.discShift.toPx()
            }
            .blockTouches(visual.tabBarCovered)
            .navigationBarsPadding()
            .padding(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = if (settings.bottomBarRaise) {
                    4.dp + TeakChromeDefaults.RaisedBottomBarExtra
                } else {
                    4.dp
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TeakFloatingTabBar(
            items = tabItems,
            selectedKey = selectedKey,
            onItemSelected = onBarItem,
            layout = TeakFloatingTabLayout.Stacked,
            selectedContentColor = visual.barSelectedColor,
        )
    }
}
