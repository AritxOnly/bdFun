package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.common.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class BiliVideoCacheTransferTest {
    @get:Rule val temp = TemporaryFolder()
    private fun client(response: (Request) -> Response) = OkHttpClient.Builder().addInterceptor { chain -> response(chain.request()) }.build()
    private fun response(request: Request, code: Int = 200, bytes: ByteArray = byteArrayOf(), length: String? = null): Response =
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("test")
            .body(bytes.toResponseBody()).apply { if (length != null) header("Content-Length", length) }.build()
    @Test fun downloadWritesExactBytesWithOriginalHeadersAnd256KiBChunks() = runBlocking {
        val bytes = ByteArray(600_000) { (it % 251).toByte() }
        val transport = BiliVideoCacheTransfer(client { request ->
            assertEquals("GET", request.method); assertEquals(BiliBackend.UA, request.header("User-Agent"))
            assertEquals("https://www.bilibili.com/", request.header("Referer")); response(request, bytes = bytes)
        })
        val target = temp.newFile("video.part"); val samples = mutableListOf<Long>()
        assertEquals(Outcome.Success(600_000L), transport.download("https://example.test/video", target.path) { samples += it })
        assertArrayEquals(bytes, target.readBytes()); assertEquals(600_000L, samples.last())
        // InputStream 可短读；只约束累计递增、无丢字节及单次不超缓冲上限。
        assertTrue((listOf(0L) + samples).zipWithNext().all { (before, after) -> after > before && after - before <= 256 * 1024 })
    }
    @Test fun httpAndNetworkFailuresUseExplicitOutcomeWithoutCreatingFile() = runBlocking {
        val target = java.io.File(temp.root, "missing.part")
        val http = BiliVideoCacheTransfer(client { response(it, 403) })
        assertEquals(Outcome.Failure(AppError.Network("HTTP 403")), http.download("https://example.test/video", target.path) {})
        assertFalse(target.exists())
        val offline = BiliVideoCacheTransfer(client { throw IOException("offline") })
        assertEquals(Outcome.Failure(AppError.Network("offline")), offline.download("https://example.test/video", target.path) {})
    }
    @Test fun headLengthKeepsLegacyMissingMalformedAndErrorStatusBehavior() = runBlocking {
        for ((header, status, expected) in listOf(Triple("123", 200, 123L), Triple(null, 200, -1L), Triple("bad", 200, -1L), Triple("99", 500, 99L))) {
            val transport = BiliVideoCacheTransfer(client { request ->
                assertEquals("HEAD", request.method); response(request, code = status, length = header)
            })
            assertEquals(expected, transport.contentLength("https://example.test/video"))
        }
        val offline = BiliVideoCacheTransfer(client { throw IOException("offline") })
        assertEquals(-1L, offline.contentLength("https://example.test/video"))
    }
    @Test fun coverIsBestEffortAndReturnsPathOnlyForNonEmptySuccess() = runBlocking {
        val target = java.io.File(temp.root, "cover.jpg")
        val ok = BiliVideoCacheTransfer(client { response(it, bytes = byteArrayOf(1, 2)) })
        assertEquals(target.absolutePath, ok.cover("https://example.test/cover", target.path)); assertArrayEquals(byteArrayOf(1, 2), target.readBytes())
        assertEquals("", ok.cover("", target.path))
        assertEquals("", BiliVideoCacheTransfer(client { response(it, 500) }).cover("https://example.test/cover", target.path))
        assertEquals("", BiliVideoCacheTransfer(client { response(it) }).cover("https://example.test/cover", target.path))
    }
    @Test fun cancellationFromByteCallbackPropagatesInsteadOfCachingFailure() = runBlocking {
        val transport = BiliVideoCacheTransfer(client { response(it, bytes = ByteArray(100)) })
        try {
            transport.download("https://example.test/video", temp.newFile().path) { throw CancellationException() }
            fail("cancelled")
        } catch (_: CancellationException) { }
    }
}
