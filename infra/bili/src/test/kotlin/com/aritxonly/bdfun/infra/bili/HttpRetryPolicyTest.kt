package com.aritxonly.bdfun.infra.bili

import org.junit.Assert.*
import org.junit.Test

class HttpRetryPolicyTest {
    @Test fun onlyGetMayBeRepeated() {
        assertEquals(2, HttpRetryPolicy.attempts("GET"))
        for (method in listOf("POST", "PUT", "PATCH", "DELETE")) {
            assertEquals(1, HttpRetryPolicy.attempts(method))
        }
    }

    @Test fun permanentErrorsAreNotRetried() {
        for (code in listOf(200, 204, 400, 401, 403, 404)) {
            assertFalse("HTTP $code", HttpRetryPolicy.retryableStatus(code))
        }
        for (code in listOf(408, 429, 500, 502, 503, 599)) {
            assertTrue("HTTP $code", HttpRetryPolicy.retryableStatus(code))
        }
    }

    @Test fun backoffRetainsJitterBounds() {
        repeat(100) {
            assertTrue(HttpRetryPolicy.delayMs(0) in 350L..530L)
            assertTrue(HttpRetryPolicy.delayMs(1) in 700L..880L)
        }
    }
}
