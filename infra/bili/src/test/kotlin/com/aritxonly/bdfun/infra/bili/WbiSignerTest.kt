package com.aritxonly.bdfun.infra.bili

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class WbiSignerTest {
    private val pair = "abcdefghijklmnopqrstuvwxyzABCDEF" to "GHIJKLMNOPQRSTUVWXYZ0123456789ab"
    private fun signer(loader: suspend () -> Pair<String, String>?): WbiSigner =
        WbiSigner(loader, { it })

    @Test fun freshKeysAreSharedAcrossRequests() = runBlocking {
        var calls = 0
        val signer = signer { calls++; pair }
        assertSame(signer.keys(), signer.keys())
        assertEquals(1, calls)
    }

    @Test fun concurrentRefreshLoadsOnce() = runBlocking {
        var calls = 0
        val signer = signer { calls++; delay(10); pair }
        val keys = List(10) { async { signer.keys() } }.awaitAll()
        assertEquals(1, calls)
        assertTrue(keys.all { it === keys.first() })
    }

    @Test fun expiredKeysRefreshAtSixHours() = runBlocking {
        var now = 0L
        var calls = 0
        val signer = WbiSigner({ calls++; pair }, { it }, { now })
        val first = signer.keys()
        now = 6 * 60 * 60 * 1000L - 1
        assertSame(first, signer.keys())
        now++
        assertNotSame(first, signer.keys())
        assertEquals(2, calls)
    }

    @Test fun failedRefreshKeepsPreviousKeys() = runBlocking {
        var now = 0L
        val signer = WbiSigner({ throw IllegalStateException("offline") }, { it }, { now })
        signer.update(pair.first, pair.second)
        val old = signer.keys()
        now = 6 * 60 * 60 * 1000L
        assertSame(old, signer.keys())
    }

    @Test fun missingKeysExposeExistingFailure() = runBlocking {
        val signer = signer { null }
        try {
            signer.keys()
            fail("缺失签名密钥必须报错")
        } catch (e: BiliException) {
            assertEquals(-1, e.code)
            assertEquals("无法获取请求签名，请检查网络", e.message)
        }
    }

    @Test fun cancellationDoesNotFallBackToExpiredKeys() = runBlocking {
        var now = 0L
        val signer = WbiSigner({ throw CancellationException("cancelled") }, { it }, { now })
        signer.update(pair.first, pair.second)
        now = 6 * 60 * 60 * 1000L
        try {
            signer.keys()
            fail("取消必须继续传播")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }
    }

    @Test fun logoutClearsKeysAndBlankUpdatesAreIgnored() = runBlocking {
        var calls = 0
        val signer = signer { calls++; pair }
        signer.update("", pair.second)
        assertFalse(signer.hasKeys)
        signer.keys()
        signer.clear()
        assertFalse(signer.hasKeys)
        signer.keys()
        assertEquals(2, calls)
    }

    @Test fun queryRetainsFilteringOrderingAndTimestamp() = runBlocking {
        val signer = WbiSigner({ pair }, { it }, { 1700000000000L })
        val params = linkedMapOf("z" to "last", "a" to "c!l'e(a)n*")
        val query = signer.signQuery(params)
        assertEquals("1700000000", params["wts"])
        assertTrue(query.startsWith("a=clean&wts=1700000000&z=last&w_rid="))
        assertEquals("99cf0187199be6c09e097c2821aee153", query.substringAfter("w_rid="))
    }
    @Test fun forcedRefreshBypassesFreshSnapshot() = runBlocking {
        var calls = 0
        val signer = signer { calls++; pair }
        val previous = signer.keys()
        signer.forceRefresh()
        assertNotSame(previous, signer.keys())
        assertEquals(2, calls)
    }

    @Test fun failedForcedRefreshRestoresPreviousSnapshot() = runBlocking {
        var fail = false
        val signer = signer { if (fail) throw IllegalStateException("offline") else pair }
        val previous = signer.keys()
        fail = true
        try { signer.forceRefresh(); fail("刷新必须报告失败") } catch (_: BiliException) { }
        assertSame(previous, signer.keys())
    }

}
