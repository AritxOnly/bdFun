package com.aritxonly.bdfun.infra.bili

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.min

internal class CdnSpeedProbe {
    private val ROUND_BYTES = 1024L * 1024L
    private val MAX_ROUND_MS = 6_000L
    private val client = HttpClients.base.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    fun measure(service: CdnService, sampleUrl: String): Double? {
        val url = service.rewrite(sampleUrl)
        val first = downloadMbps(url) ?: return null
        val second = downloadMbps(url) ?: return null
        val slowest = min(first, second)
        if (slowest <= 0.01) return null
        return slowest
    }

    private fun downloadMbps(url: String): Double? {
        val request = runCatching {
            Request.Builder()
                .url(url)
                .header("User-Agent", BiliBackend.UA)
                .header("Referer", "https://www.bilibili.com/")
                .header("Range", "bytes=0-${ROUND_BYTES - 1}")
                .get()
                .build()
        }.getOrNull() ?: return null
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val input = response.body?.byteStream() ?: return@use null
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                val start = System.nanoTime()
                while (total < ROUND_BYTES) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    total += read
                    if ((System.nanoTime() - start) / 1_000_000 >= MAX_ROUND_MS) break
                }
                val seconds = ((System.nanoTime() - start) / 1_000_000_000.0)
                    .coerceAtLeast(0.001)
                if (total <= 0) null else total / 1024.0 / 1024.0 / seconds
            }
        }.getOrNull()
    }
}
