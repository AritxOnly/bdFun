package com.aritxonly.bdfun.feature.login.application

import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.user.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.*
import org.junit.Test

class QrLoginStateHolderTest {
    private class Fake : QrLoginRepository {
        var generated: QrLoginResult<QrLogin> = QrLoginResult.Success(QrLogin("url", "key"))
        var gate: CompletableDeferred<Unit>? = null
        var pollGate: CompletableDeferred<Unit>? = null
        var calls = 0
        val keys = mutableListOf<String>()
        val polls = ArrayDeque<QrLoginResult<QrPoll>>()
        override suspend fun generate(): QrLoginResult<QrLogin> { calls++; gate?.await(); return generated }
        override suspend fun poll(key: String): QrLoginResult<QrPoll> { keys += key; pollGate?.await(); return polls.removeFirst() }
    }
    @Test fun officialAppLaunchUsesOnlyReadyCurrentQrAndDoesNotReplacePollingKey() = runBlocking {
        val first = "https://passport.bilibili.com/scan?auth_code=first"
        val second = "https://passport.bilibili.com/scan?auth_code=second"
        val fake = Fake().apply { generated = QrLoginResult.Success(QrLogin(first, "first")) }
        val holder = QrLoginStateHolder(fake) {}
        assertNull(holder.officialAppLoginLink())
        holder.generate()
        assertEquals(officialAppLoginUri(first), holder.officialAppLoginLink())
        assertEquals("first", holder.state.value.qr?.key)
        fake.gate = CompletableDeferred()
        val refresh = launch(start = CoroutineStart.UNDISPATCHED) { holder.generate() }
        assertNull(holder.officialAppLoginLink())
        fake.generated = QrLoginResult.Success(QrLogin(second, "second"))
        fake.gate!!.complete(Unit); refresh.join()
        assertEquals(officialAppLoginUri(second), holder.officialAppLoginLink())
        fake.polls += QrLoginResult.Success(QrPoll(QrState.Success, "ok"))
        assertEquals(QrLoginCompletion(null), holder.poll("second"))
        assertEquals(listOf("second"), fake.keys)
    }
    @Test fun officialAppLaunchIsDisabledAfterExpiryFailureOrClose() = runBlocking {
        val fake = Fake().apply { generated = QrLoginResult.Success(QrLogin("https://passport.bilibili.com/scan", "key")) }
        val holder = QrLoginStateHolder(fake) {}; holder.generate()
        fake.polls += QrLoginResult.Success(QrPoll(QrState.Expired, "expired"))
        holder.poll("key"); assertNull(holder.officialAppLoginLink())
        holder.generate(); assertNotNull(holder.officialAppLoginLink())
        fake.generated = QrLoginResult.Failure("offline"); holder.generate()
        assertNull(holder.officialAppLoginLink())
        fake.generated = QrLoginResult.Success(QrLogin("https://passport.bilibili.com/scan", "new"))
        holder.generate(); holder.close(); assertNull(holder.officialAppLoginLink())
    }
    @Test fun initialLoadingAndRefreshPreserveOldPollingKeyButHideOldImage() = runBlocking {
        val fake = Fake(); val holder = QrLoginStateHolder(fake)
        assertEquals(QrLoginState(), holder.state.value); holder.generate()
        assertEquals("请用哔哩哔哩手机客户端扫码", holder.state.value.status)
        fake.gate = CompletableDeferred()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.generate() }
        assertNull(holder.state.value.displayQr); assertEquals("key", holder.state.value.qr?.key)
        assertTrue(holder.state.value.loading); assertFalse(holder.state.value.expired)
        fake.generated = QrLoginResult.Failure(null); fake.gate!!.complete(Unit); job.join()
        assertEquals("二维码获取失败：网络异常", holder.state.value.status)
        assertTrue(holder.state.value.expired); assertFalse(holder.state.value.loading)
        assertNull(holder.state.value.displayQr); assertEquals("key", holder.state.value.qr?.key)
    }
    @Test fun emptyErrorMessageIsNotReplacedByFallback() = runBlocking {
        val fake = Fake().apply { generated = QrLoginResult.Failure("") }
        val holder = QrLoginStateHolder(fake); holder.generate()
        assertEquals("二维码获取失败：", holder.state.value.status)
    }
    @Test fun waitsBeforeEveryPollAndNetworkFailuresKeepPolling() = runBlocking {
        val fake = Fake().apply {
            polls += QrLoginResult.Failure("offline")
            polls += QrLoginResult.Success(QrPoll(QrState.Scanned, "ignored"))
            polls += QrLoginResult.Success(QrPoll(QrState.Waiting, "ignored"))
            polls += QrLoginResult.Success(QrPoll(QrState.Success, "ok", null))
        }
        val ticks = Channel<Unit>(); var waits = 0
        val holder = QrLoginStateHolder(fake) { waits++; ticks.receive() }; holder.generate()
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.poll("key") }
        assertTrue(fake.keys.isEmpty()); assertEquals(1, waits)
        ticks.send(Unit); yield(); assertEquals("请用哔哩哔哩手机客户端扫码", holder.state.value.status)
        ticks.send(Unit); yield(); assertEquals("已扫码，请在手机上确认登录", holder.state.value.status)
        ticks.send(Unit); yield(); assertEquals("已扫码，请在手机上确认登录", holder.state.value.status)
        ticks.send(Unit); assertEquals(QrLoginCompletion(null), job.await())
        assertEquals(List(4) { "key" }, fake.keys); assertEquals(4, waits)
    }
    @Test fun expiredPollUsesOriginalFixedMessageAndStops() = runBlocking {
        val fake = Fake().apply { polls += QrLoginResult.Success(QrPoll(QrState.Expired, "server")) }
        val holder = QrLoginStateHolder(fake) {}; holder.generate()
        assertNull(holder.poll("key")); assertEquals("二维码已过期，请刷新", holder.state.value.status)
        assertTrue(holder.state.value.expired); assertEquals(1, fake.keys.size)
    }
    @Test fun errorPollUsesServerMessageOrBlankFallback() = runBlocking {
        for ((raw, expected) in listOf("error" to "error", "  " to "登录失败")) {
            val fake = Fake().apply { polls += QrLoginResult.Success(QrPoll(QrState.Error, raw)) }
            val holder = QrLoginStateHolder(fake) {}; holder.generate(); holder.poll("key")
            assertEquals(expected, holder.state.value.status); assertTrue(holder.state.value.expired)
        }
    }
    @Test fun closedPageRejectsLateGenerateAndDoesNotRestart() = runBlocking {
        val fake = Fake().apply { gate = CompletableDeferred() }; val holder = QrLoginStateHolder(fake)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.generate() }
        holder.close(); fake.gate!!.complete(Unit); job.join(); holder.generate()
        assertNull(holder.state.value.qr); assertEquals(1, fake.calls)
    }
    @Test fun replacementQrRejectsOldSuccessResponse() = runBlocking {
        val fake = Fake().apply { polls += QrLoginResult.Success(QrPoll(QrState.Success, "ok")); pollGate = CompletableDeferred() }
        val holder = QrLoginStateHolder(fake) {}; holder.generate()
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.poll("key") }
        fake.generated = QrLoginResult.Success(QrLogin("new-url", "new-key")); holder.generate()
        fake.pollGate!!.complete(Unit)
        assertNull(job.await()); assertEquals("new-key", holder.state.value.qr?.key)
    }
    @Test fun cancelledGenerateUnlocksRetryWithoutErrorState() = runBlocking {
        val fake = Fake().apply { gate = CompletableDeferred() }; val holder = QrLoginStateHolder(fake)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.generate() }; job.cancelAndJoin()
        assertFalse(holder.state.value.loading); assertFalse(holder.state.value.expired)
        fake.gate = null; holder.generate(); assertEquals("key", holder.state.value.qr?.key)
    }
    @Test fun closeDuringPollDoesNotDeliverSuccess() = runBlocking {
        val fake = Fake().apply { polls += QrLoginResult.Success(QrPoll(QrState.Success, "ok")); pollGate = CompletableDeferred() }
        val holder = QrLoginStateHolder(fake) {}; holder.generate()
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.poll("key") }
        holder.close(); fake.pollGate!!.complete(Unit); assertNull(job.await())
    }
}
