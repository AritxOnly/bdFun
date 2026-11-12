package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.model.message.MessageItem
import com.aritxonly.bdfun.core.model.message.MessageSession
import kotlinx.coroutines.*

internal class MessageSessionsPresentationStateHolder(readAll: Int, more: Int) {
    var tabIndex by mutableIntStateOf(0)
    val tabProgress = mutableFloatStateOf(0f)
    var consumedReadAll by mutableIntStateOf(readAll)
    var consumedMore by mutableIntStateOf(more)
}

internal class MessageSessionMenuStateHolder {
    var selected by mutableStateOf<MessageSession?>(null)
    var confirmDelete by mutableStateOf(false)
    var showMore by mutableStateOf(false)
}

internal class MessageFeedPresentationStateHolder {
    var tab by mutableIntStateOf(0)
    val tabProgress = mutableFloatStateOf(0f)
}

internal class MessageFeedMenuStateHolder {
    var selected by mutableStateOf<Pair<Int, MessageItem>?>(null)
    var confirmDelete by mutableStateOf(false)
}

internal data class MessageScreenEvents(
    val onNeedLogin: () -> Unit,
    val onOpenReplies: () -> Unit,
    val onOpenLikes: () -> Unit,
    val onOpenFans: () -> Unit,
)

@Composable
internal fun rememberMessageTaskScope(owner: Any, onClose: () -> Unit): CoroutineScope {
    val parent = rememberCoroutineScope()
    val scope = remember(owner) { CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job])) }
    DisposableEffect(owner) {
        onDispose { scope.cancel(); onClose() }
    }
    return scope
}

internal fun messageFeedTabs(kind: MessageFeedKind) = when (kind) {
    MessageFeedKind.Reply -> listOf("全部", "我关注的人", "@我")
    MessageFeedKind.Like -> listOf("全部", "我关注的人")
    else -> emptyList()
}

internal fun messageFeedTitle(kind: MessageFeedKind) = when (kind) {
    MessageFeedKind.Reply -> "回复与@"
    MessageFeedKind.Like -> "收到喜欢"
    MessageFeedKind.Fans -> "新增粉丝"
    MessageFeedKind.System -> "系统通知"
}
