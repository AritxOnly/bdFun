package com.aritxonly.bdfun.infra.bili

/** 保留旧网络层策略；写请求不可因自动重试重复投币等操作。 */
internal object HttpRetryPolicy {
    fun attempts(method: String): Int = if (method == "GET") 2 else 1
    fun retryableStatus(code: Int): Boolean = code == 408 || code == 429 || code >= 500
    fun delayMs(attempt: Int): Long = 350L * (1L shl attempt) + (0..180).random().toLong()
}
