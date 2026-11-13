package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToVideo
import com.aritxonly.bdfun.navigateToDynamic
import android.content.Context
import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.model.feed.DynamicItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.dynamic.application.DynamicDetailStateHolder
import com.aritxonly.bdfun.ui.showToast
import kotlinx.coroutines.*

internal data class DynamicDetailRuntime(val controller: DynamicDetailStateHolder, val scope: CoroutineScope)

@Composable
internal fun rememberDynamicDetailRuntime(dynamic: DynamicItem): DynamicDetailRuntime {
    val owner = AppGraph.session.value?.mid
    val parent = rememberCoroutineScope()
    val controller = remember(owner, dynamic.id) { AppGraph.dynamicDetailStateHolder(dynamic) }
    val scope = remember(controller) { CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job])) }
    DisposableEffect(controller) { onDispose { scope.cancel(); controller.close() } }
    return DynamicDetailRuntime(controller, scope)
}

internal class DynamicDetailActions(
    private val controller: DynamicDetailStateHolder,
    private val scope: CoroutineScope,
    private val context: Context,
) {
    fun like() {
        val next = controller.beginLike() ?: return
        scope.launch { controller.commitLike(next)?.let { context.showToast(it) } }
    }
    fun openVideo(video: VideoItem) { navigateToVideo(context, video) }
    fun openForwarded(item: DynamicItem) { navigateToDynamic(context, item) }
}
