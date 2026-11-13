package com.aritxonly.bdfun

import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.model.message.MessageSession
import com.aritxonly.bdfun.feature.message.application.PrivateChatStateHolder
import kotlinx.coroutines.*

internal class PrivateChatPresentationStateHolder {
    var reloadTick by mutableIntStateOf(0)
    var imagePickerOwner: PrivateChatStateHolder? = null
}
internal data class PrivateChatRuntime(val controller: PrivateChatStateHolder, val scope: CoroutineScope)

@Composable
internal fun rememberPrivateChatRuntime(session: MessageSession): PrivateChatRuntime {
    val owner = AppGraph.session.identity
    val parent = rememberCoroutineScope()
    val controller = remember(owner, session.talkerId, session.sessionType) { AppGraph.privateChatStateHolder(session) }
    val scope = remember(controller) {
        CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    }
    DisposableEffect(controller) {
        onDispose { scope.cancel(); controller.close() }
    }
    return PrivateChatRuntime(controller, scope)
}
