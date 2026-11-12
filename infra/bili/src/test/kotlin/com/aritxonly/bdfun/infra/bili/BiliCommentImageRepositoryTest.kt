package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.common.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BiliCommentImageRepositoryTest {
    @Test fun uploadForwardsReferenceAndKeepsEncodedBodyUnchanged() = runBlocking {
        var received = ""
        val raw = "  encoded-pictures  "
        val repository = BiliCommentImageRepository { reference -> received = reference; raw }
        assertEquals(Outcome.Success(raw), repository.upload("content://picker/图片%20a"))
        assertEquals("content://picker/图片%20a", received)
    }
    @Test fun nullableBackendFailureRemainsNullAndExceptionsBecomeOutcomeFailure() = runBlocking {
        assertEquals(Outcome.Success(null), BiliCommentImageRepository { null }.upload("one"))
        assertTrue(BiliCommentImageRepository { error("offline") }.upload("one") is Outcome.Failure)
    }
    @Test fun cancellationPropagates() = runBlocking {
        try {
            BiliCommentImageRepository { throw CancellationException("leave") }.upload("one")
            fail("取消必须传播")
        } catch (cancel: CancellationException) { assertEquals("leave", cancel.message) }
    }
}
