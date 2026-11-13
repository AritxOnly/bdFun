package com.aritxonly.bdfun.infra.bili

import java.util.concurrent.TimeUnit
import okhttp3.Dispatcher
import okhttp3.OkHttpClient

/**
 * 全 App 共用一个连接池和线程池。
 *
 * 各个业务需要不同超时时用 `HttpClients.base.newBuilder()` 派生，
 * OkHttp 的 dispatcher / connectionPool 会沿用同一份，避免每个模块各建一套线程。
 */
object HttpClients {
    val base: OkHttpClient = OkHttpClient.Builder()
        .dispatcher(
            Dispatcher().apply {
                maxRequests = 32
                maxRequestsPerHost = 8
            },
        )
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .callTimeout(18, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}
