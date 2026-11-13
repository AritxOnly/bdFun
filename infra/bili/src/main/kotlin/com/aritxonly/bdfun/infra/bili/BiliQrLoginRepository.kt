package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.QrLoginRepository
import com.aritxonly.bdfun.core.domain.QrLoginResult
import com.aritxonly.bdfun.core.model.user.QrLogin
import com.aritxonly.bdfun.core.model.user.QrPoll
import kotlinx.coroutines.CancellationException

class BiliQrLoginRepository(private val create: suspend () -> QrLogin, private val query: suspend (String) -> QrPoll) : QrLoginRepository {
    override suspend fun generate() = qrResult { create() }
    override suspend fun poll(key: String) = qrResult { query(key) }
}
private suspend fun <T> qrResult(block: suspend () -> T): QrLoginResult<T> = try {
    QrLoginResult.Success(block())
} catch (cancel: CancellationException) {
    throw cancel
} catch (failure: Exception) {
    QrLoginResult.Failure(failure.message)
}
