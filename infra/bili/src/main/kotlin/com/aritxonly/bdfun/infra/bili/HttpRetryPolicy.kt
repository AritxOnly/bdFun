package com.aritxonly.bdfun.infra.bili

/** 保留旧网络层策略；写请求不可因自动重试重复投币等操作。 */
internal fun httpRequestAttempts(method: String): Int = if (method == "GET") 2 else 1
internal fun isRetryableHttpStatus(code: Int): Boolean = code == 408 || code == 429 || code >= 500
internal fun httpRetryDelayMs(attempt: Int): Long = 350L * (1L shl attempt) + (0..180).random().toLong()
