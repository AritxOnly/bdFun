package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.VideoCacheTransfer
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class BiliVideoCacheTransfer(private val client: OkHttpClient = defaultCacheTransferClient()) : VideoCacheTransfer {
    override suspend fun download(
        url: String,
        path: String,
        onBytes: suspend (Long) -> Unit,
    ): Outcome<Long> = withContext(Dispatchers.IO) {
        val target = File(path)
        transferResult {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", BiliBackend.UA)
                .header("Referer", "https://www.bilibili.com/")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }
                val body = response.body ?: throw IOException("空响应")
                var total = 0L
                target.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(256 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            total += read
                            onBytes(total)
                        }
                    }
                }
                total
            }
        }
    }

    override suspend fun contentLength(url: String): Long = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", BiliBackend.UA)
                .header("Referer", "https://www.bilibili.com/")
                .head()
                .build()
            client.newCall(request).execute().use { response ->
                response.header("Content-Length")?.toLongOrNull() ?: -1L
            }
        }.getOrDefault(-1L)
    }

    override suspend fun cover(url: String, path: String): String {
        if (url.isBlank()) return ""
        val target = File(path)
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", BiliBackend.UA)
                .header("Referer", "https://www.bilibili.com/")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return ""
                val body = response.body ?: return ""
                target.outputStream().use { output ->
                    body.byteStream().use { input -> input.copyTo(output) }
                }
            }
            if (target.length() > 0) target.absolutePath else ""
        }.getOrDefault("")
    }

}

internal suspend fun <T> transferResult(block: suspend () -> T): Outcome<T> = try {
    Outcome.Success(block())
} catch (cancel: CancellationException) {
    throw cancel
} catch (failure: Exception) {
    Outcome.Failure(AppError.Network(failure.message ?: "下载失败"))
}

internal fun defaultCacheTransferClient(): OkHttpClient = HttpClients.base.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
