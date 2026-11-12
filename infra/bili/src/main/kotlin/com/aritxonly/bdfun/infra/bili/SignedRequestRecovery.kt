package com.aritxonly.bdfun.infra.bili

import kotlinx.coroutines.CancellationException

/** 拒绝不必然意味着密钥过期；仅对签名 GET 做一次恢复尝试，不重复写操作或风控请求。 */
internal suspend fun <T> recoverSignedRead(
    signed: Boolean,
    method: String,
    request: suspend () -> T,
    denied: (T) -> Boolean,
    refresh: suspend () -> Unit,
): T {
    val first = cancellableCatching { request() }
    val rejected = first.fold(denied) { it is BiliException && it.code == 403 }
    if (!signed || method != "GET" || !rejected) return first.getOrThrow()
    try {
        refresh()
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (failure: Exception) {
        return first.getOrThrow()
    }
    return request()
}
