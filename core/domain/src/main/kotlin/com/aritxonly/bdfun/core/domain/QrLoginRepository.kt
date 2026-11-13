package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.user.QrLogin
import com.aritxonly.bdfun.core.model.user.QrPoll

/** 登录失败文案保留服务端原消息，包括空串与缺失消息的差别。 */
sealed interface QrLoginResult<out T> {
    data class Success<T>(val value: T) : QrLoginResult<T>
    data class Failure(val message: String?) : QrLoginResult<Nothing>
}
interface QrLoginRepository {
    suspend fun generate(): QrLoginResult<QrLogin>
    suspend fun poll(key: String): QrLoginResult<QrPoll>
}
