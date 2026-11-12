package com.aritxonly.bdfun

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.repeatOnLifecycle
import com.aritxonly.bdfun.core.model.message.MessageSession
import com.aritxonly.bdfun.feature.message.application.PrivateChatUiState
import com.aritxonly.bdfun.ui.LocalRichTextDependencies

@Composable
internal fun PrivateChatEffects(
    runtime: PrivateChatRuntime, ui: PrivateChatPresentationStateHolder,
    session: MessageSession, content: PrivateChatUiState, listState: LazyListState,
) {
    val controller = runtime.controller
    val emotes = LocalRichTextDependencies.current.emotes
    LaunchedEffect(Unit) { emotes.ensure() }
    LaunchedEffect(controller, session.talkerId, ui.reloadTick) {
        controller.load()
        val current = controller.state.value
        com.aritxonly.bdfun.data.LogCollector.record("CHAT",
            "loaded talker=${session.talkerId} count=${current.messages.size} error=${current.loadError}")
    }
    // 长期 effect 在动作发生时读取控制器，而非固定在首次组合的投影上。
    LaunchedEffect(controller, content.messages.lastOrNull()?.id) {
        val current = controller.state.value
        if (!current.loading && current.messages.isNotEmpty() && listState.firstVisibleItemIndex < 2) {
            listState.scrollToItem(0)
        }
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(controller, lifecycleOwner, session.talkerId) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            while (true) {
                kotlinx.coroutines.delay(8000)
                controller.pollLatest { listState.firstVisibleItemIndex == 0 }
            }
        }
    }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(controller, imeBottom) {
        if (imeBottom > 0 && controller.state.value.messages.isNotEmpty()) listState.scrollToItem(0)
    }
}
