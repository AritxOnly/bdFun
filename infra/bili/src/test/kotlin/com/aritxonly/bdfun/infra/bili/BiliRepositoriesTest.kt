package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.common.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class BiliRepositoriesTest {
    @Test fun anonymousProfileIsSuccessfulNull() = runBlocking {
        val repo = BiliAccountRepository({ null }, { error("unused") }, { error("unused") }, {})
        assertEquals(Outcome.Success<Any?>(null), repo.profile())
    }
    @Test fun logoutDelegatesExactlyOnce() {
        var calls = 0
        val repo = BiliAccountRepository({ null }, { error("unused") }, { error("unused") }, { calls++ })
        repo.logout()
        assertEquals(1, calls)
    }
    @Test fun loginAndRiskErrorsKeepTheirDomainMeaning() = runBlocking {
        assertEquals(Outcome.Failure(AppError.LoginExpired), biliOutcome<Int> { throw BiliException(-101, "login") })
        assertEquals(Outcome.Failure(AppError.RiskControl("risk")), biliOutcome<Int> { throw BiliException(-412, "risk") })
    }
    @Test fun generatedParsingAndTransportFailuresAreSeparated() = runBlocking {
        assertEquals(Outcome.Failure(AppError.Parsing("数据解析失败")), biliOutcome<Int> { throw BiliException(-1, "数据解析失败") })
        assertEquals(Outcome.Failure(AppError.Network("offline")), biliOutcome<Int> { throw IOException("offline") })
        assertEquals(Outcome.Failure(AppError.Unknown("gone")), biliOutcome<Int> { throw BiliException(62002, "gone") })
    }
    @Test fun cancellationIsNeverReportedAsNetworkFailure() = runBlocking {
        try {
            biliOutcome<Int> { throw CancellationException("cancel") }
            fail("取消必须继续传播")
        } catch (e: CancellationException) {
            assertEquals("cancel", e.message)
        }
    }
    @Test fun recommendationPageReachesInjectedSource() = runBlocking {
        var page = 0
        val repo = BiliFeedRepository({ page = it; emptyList() }, { error("unused") })
        assertEquals(Outcome.Success(emptyList<Any>()), repo.recommend(7))
        assertEquals(7, page)
    }
}
