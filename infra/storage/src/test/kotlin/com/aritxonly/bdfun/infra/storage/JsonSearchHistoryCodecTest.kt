package com.aritxonly.bdfun.infra.storage

import org.junit.Assert.*
import org.junit.Test

class JsonSearchHistoryCodecTest {
    private val codec = JsonSearchHistoryCodec()
    @Test fun encodedValuesRetainQuotesUnicodeAndOrder() {
        val values = listOf("中文", "quotes\"", "line\n", "emoji😀")
        assertEquals(values, codec.decode(codec.encode(values)))
    }
    @Test fun malformedHistoryKeepsLegacyEmptyFallback() {
        assertTrue(codec.decode("invalid").isEmpty())
        assertEquals(listOf("valid"), codec.decode("[\"\",\"valid\",\"  \"]"))
    }
}
