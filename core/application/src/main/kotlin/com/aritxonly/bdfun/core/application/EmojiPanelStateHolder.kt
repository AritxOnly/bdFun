package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.model.comment.EmoteItem
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 面板按原逻辑在首次 ensure 结束后取一次表情，不改变已经打开的面板快照。 */
class EmojiPanelStateHolder(private val store: EmoteStore) {
    private val mutable = MutableStateFlow(store.state.value.items)
    val state = mutable.asStateFlow()
    private var closed = false
    fun close() { closed = true }
    suspend fun load() {
        if (closed) return
        store.ensure()
        currentCoroutineContext().ensureActive()
        if (!closed) mutable.value = store.state.value.items
    }
}

data class EmojiCell(val pick: String, val url: String?)

private val fallbackEmojis = listOf(
    "\uD83D\uDE00", "\uD83D\uDE02", "\uD83E\uDD70", "\uD83D\uDE0E", "\uD83E\uDD14", "\uD83D\uDE2D", "\uD83D\uDE21", "\uD83E\uDD73",
    "\uD83D\uDC4D", "\uD83D\uDC4E", "\u2764\uFE0F", "\uD83D\uDD25", "\uD83C\uDF89", "\u2728", "\uD83D\uDCAF", "\uD83D\uDC36",
)
fun emojiCells(items: List<EmoteItem>): List<EmojiCell> = if (items.isEmpty()) fallbackEmojis.map { EmojiCell(it, null) }
    else items.map { EmojiCell("[" + it.text + "]", it.url) }
