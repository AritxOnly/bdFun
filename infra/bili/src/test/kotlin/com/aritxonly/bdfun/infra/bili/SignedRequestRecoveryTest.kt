package com.aritxonly.bdfun.infra.bili

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class SignedRequestRecoveryTest {
    @Test fun deniedSignedReadRefreshesExactlyOnceAndReturnsTheRetry() = runBlocking {
        var calls = 0; var refreshes = 0
        val value = recoverSignedRead(true, "GET", { if (++calls == 1) "denied" else "ok" }, { it == "denied" }, { refreshes++ })
        assertEquals("ok", value); assertEquals(2, calls); assertEquals(1, refreshes)
    }
    @Test fun secondRejectionCannotLoop() = runBlocking {
        var calls = 0; var refreshes = 0
        assertEquals("denied", recoverSignedRead(true, "GET", { calls++; "denied" }, { true }, { refreshes++ }))
        assertEquals(2, calls); assertEquals(1, refreshes)
    }
    @Test fun postAndUnsignedReadsAreNeverRepeated() = runBlocking {
        for ((signed, method) in listOf(true to "POST", false to "GET")) {
            var calls = 0; var refreshes = 0
            recoverSignedRead(signed, method, { calls++; "denied" }, { true }, { refreshes++ })
            assertEquals(1, calls); assertEquals(0, refreshes)
        }
    }
    @Test fun failedRefreshRetainsTheOriginalServerRejection() = runBlocking {
        assertEquals("denied", recoverSignedRead(true, "GET", { "denied" }, { true }, { throw IOException("offline") }))
        val original = BiliException(403, "forbidden")
        try {
            recoverSignedRead<String>(true, "GET", { throw original }, { false }, { throw IOException("offline") })
            fail("必须保留原错误")
        } catch (failure: BiliException) { assertSame(original, failure) }
    }
    @Test fun riskErrorsAreNotRetriedAndCancellationStillPropagates() = runBlocking {
        var refreshes = 0
        try {
            recoverSignedRead<String>(true, "GET", { throw BiliException(-412, "risk") }, { false }, { refreshes++ })
            fail("风控错误必须传播")
        } catch (failure: BiliException) { assertEquals(-412, failure.code) }
        assertEquals(0, refreshes)
        try {
            recoverSignedRead(true, "GET", { "denied" }, { true }, { throw CancellationException("leave") })
            fail("取消必须传播")
        } catch (cancel: CancellationException) { assertEquals("leave", cancel.message) }
    }
}
