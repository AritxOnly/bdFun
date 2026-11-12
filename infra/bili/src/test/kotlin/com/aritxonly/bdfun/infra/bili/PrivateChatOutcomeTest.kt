package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PrivateChatOutcomeTest {
    @Test fun rawBlankAndMissingMessagesKeepTheirOriginalFallbacks() = runBlocking {
        for ((message, expected) in listOf("原文" to "原文", "" to "", null to "历史消息加载失败")) {
            assertEquals(Outcome.Failure(AppError.Unknown(expected)), privateChatOutcome<Int>("历史消息加载失败") {
                throw Exception(message)
            })
        }
    }
    @Test fun successKeepsTheOriginalValue() = runBlocking {
        assertEquals(Outcome.Success(false), privateChatOutcome("发送失败") { false })
    }
    @Test fun cancellationEscapesWithoutAChatError() = runBlocking {
        try {
            privateChatOutcome<Int>("发送失败") { throw CancellationException("close") }
            fail("取消不能变成错误文案")
        } catch (cancel: CancellationException) { assertEquals("close", cancel.message) }
    }
}
