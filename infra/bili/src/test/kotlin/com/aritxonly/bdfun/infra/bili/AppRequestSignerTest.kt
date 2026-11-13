package com.aritxonly.bdfun.infra.bili

import org.junit.Assert.*
import org.junit.Test

class AppRequestSignerTest {
    private val signer = AppRequestSigner({ it }, { 1700000000000L })

    @Test fun signatureUsesSortedParamsAndAppSecret() {
        val params = linkedMapOf("foo" to "value")
        signer.sign(params, "key", "secret", null)
        assertEquals("key", params["appkey"])
        assertEquals("1700000000", params["ts"])
        assertEquals("98712f8b2b535a62a679fb11110f5bff", params["sign"])
    }

    @Test fun optionalAccessKeyIsOnlyAddedWhenProvided() {
        val anonymous = linkedMapOf<String, String>()
        signer.sign(anonymous, "key", "secret", "")
        assertFalse(anonymous.containsKey("access_key"))
        val loggedIn = linkedMapOf<String, String>()
        signer.sign(loggedIn, "key", "secret", "token")
        assertEquals("token", loggedIn["access_key"])
        assertNotEquals(anonymous["sign"], loggedIn["sign"])
    }

    @Test fun callerSuppliedAccessKeyIsPreservedWhenAutoInsertIsOff() {
        val params = linkedMapOf("access_key" to "caller-token")
        signer.sign(params, "tv-key", "tv-secret", null)
        assertEquals("caller-token", params["access_key"])
        assertEquals("tv-key", params["appkey"])
    }
}
