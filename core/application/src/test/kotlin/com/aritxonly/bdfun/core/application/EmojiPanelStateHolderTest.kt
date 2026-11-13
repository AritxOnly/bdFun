package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.EmoteRepository
import com.aritxonly.bdfun.core.model.comment.EmoteItem
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class EmojiPanelStateHolderTest {
    private class Fake : EmoteRepository {
        var calls = 0
        var gate: CompletableDeferred<Unit>? = null
        val items = listOf(EmoteItem("doge", "a"), EmoteItem("[笑]", "b"))
        override suspend fun replyEmotes(): Outcome<List<EmoteItem>> {
            calls++; gate?.await(); return Outcome.Success(items)
        }
    }
    @Test fun cachedPackIsImmediatelyVisibleAndPickKeepsOriginalBrackets() = runBlocking {
        val fake = Fake(); val store = EmoteStore(fake); store.ensure()
        val holder = EmojiPanelStateHolder(store); holder.load()
        assertEquals(fake.items, holder.state.value); assertEquals(1, fake.calls)
        assertEquals(listOf(EmojiCell("[doge]", "a"), EmojiCell("[笑]", "b")), emojiCells(holder.state.value))
    }
    @Test fun textEmotesKeepLiteralTextAndDoNotLoadImages() {
        assertEquals(listOf(EmojiCell("(´･_･`)", null)),
            emojiCells(listOf(EmoteItem("(´･_･`)", "", type = 4))))
    }
    @Test fun initialFallbackKeepsSixteenOriginalChoices() {
        assertEquals(listOf("😀", "😂", "🥰", "😎", "🤔", "😭", "😡", "🥳", "👍", "👎", "❤️", "🔥", "🎉", "✨", "💯", "🐶"), emojiCells(emptyList()).map { it.pick })
        assertTrue(emojiCells(emptyList()).all { it.url == null })
    }
    @Test fun anotherPreloadDoesNotContinuouslyChangeOpenPanelSnapshot() = runBlocking {
        val fake = Fake().apply { gate = CompletableDeferred() }; val store = EmoteStore(fake)
        val preload = launch(start = CoroutineStart.UNDISPATCHED) { store.ensure() }
        val holder = EmojiPanelStateHolder(store); holder.load()
        fake.gate!!.complete(Unit); preload.join()
        assertTrue(holder.state.value.isEmpty())
        assertEquals(fake.items, EmojiPanelStateHolder(store).state.value)
    }
    @Test fun closePreventsLateOrFuturePanelUpdates() = runBlocking {
        val fake = Fake().apply { gate = CompletableDeferred() }; val holder = EmojiPanelStateHolder(EmoteStore(fake))
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.load() }
        holder.close(); fake.gate!!.complete(Unit); job.join(); holder.load()
        assertTrue(holder.state.value.isEmpty()); assertEquals(1, fake.calls)
    }
}
