package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.ui.*
import com.aritxonly.teak.common.chrome.*
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

import com.aritxonly.bdfun.feature.relations.application.*
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings

@Composable
internal fun RelationScreen(mid: Long, fans: Boolean, currentMid: Long?, settings: SettingsBindings,
    createController: (Boolean) -> RelationFeedStateHolder, onBack: () -> Unit, onOpenSpace: (Long) -> Unit) {
    val order = rememberSaveable(mid, fans, saver = RelationOrderStateHolder.Saver) { RelationOrderStateHolder(false) }
    val controller = remember(mid, fans, order.frequent, currentMid) { createController(order.frequent) }
    RelationPageContent(controller, order, fans, currentMid, settings, onBack, onOpenSpace)
}

@Composable
private fun RelationPageContent(controller: RelationFeedStateHolder, order: RelationOrderStateHolder,
    fans: Boolean, currentMid: Long?, settings: SettingsBindings, onBack: () -> Unit, onOpenSpace: (Long) -> Unit) {
    val context = LocalContext.current
    val parentScope = rememberCoroutineScope()
    val scope = remember(controller, parentScope) {
        kotlinx.coroutines.CoroutineScope(parentScope.coroutineContext + kotlinx.coroutines.SupervisorJob(parentScope.coroutineContext[kotlinx.coroutines.Job]))
    }
    val state by controller.state.collectAsState()
    val listState = rememberLazyListState()
    val title = if (fans) "粉丝" else "关注"
    DisposableEffect(controller, scope) { onDispose { controller.close(); scope.coroutineContext[kotlinx.coroutines.Job]?.cancel() } }
    LaunchedEffect(controller) { listState.scrollToItem(0); controller.fetch(reset = true) }
    MessageResumeEffect { controller.fetch(reset = true) }
    TeakActivityScaffold(
        settings = settings,
        onBack = onBack,
        options = ActivityPageOptions(
            title = title,
        ),
        slots = ActivityPageSlots(
            topBarActions = {
            TeakTopGlassButton(onClick = { scope.launch { controller.fetch(reset = true) } }) {
                Icon(MiuixIcons.Refresh, contentDescription = "刷新$title", tint = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp))
            }
        },
        ),
    ) {
        val colors = MiuixTheme.colorScheme
        val dark = colors.background.luminance() < 0.5f
        SideEffect { context.findHostActivity()?.syncSystemBarIcons(dark) }
        TrackTeakPageScroll(listState)
        RegisterScrollTop(listState, "relations")
        RelationFeedList(RelationScreenRuntime(controller, state, listState, scope, title, fans, order, currentMid, onOpenSpace))
    }
}
internal class RelationOrderStateHolder(initial: Boolean) {
    var frequent by mutableStateOf(initial)
    companion object {
        val Saver = androidx.compose.runtime.saveable.Saver<RelationOrderStateHolder, Boolean>(
            save = { it.frequent }, restore = { RelationOrderStateHolder(it) })
    }
}
internal class RelationScreenRuntime(val controller: RelationFeedStateHolder, val state: RelationFeedState,
    val listState: androidx.compose.foundation.lazy.LazyListState, val scope: kotlinx.coroutines.CoroutineScope,
    val title: String, val fans: Boolean, val order: RelationOrderStateHolder, val currentMid: Long?, val onOpenSpace: (Long) -> Unit)
