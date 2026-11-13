package com.aritxonly.bdfun.infra.bili

import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class CancellableResultTest {
    @Test fun ordinaryFailureRetainsTheSameException() {
        val failure = IOException("offline")
        assertSame(failure, cancellableCatching<Int> { throw failure }.exceptionOrNull())
    }
    @Test fun cancellationEscapesInsteadOfTurningIntoFalseOrNull() {
        try {
            cancellableCatching<Int> { throw CancellationException("leave") }.getOrDefault(0)
            fail("取消不能变成默认值")
        } catch (cancel: CancellationException) { assertEquals("leave", cancel.message) }
    }
}
