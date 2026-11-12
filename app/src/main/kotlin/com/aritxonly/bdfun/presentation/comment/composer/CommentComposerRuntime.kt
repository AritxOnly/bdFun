package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.feature.comment.application.CommentComposerStateHolder
import kotlinx.coroutines.*

internal class CommentComposerPresentationStateHolder {
    val focusRequester = FocusRequester()
    var pickerOwner: CommentComposerStateHolder? = null
}
internal data class CommentComposerRuntime(val controller: CommentComposerStateHolder, val scope: CoroutineScope)

@Composable
internal fun rememberCommentComposerRuntime(page: VideoPagePresentationState): CommentComposerRuntime {
    val owner = AppGraph.session.value?.mid
    val parent = rememberCoroutineScope()
    val controller = remember(owner, page) { AppGraph.commentComposerStateHolder() }
    val scope = remember(controller) { CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job])) }
    DisposableEffect(controller) { onDispose { scope.cancel(); controller.close() } }
    return CommentComposerRuntime(controller, scope)
}
