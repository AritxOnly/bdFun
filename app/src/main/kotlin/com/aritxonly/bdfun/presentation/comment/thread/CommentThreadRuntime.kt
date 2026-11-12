package com.aritxonly.bdfun.ui

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.core.domain.CommentQuery
import com.aritxonly.bdfun.core.model.comment.CommentItem
import com.aritxonly.bdfun.feature.comment.application.CommentThreadStateHolder
import kotlinx.coroutines.*

internal class CommentThreadRuntime(
    val controller: CommentThreadStateHolder,
    val transient: CommentThreadPresentationStateHolder,
    val scope: CoroutineScope,
    val context: Context,
    val listState: LazyListState,
    val focusRequester: FocusRequester,
    private val focusManager: FocusManager,
    private val keyboard: SoftwareKeyboardController?,
) {
    val actions get() = controller.actions
    fun exitReply() {
        transient.replyTarget = null
        transient.draft = ""
        focusManager.clearFocus()
        keyboard?.hide()
    }
    fun send(input: CommentThreadInput, sending: Boolean) {
        if (!controller.isActive) return
        val target = transient.replyTarget ?: return
        val text = transient.draft.trim()
        if (text.isEmpty() || sending) return
        scope.launch {
            val result = controller.send(text, target.rpid, AppGraph.session.value, System::currentTimeMillis)
            currentCoroutineContext().ensureActive()
            if (!controller.isActive) return@launch
            if (result != null) {
                AppGraph.sentHistory.record("comment", text, input.bvid, input.videoTitle)
                transient.draft = ""
                transient.replyTarget = null
                keyboard?.hide()
                context.showToast("回复成功")
            } else context.showToast("发送失败，请确认已登录")
        }
    }
    fun toggleLike(item: CommentItem) {
        scope.launch { actions.toggleLike(item)?.let { context.showToast(it) } }
    }
    fun dismissMenu(after: (() -> Unit)? = null) {
        transient.menuVisible = false
        scope.launch {
            delay(360)
            transient.menuReply = null
            if (controller.isActive) after?.invoke()
        }
    }
}

@Composable
internal fun rememberCommentThreadRuntime(input: CommentThreadInput): CommentThreadRuntime {
    val owner = AppGraph.session.value?.mid
    val generation by AppGraph.commentLikeGeneration.collectAsState()
    val page = LocalVideoPageState.current
    val parent = rememberCoroutineScope()
    val actions = remember(owner, generation, page, input.commentOid, input.commentType, input.comment.rpid) {
        AppGraph.commentActionsStateHolder(CommentQuery(input.commentOid, input.commentType, 0, input.comment.rpid))
    }
    val controller = remember(actions) { CommentThreadStateHolder(actions, input.comment.rpid) }
    val scope = remember(controller) { CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job])) }
    DisposableEffect(controller) { onDispose { controller.close(); actions.close(); scope.cancel() } }
    val transient = remember(owner, input.commentOid, input.commentType, input.comment.rpid) {
        CommentThreadPresentationStateHolder()
    }
    return CommentThreadRuntime(controller, transient, scope, LocalContext.current, rememberLazyListState(),
        remember { FocusRequester() }, LocalFocusManager.current, LocalSoftwareKeyboardController.current)
}
