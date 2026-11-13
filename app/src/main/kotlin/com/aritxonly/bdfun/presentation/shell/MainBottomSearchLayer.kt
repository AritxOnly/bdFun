package com.aritxonly.bdfun

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.*

@Composable
internal fun MainBottomSearchLayer(settings: SettingsBindings, shell: MainShellState) {
    val state = shell.bottom
    val context = LocalContext.current
    if (state.showBottomSearch || state.bottomSearchProgress.value > 0.001f) {
        BottomSearchPage(settings,
            BottomSearchTransition(state.bottomSearchProgress.value, state.bottomWindowAlpha.value,
                state.bottomMaskAlpha.value, state.bottomContentAlpha.value),
            BottomSearchRequest(state.bottomQuery, state.bottomSubmitted),
            BottomSearchActions({ state.bottomQuery = it }, { state.bottomSubmitted = it },
                { state.close() }, { state.bottomSubmitted = null },
                { navigateToVideo(context, it) }, { navigateToSpace(context, it.mid) }),
            shell.bottomSearchFactory)
    }
}
