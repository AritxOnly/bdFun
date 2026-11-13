package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.teak.common.search.TeakSearchTransitionDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

import com.aritxonly.teak.common.interaction.blockTouches
import androidx.compose.runtime.collectAsState
import com.aritxonly.bdfun.feature.search.application.BottomSearchStateHolder

@Composable
internal fun BottomSearchPage(
    settings: SettingsBindings, transition: BottomSearchTransition,
    request: BottomSearchRequest, actions: BottomSearchActions,
    controllerFactory: () -> BottomSearchStateHolder, modifier: Modifier = Modifier,
) {
    val reveal = transition.reveal
    val windowAlpha = transition.windowAlpha
    val maskAlpha = transition.maskAlpha
    val contentAlpha = transition.contentAlpha
    val submitted = request.submitted
    val query = request.query
    val colors = MiuixTheme.colorScheme
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }

    val controller = remember { controllerFactory() }
    val state by controller.state.collectAsState()
    val local = remember { BottomSearchPresentationState(settings.searchHistory) }
    BottomSearchEffects(settings, controller, request, local, focusRequester)

    val p = reveal.coerceIn(0f, 1f)
    val liftPx = with(density) { TeakSearchTransitionDefaults.ContentLift.toPx() }
    val window = windowAlpha.coerceIn(0f, 1f)
    val mask = maskAlpha.coerceIn(0f, 1f)
    val content = contentAlpha.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = window }
        // 遮罩：linear 100ms（进入）/ 100ms + 50ms 延迟（退出）。
            .background(colors.background.copy(alpha = mask)),
    ) {
        // 拦住透传到首页底下的手势：页面本身是半透明玻璃，空白处不能点穿。
        Box(Modifier.matchParentSize().blockTouches(true))
        Column(
            modifier = Modifier
                .fillMaxSize()
            // 原实现：容器从 +106dp 滑到 0（弹簧），透明度是独立的 linear 100ms(+50ms 延迟)。
                .graphicsLayer {
                translationY = (1f - p) * liftPx
                alpha = content
            },
        ) {
            BottomSearchHeader(settings, request, actions, local, focusRequester)

            BottomSearchBody(settings, request, actions, local, state)
        }
    }
}
