package com.aritxonly.bdfun


import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.aritxonly.teak.common.navigation.TeakFloatingTabBarDefaults
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.ui.BackgroundPlayDisc

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.MainNavigationDisc(
    settings: com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings, visual: MainBottomNavigationVisual, onOpenVideo: (VideoItem) -> Unit,
) {

    BackgroundPlayDisc(
        settings = settings, dependencies = AppGraph.discDependencies,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(end = visual.discEndPadding)
            .graphicsLayer {
                alpha = visual.discSlot
                translationX = (1f - visual.discSlot) * 70.dp.toPx()
                translationY = visual.discBottomOffset.toPx()
            },
        size = TeakFloatingTabBarDefaults.Height,
    ) { onOpenVideo(it) }
}
