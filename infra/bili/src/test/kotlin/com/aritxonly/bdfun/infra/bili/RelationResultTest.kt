package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.common.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class RelationResultTest {
    @Test fun preservesResultAndOriginalErrorIncludingEmptyMessage() = runBlocking {
        assertEquals(Outcome.Success(6), relationResult("fallback") { 6 })
        for (raw in listOf(null, "", "server")) {
            assertEquals(Outcome.Failure(AppError.Unknown(raw ?: "fallback")),
                relationResult<Int>("fallback") { throw IOException(raw) })
        }
    }
    @Test fun cancellationIsNeverAnErrorMessage() = runBlocking {
        try { relationResult<Unit>("fallback") { throw CancellationException() }; fail("cancel") }
        catch (_: CancellationException) { }
    }
}
