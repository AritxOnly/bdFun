package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.aritxonly.bdfun.core.model.settings.MineLayout
import com.aritxonly.bdfun.feature.mine.application.*
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

@Composable
internal fun MineScreen(request: MineScreenRequest, navigation: MineNavigation, dependencies: MineDependencies) {
    val controller = remember(dependencies, request.profile?.mid, request.sessionRevision) { dependencies.createData(request.profile) }
    val preview = remember(controller) { dependencies.createPreview(request.profile) }
    val state by controller.state.collectAsState()
    val previews by preview.state.collectAsState()
    val layout by dependencies.layout.state.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(controller, preview) { onDispose { controller.close(); preview.close() } }
    LaunchedEffect(controller) { controller.load() }
    LaunchedEffect(preview, layout, lifecycle) {
        if (request.profile != null) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            preview.load(layout)
            awaitCancellation()
        }
    }
    val scope = rememberCoroutineScope()
    MinePage(request, state.profile ?: request.profile, navigation, dependencies,
        layout, previews, { kind -> scope.launch { preview.retry(kind) } })
}

@Composable
private fun MinePage(
    request: MineScreenRequest, shown: com.aritxonly.bdfun.core.model.user.UserProfile?,
    navigation: MineNavigation, dependencies: MineDependencies, layout: MineLayout,
    previews: MinePreviewUiState, retry: (MinePreviewKind) -> Unit,
) {
    val shape = remember { ContinuousCornerShape(22.dp) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 14.dp, top = 20.dp, end = 14.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (request.profile == null) {
                item { MineGuest(shape, navigation.onLogin) }
                if (request.showSentHistory) item { MineRow("我的评论与弹幕", onClick = navigation.onOpenSentHistory) }
                item { SettingsBar(navigation.onOpenSettings) }
            } else if (shown != null) {
                if (wide) item {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Box(Modifier.weight(0.38f)) { MineHero(shown, navigation) }
                        Column(Modifier.weight(0.62f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            MinePageContent(request, navigation, dependencies, layout, previews, retry)
                        }
                    }
                } else {
                    item { MineHero(shown, navigation) }
                    item { MineQuickActions(shape, navigation) }
                    if (layout.history) item { MinePreviewCard(MinePreviewKind.History, previews[MinePreviewKind.History], navigation) { retry(MinePreviewKind.History) } }
                    if (layout.favorites) item { MinePreviewCard(MinePreviewKind.Favorites, previews[MinePreviewKind.Favorites], navigation) { retry(MinePreviewKind.Favorites) } }
                    if (layout.watchLater) item { MinePreviewCard(MinePreviewKind.WatchLater, previews[MinePreviewKind.WatchLater], navigation) { retry(MinePreviewKind.WatchLater) } }
                    if (request.showSentHistory) item { MineRecent(shape, dependencies.recent(), navigation) }
                    item { SettingsBar(navigation.onOpenSettings) }
                }
            }
        }
    }
}

@Composable
private fun MinePageContent(
    request: MineScreenRequest, navigation: MineNavigation, dependencies: MineDependencies,
    layout: MineLayout, previews: MinePreviewUiState, retry: (MinePreviewKind) -> Unit,
) {
    val shape = remember { ContinuousCornerShape(22.dp) }
    MineQuickActions(shape, navigation)
    val enabled = listOf(MinePreviewKind.History to layout.history,
        MinePreviewKind.Favorites to layout.favorites, MinePreviewKind.WatchLater to layout.watchLater)
    enabled.filter { it.second }.forEach { (kind, _) ->
        MinePreviewCard(kind, previews[kind], navigation) { retry(kind) }
    }
    if (request.showSentHistory) MineRecent(shape, dependencies.recent(), navigation)
    SettingsBar(navigation.onOpenSettings)
}
