package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SearchOutcomeTest {
    @Test fun successRetainsValue() = runBlocking {
        assertEquals(Outcome.Success(listOf("原词")), searchOutcome { listOf("原词") })
    }
    @Test fun rawMessageIncludingBlankAndMissingFallbackIsPreserved() = runBlocking {
        for ((message, expected) in listOf("服务器原文" to "服务器原文", "" to "", null to "搜索失败")) {
            assertEquals(Outcome.Failure(AppError.Unknown(expected)), searchOutcome<Int> { throw Exception(message) })
        }
    }
    @Test fun cancellationEscapes() = runBlocking {
        try {
            searchOutcome<Int> { throw CancellationException("离页") }
            fail("不能吞掉取消")
        } catch (cancel: CancellationException) { assertEquals("离页", cancel.message) }
    }
}
