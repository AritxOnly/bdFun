package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.QrLoginResult
import com.aritxonly.bdfun.core.model.user.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class BiliQrLoginRepositoryTest {
    @Test fun passesGeneratedQrAndPollingKeyUnchanged() = runBlocking {
        val qr = QrLogin("url", "key"); val poll = QrPoll(QrState.Scanned, "raw")
        val repo = BiliQrLoginRepository({ qr }, { key -> assertEquals("key", key); poll })
        assertEquals(QrLoginResult.Success(qr), repo.generate())
        assertEquals(QrLoginResult.Success(poll), repo.poll("key"))
    }
    @Test fun preservesNullEmptyAndOriginalFailureMessages() = runBlocking {
        for (message in listOf(null, "", "raw server message")) {
            val repo = BiliQrLoginRepository({ throw IOException(message) }, { throw IOException(message) })
            assertEquals(QrLoginResult.Failure(message), repo.generate())
            assertEquals(QrLoginResult.Failure(message), repo.poll("key"))
        }
    }
    @Test fun cancellationPropagatesFromBothRequests() = runBlocking {
        val repo = BiliQrLoginRepository({ throw CancellationException() }, { throw CancellationException() })
        try { repo.generate(); fail("cancelled generate") } catch (_: CancellationException) { }
        try { repo.poll("key"); fail("cancelled poll") } catch (_: CancellationException) { }
    }
}
