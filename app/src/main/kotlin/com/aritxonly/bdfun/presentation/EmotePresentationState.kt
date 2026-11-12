package com.aritxonly.bdfun.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.core.application.EmoteStore
import com.aritxonly.bdfun.core.model.comment.EmoteItem
import kotlinx.coroutines.*

/** 与原来的独立字段一样观察，加载标记变化不重组所有正文。 */
internal class EmotePresentationState(private val store: EmoteStore, scope: CoroutineScope) {
    var items by mutableStateOf<List<EmoteItem>>(store.state.value.items)
        private set
    var byText by mutableStateOf(store.state.value.byText)
        private set
    init { scope.launch(start = CoroutineStart.UNDISPATCHED) { store.state.collect { refresh() } } }
    private fun refresh() { items = store.state.value.items; byText = store.state.value.byText }
    suspend fun ensure() { store.ensure(); refresh() }
}
