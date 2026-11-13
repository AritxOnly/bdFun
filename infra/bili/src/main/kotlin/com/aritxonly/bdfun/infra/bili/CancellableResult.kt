package com.aritxonly.bdfun.infra.bili

import kotlinx.coroutines.CancellationException

/** 兼容旧 Result 转发，同时不吞掉离页、登出或刷新触发的协程取消。 */
internal inline fun <T> cancellableCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancel: CancellationException) {
    throw cancel
} catch (failure: Throwable) {
    Result.failure(failure)
}
