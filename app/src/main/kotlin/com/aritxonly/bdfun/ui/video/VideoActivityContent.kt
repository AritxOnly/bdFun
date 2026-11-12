package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun VideoActivityContent(
    isLoggedIn: () -> Boolean, settings: com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings,
    route: com.aritxonly.bdfun.core.navigation.Route.Video?, requestedBvid: String?, requestedAid: Long,
    pipMode: Boolean, host: VideoActivityHostActions,
) {
    val page = LocalVideoPageState.current
    val video = rememberVideoActivityEntry(route?.snapshot, requestedBvid, requestedAid, host) ?: return
    val ui = remember { VideoActivityPageStateHolder(page.autoNextRequest) }
    BindVideoActivityQueue(video, ui, host)
    val fullscreen = page.playerUi.fullscreen || pipMode
    val alpha = rememberManualProgress(if (fullscreen) 0f else 1f, 200f)
    val commentRequest = remember { Triple(route?.commentRoot ?: 0L, route?.commentTarget ?: 0L, route?.seekMs ?: 0L) }
    val pager = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = if (commentRequest.first > 0L) 1 else 0, pageCount = { 2 },
    )
    val scope = rememberCoroutineScope()
    val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val statusBarDp = rememberStatusBarTop()
    val playerHeightDp = (screenWidthDp * 9f / 16f).dp
    val immersiveStatus = settings.videoImmersiveStatusBar
    val playerTopInset = if (immersiveStatus) 0.dp else statusBarDp
    BindVideoActivityWindow(immersiveStatus, ui, host)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val paneLayout = resolveVideoPaneLayout(maxWidth.value, maxHeight.value)
        val layout = VideoActivityLayout(paneLayout, statusBarDp, fullscreen, pipMode, alpha, playerTopInset)
        androidx.compose.runtime.SideEffect { host.syncSystemBars(layout.splitPanes) }
        TeakActivityScaffold(
            settings = settings,
            onBack = { host.onBack() },
            options = ActivityPageOptions(
                backRevealed = false,
                showBackButton = false,
                showTopBar = false,
                showScrollTop = !pipMode,
            ),
            disc = ActivityDiscOptions(
                showBackgroundDisc = false,
            ),
            slots = ActivityPageSlots(
                overlay = { VideoActivityOverlay(layout, ui, pager, scope) },
            ),
        ) {
            VideoActivityBody(isLoggedIn, settings, VideoActivityScene(video, commentRequest), layout, ui, pager, scope, host)
        }
    }
}
