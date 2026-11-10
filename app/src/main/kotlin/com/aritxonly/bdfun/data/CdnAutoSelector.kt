package com.aritxonly.bdfun.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * 「自动」CDN：并行测速后选兼顾速度和稳定性的节点。
 *
 * 每个节点跑两轮，取两轮里较慢的一轮作为成绩。只看峰值会把偶尔冲高、
 * 下一轮就掉下去的节点排到前面，取最慢一轮相当于把抖动直接扣掉。
 * 结果写入 SharedPreferences，冷启动先沿用上次结果，再在后台重新测。
 */
object CdnAutoSelector {
    const val ID = "auto"
    const val LABEL = "自动"

    private const val PREFS = "cdn_auto"
    private const val KEY_BEST_ID = "best_id"
    private const val KEY_BEST_SPEED = "best_speed"
    private const val KEY_MEASURED_AT = "measured_at"
    private const val KEY_SPEEDS = "speeds"
    private const val CACHE_TTL_MS = 60 * 60 * 1000L

    private const val ROUND_BYTES = 1024L * 1024L
    private const val MAX_ROUND_MS = 6_000L

    private val client = com.aritxonly.bdfun.data.HttpClients.base.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    private val refreshMutex = Mutex()

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var bestId: String? = null

    @Volatile
    private var bestSpeed: Double = 0.0

    @Volatile
    private var measuredAt: Long = 0L

    @Volatile
    private var probing: Boolean = false

    @Volatile
    private var lastFailure: String? = null

    /** 最近一次测速里每个节点的成绩（MB/s），设置页直接显示在选项后面。 */
    @Volatile
    private var speeds: Map<String, Double> = emptyMap()

    fun speedMbps(id: String): Double? = speeds[id]

    fun speedsSnapshot(): Map<String, Double> = speeds

    fun init(context: Context) {
        if (prefs != null) return
        val store = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = store
        bestId = store.getString(KEY_BEST_ID, null)
        bestSpeed = store.getFloat(KEY_BEST_SPEED, 0f).toDouble()
        measuredAt = store.getLong(KEY_MEASURED_AT, 0L)
        speeds = parseSpeeds(store.getString(KEY_SPEEDS, "").orEmpty())
    }

    fun currentService(): CdnService? {
        val id = bestId ?: return null
        val service = CdnServices.firstOrNull { it.id == id } ?: return null
        return service.takeIf { isAutoCandidate(it) }
    }

    fun isFresh(): Boolean = bestId != null && System.currentTimeMillis() - measuredAt < CACHE_TTL_MS

    fun isProbing(): Boolean = probing

    /** 给设置页显示的一行状态。 */
    fun describe(): String {
        val service = currentService()
        return when {
            probing -> "正在测速，自动匹配最快且最稳定的节点…"
            service != null -> {
                val speed = if (bestSpeed > 0.01) {
                    String.format(Locale.US, "%.1f MB/s", bestSpeed)
                } else {
                    "已缓存"
                }
                "${service.label} · $speed"
            }
            lastFailure != null -> "自动测速失败，暂用备用地址"
            else -> "自动测速尚未完成，暂用备用地址"
        }
    }

    /** 测速并写入选择；force 时忽略缓存（用户刚手动选中「自动」）。 */
    suspend fun refresh(api: BiliApi, force: Boolean = false): Boolean {
        if (!force && isFresh()) return true
        return refreshMutex.withLock {
            if (!force && isFresh()) return@withLock true
            probing = true
            try {
                val sample = api.cdnSampleUrl()
                if (sample.isNullOrBlank()) {
                    lastFailure = "no-sample"
                    return@withLock false
                }
                val candidates = CdnServices.filter { isAutoCandidate(it) }
                val results = coroutineScope {
                    val limiter = Semaphore(4)
                    candidates.map { service ->
                        async(Dispatchers.IO) {
                            limiter.withPermit { probe(service, sample) }
                        }
                    }.awaitAll()
                }.filterNotNull()
                speeds = results.associate { it.service.id to it.slowestMbps }
                val best = results.maxByOrNull { it.score }
                if (best == null) {
                    lastFailure = "no-result"
                    return@withLock false
                }
                bestId = best.service.id
                bestSpeed = best.slowestMbps
                measuredAt = System.currentTimeMillis()
                LogCollector.record(
                    "CDN",
                    "auto best=${best.service.id} speed=${best.slowestMbps}",
                )
                lastFailure = null
                prefs?.edit()
                    ?.putString(KEY_BEST_ID, best.service.id)
                    ?.putFloat(KEY_BEST_SPEED, best.slowestMbps.toFloat())
                    ?.putLong(KEY_MEASURED_AT, measuredAt)
                    ?.putString(KEY_SPEEDS, encodeSpeeds(speeds))
                    ?.apply()
                true
            } finally {
                probing = false
            }
        }
    }

    private fun isAutoCandidate(service: CdnService): Boolean =
        !service.useBackup && !service.auto && !service.host.isNullOrBlank()

    private fun encodeSpeeds(values: Map<String, Double>): String =
        values.entries.joinToString(",") { "${it.key}:${it.value}" }

    private fun parseSpeeds(raw: String): Map<String, Double> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(",").mapNotNull { part ->
            val index = part.lastIndexOf(':')
            if (index <= 0) return@mapNotNull null
            val speed = part.substring(index + 1).toDoubleOrNull() ?: return@mapNotNull null
            part.substring(0, index) to speed
        }.toMap()
    }

    private data class Probe(
        val service: CdnService,
        val slowestMbps: Double,
    ) {
        val score: Double get() = slowestMbps
    }

    private fun probe(service: CdnService, sampleUrl: String): Probe? {
        val url = service.rewrite(sampleUrl)
        val first = downloadMbps(url) ?: return null
        val second = downloadMbps(url) ?: return null
        val slowest = min(first, second)
        if (slowest <= 0.01) return null
        return Probe(service, slowest)
    }

    private fun downloadMbps(url: String): Double? {
        val request = runCatching {
            Request.Builder()
                .url(url)
                .header("User-Agent", BiliApi.UA)
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
