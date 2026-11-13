package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.infra.bili.isRetryableHttpStatus
import com.aritxonly.bdfun.infra.bili.httpRetryDelayMs
import com.aritxonly.bdfun.infra.bili.httpRequestAttempts
import org.junit.Assert.*
import org.junit.Test

class HttpRetryPolicyTest {
    @Test fun onlyGetMayBeRepeated() {
        assertEquals(2, httpRequestAttempts("GET"))
        for (method in listOf("POST", "PUT", "PATCH", "DELETE")) {
            assertEquals(1, httpRequestAttempts(method))
        }
    }

    @Test fun permanentErrorsAreNotRetried() {
        for (code in listOf(200, 204, 400, 401, 403, 404)) {
            assertFalse("HTTP $code", isRetryableHttpStatus(code))
        }
        for (code in listOf(408, 429, 500, 502, 503, 599)) {
            assertTrue("HTTP $code", isRetryableHttpStatus(code))
        }
    }

    @Test fun backoffRetainsJitterBounds() {
        repeat(100) {
            assertTrue(httpRetryDelayMs(0) in 350L..530L)
            assertTrue(httpRetryDelayMs(1) in 700L..880L)
        }
    }
}
