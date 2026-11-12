package com.aritxonly.bdfun.feature.comment.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.CommentImageRepository
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CommentComposerStateHolderTest {
    private class Images : CommentImageRepository {
        val calls = mutableListOf<String>()
        var action: suspend (String) -> Outcome<String?> = { Outcome.Success("pictures:$it") }
        override suspend fun upload(reference: String): Outcome<String?> { calls += reference; return action(reference) }
    }
    private suspend fun attach(holder: CommentComposerStateHolder, reference: String = "one") {
        assertTrue(holder.selectImage(reference)); holder.upload(reference)
    }

    @Test fun emptyOrUploadingDraftDoesNotReadSenderAndMissingSenderMarksFailure() = runBlocking {
        val images = Images(); val holder = CommentComposerStateHolder(images)
        assertNull(holder.beginSend { error("空输入不能读取发送器") })
        holder.setDraft("text"); holder.selectImage("one")
        assertNull(holder.beginSend { error("上传时不能读取发送器") })
        holder.upload("one")
        assertNull(holder.beginSend { null })
        assertTrue(holder.state.value.failed); assertFalse(holder.state.value.sending)
        assertEquals("text", holder.state.value.draft)
    }

    @Test fun sendCapturesTrimmedTextAndSenderButReadsPictureAtExecution() = runBlocking {
        val images = Images(); val holder = CommentComposerStateHolder(images)
        attach(holder); holder.setDraft("  old text  ")
        val sent = mutableListOf<Pair<String, String?>>()
        val request = holder.beginSend { { text, pictures -> sent += text to pictures; true } }!!
        assertTrue(holder.state.value.sending)
        assertNull(holder.beginSend { error("发送中不能重复读取发送器") })
        holder.setDraft("new draft"); attach(holder, "two")
        assertTrue(holder.send(request))
        assertEquals(listOf("old text" to "pictures:two"), sent)
        assertEquals("", holder.state.value.draft)
        assertNull(holder.state.value.pendingPictures); assertNull(holder.state.value.pendingPreview)
    }

    @Test fun successfulSendUnconditionallyClearsDraftEditedWhileWaiting() = runBlocking {
        val holder = CommentComposerStateHolder(Images()); holder.setDraft("one")
        val gate = CompletableDeferred<Boolean>()
        val request = holder.beginSend { { _, _ -> gate.await() } }!!
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.send(request) }
        holder.setDraft("edited during send"); gate.complete(true)
        assertTrue(job.await()); assertEquals("", holder.state.value.draft)
        assertFalse(holder.state.value.sending)
    }

    @Test fun falseOrThrownSendKeepsOriginalDraftAndAttachmentsForRetry() = runBlocking {
        for (throws in listOf(false, true)) {
            val holder = CommentComposerStateHolder(Images()); attach(holder); holder.setDraft("  draft  ")
            val request = holder.beginSend { { _, _ -> if (throws) error("offline") else false } }!!
            assertFalse(holder.send(request)); assertEquals("  draft  ", holder.state.value.draft)
            assertEquals("pictures:one", holder.state.value.pendingPictures)
            assertEquals("one", holder.state.value.pendingPreview)
            assertTrue(holder.state.value.failed); assertFalse(holder.state.value.sending)
            assertNotNull(holder.beginSend { { _, _ -> true } })
            assertFalse(holder.state.value.failed)
        }
    }

    @Test fun cancellationPropagatesAndReleasesSendingWithoutShowingFailure() = runBlocking {
        val holder = CommentComposerStateHolder(Images()); holder.setDraft("draft")
        val gate = CompletableDeferred<Boolean>()
        val request = holder.beginSend { { _, _ -> gate.await() } }!!
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.send(request) }
        job.cancelAndJoin(); assertTrue(job.isCancelled)
        assertFalse(holder.state.value.sending); assertFalse(holder.state.value.failed)
        assertEquals("draft", holder.state.value.draft)
        assertNotNull(holder.beginSend { { _, _ -> true } })
    }

    @Test fun failedNewUploadClearsPreviewButKeepsPreviousPicturesAndAllowsImageOnlySend() = runBlocking {
        for (result in listOf(Outcome.Success(null), Outcome.Failure(AppError.Network("offline")))) {
            val images = Images(); val holder = CommentComposerStateHolder(images); attach(holder)
            images.action = { result }; holder.selectImage("two"); holder.upload("two")
            assertNull(holder.state.value.pendingPreview)
            assertEquals("pictures:one", holder.state.value.pendingPictures)
            assertFalse(holder.state.value.uploading); assertTrue(holder.state.value.failed)
            val request = holder.beginSend { { text, pictures -> text.isEmpty() && pictures == "pictures:one" } }!!
            assertTrue(holder.send(request))
        }
    }

    @Test fun emptyEncodedPictureValueRemainsANonNullAttachment() = runBlocking {
        val images = Images(); images.action = { Outcome.Success("") }
        val holder = CommentComposerStateHolder(images); attach(holder)
        assertEquals("", holder.state.value.pendingPictures); assertEquals("one", holder.state.value.pendingPreview)
        val request = holder.beginSend { { text, pictures -> text.isEmpty() && pictures == "" } }!!
        assertTrue(holder.send(request))
    }

    @Test fun olderUploadCanCompleteAfterNewerFailureWithoutClearingItsFailureFlag() = runBlocking {
        val gate = CompletableDeferred<Outcome<String?>>()
        val images = Images(); images.action = { if (it == "one") gate.await() else Outcome.Success(null) }
        val holder = CommentComposerStateHolder(images)
        holder.selectImage("one")
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.upload("one") }
        holder.selectImage("two"); holder.upload("two")
        assertTrue(holder.state.value.failed); assertNull(holder.state.value.pendingPreview)
        gate.complete(Outcome.Success("pictures:old")); job.join()
        assertEquals("pictures:old", holder.state.value.pendingPictures)
        assertNull(holder.state.value.pendingPreview); assertTrue(holder.state.value.failed)
    }

    @Test fun uploadCancellationReleasesBusyAndKeepsPreviewWithoutFailure() = runBlocking {
        val gate = CompletableDeferred<Outcome<String?>>()
        val images = Images(); val holder = CommentComposerStateHolder(images); attach(holder)
        images.action = { gate.await() }; holder.selectImage("two")
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.upload("two") }
        job.cancelAndJoin()
        assertFalse(holder.state.value.uploading); assertFalse(holder.state.value.failed)
        assertEquals("two", holder.state.value.pendingPreview)
        assertEquals("pictures:one", holder.state.value.pendingPictures)
    }

    @Test fun blankDismissHidesEditorThenClearsTargetBeforeAttachmentsAndKeepsFailure() = runBlocking {
        val holder = CommentComposerStateHolder(Images()); attach(holder)
        holder.setDraft("draft"); assertNull(holder.beginSend { null })
        holder.setDraft("  "); holder.toggleEmoji()
        val order = mutableListOf<String>()
        holder.dismiss({
            assertFalse(holder.state.value.showEmoji); assertFalse(holder.state.value.editing)
            order += "platform"
        }, {
            assertEquals("pictures:one", holder.state.value.pendingPictures)
            order += "target"
        })
        assertEquals(listOf("platform", "target"), order)
        assertNull(holder.state.value.pendingPictures); assertNull(holder.state.value.pendingPreview)
        assertTrue(holder.state.value.failed)
    }

    @Test fun nonBlankDismissKeepsDraftTargetAndPictures() = runBlocking {
        val holder = CommentComposerStateHolder(Images()); attach(holder); holder.setDraft("draft"); holder.toggleEmoji()
        holder.dismiss({}, { error("有草稿时不能清回复目标") })
        assertEquals("draft", holder.state.value.draft); assertEquals("pictures:one", holder.state.value.pendingPictures)
        assertFalse(holder.state.value.showEmoji); assertFalse(holder.state.value.editing)
    }

    @Test fun dismissReadsDraftAfterSynchronousImeCommit() = runBlocking {
        val holder = CommentComposerStateHolder(Images()); attach(holder)
        holder.dismiss({ holder.setDraft("IME committed") }, { error("刚提交的草稿要保留") })
        assertEquals("IME committed", holder.state.value.draft)
        assertEquals("pictures:one", holder.state.value.pendingPictures)
    }

    @Test fun uploadFinishingAfterBlankDismissKeepsOriginalHiddenPictureBehavior() = runBlocking {
        val gate = CompletableDeferred<Outcome<String?>>()
        val images = Images(); images.action = { gate.await() }
        val holder = CommentComposerStateHolder(images); holder.selectImage("one")
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.upload("one") }
        holder.dismiss({}, {}); assertNull(holder.state.value.pendingPreview)
        gate.complete(Outcome.Success("pictures:late")); job.join()
        assertEquals("pictures:late", holder.state.value.pendingPictures); assertNull(holder.state.value.pendingPreview)
    }

    @Test fun successfulSendDoesNotClearFailureIntroducedByConcurrentImageUpload() = runBlocking {
        val images = Images(); images.action = { Outcome.Success(null) }
        val holder = CommentComposerStateHolder(images); holder.setDraft("draft")
        val gate = CompletableDeferred<Boolean>()
        val request = holder.beginSend { { _, _ -> gate.await() } }!!
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.send(request) }
        holder.selectImage("one"); holder.upload("one")
        assertTrue(holder.state.value.failed)
        gate.complete(true); assertTrue(job.await()); holder.dismiss({}, {})
        assertEquals("", holder.state.value.draft); assertTrue(holder.state.value.failed)
    }

    @Test fun closedComposerRejectsLateUploadAndFutureUiOrNetworkEvents() = runBlocking {
        val gate = CompletableDeferred<Outcome<String?>>()
        val images = Images(); images.action = { gate.await() }
        val holder = CommentComposerStateHolder(images); holder.selectImage("one")
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.upload("one") }
        holder.close(); val closed = holder.state.value
        gate.complete(Outcome.Success("pictures:late")); job.join()
        holder.setDraft("late"); holder.appendEmoji("emoji"); holder.setEditing(true); holder.toggleEmoji()
        assertFalse(holder.selectImage("two")); holder.upload("two")
        holder.dismiss({ error("已退出不能操作 UI") }, { error("已退出不能清目标") })
        assertNull(holder.beginSend { error("已退出不能读取发送器") })
        assertEquals(closed, holder.state.value); assertEquals(listOf("one"), images.calls)
    }

    @Test fun closedComposerSuppressesSuccessfulSendAndPreservesClosedSnapshot() = runBlocking {
        val holder = CommentComposerStateHolder(Images()); holder.setDraft("draft")
        val gate = CompletableDeferred<Boolean>()
        val request = holder.beginSend { { _, _ -> gate.await() } }!!
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.send(request) }
        holder.close(); val closed = holder.state.value
        gate.complete(true); assertFalse(job.await()); assertEquals(closed, holder.state.value)
        assertFalse(holder.send(request))
    }
}
