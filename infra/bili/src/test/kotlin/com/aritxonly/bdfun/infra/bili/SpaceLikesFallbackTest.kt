package com.aritxonly.bdfun.infra.bili

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SpaceLikesFallbackTest {
    @Test fun positiveInfoCountSkipsFallbackRequest() = runBlocking {
        var requested = false
        val result = resolveSpaceLikes(JSONObject().put("likes", 7), JSONObject().put("likes", 99)) {
            requested = true; 88
        }
        assertEquals(7L, result); assertFalse(requested)
    }
    @Test fun missingZeroAndNegativeInfoUseExactlyOnePositiveNetworkResult() = runBlocking {
        for (info in listOf(JSONObject(), JSONObject().put("likes", 0), JSONObject().put("likes", -1))) {
            var requests = 0
            val result = resolveSpaceLikes(info, JSONObject().put("likes", 99)) { requests++; 88 }
            assertEquals(88L, result); assertEquals(1, requests)
        }
    }
    @Test fun zeroNegativeAndFailedNetworkReadRetainOriginalNavFallback() = runBlocking {
        for (network in listOf<suspend () -> Long>({ 0 }, { -1 }, { error("offline") })) {
            assertEquals(99L, resolveSpaceLikes(JSONObject(), JSONObject().put("likes", 99), network))
        }
    }
    @Test fun finalNavValueIsNotClampedAndMissingNavRemainsZero() = runBlocking {
        assertEquals(-9L, resolveSpaceLikes(JSONObject(), JSONObject().put("likes", -9)) { 0 })
        assertEquals(0L, resolveSpaceLikes(JSONObject(), null) { 0 })
    }
    @Test fun cancellationPropagatesInsteadOfFallingBackToNav() {
        assertThrows(CancellationException::class.java) {
            runBlocking { resolveSpaceLikes(JSONObject(), JSONObject().put("likes", 99)) { throw CancellationException("cancel") } }
        }
    }
}
