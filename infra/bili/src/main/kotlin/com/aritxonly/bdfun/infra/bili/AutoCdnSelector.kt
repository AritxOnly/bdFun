package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.AutoCdnCache
import com.aritxonly.bdfun.core.domain.CdnSettingsSource
import com.aritxonly.bdfun.core.model.settings.AutoCdnSnapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import java.util.Locale

/** 进程作用域测速；两轮取慢值、四路并行、旧缓存回退均沿用原实现。 */
class AutoCdnSelector(private val sampleUrl: suspend () -> String?, private val cache: AutoCdnCache?,
    private val log: (String, String) -> Unit,
    private val probe: (CdnService, String) -> Double? = CdnSpeedProbe()::measure,
    private val now: () -> Long = System::currentTimeMillis,
    override val services: List<CdnService> = CdnServices) : CdnSettingsSource {
    private val refreshMutex = Mutex()
    private val initial = cache?.read() ?: AutoCdnSnapshot()
    @Volatile private var bestId = initial.bestId
    @Volatile private var bestSpeed = initial.bestSpeed
    @Volatile private var measuredAt = initial.measuredAt
    @Volatile private var probing = false
    @Volatile private var lastFailure: String? = null
    @Volatile private var measuredSpeeds = initial.speeds
    override fun speeds() = measuredSpeeds
    override fun currentServiceId() = currentService()?.id
    fun currentService(): CdnService? = services.firstOrNull { it.id == bestId }?.takeIf { isAutoCandidate(it) }
    override fun isFresh() = bestId != null && now() - measuredAt < 60 * 60 * 1000L
    override fun isProbing() = probing
    fun describe(): String {
        val service = currentService()
        return when {
            probing -> "正在测速，自动匹配最快且最稳定的节点…"
            service != null -> {
                val speed = if (bestSpeed > 0.01) String.format(Locale.US, "%.1f MB/s", bestSpeed) else "已缓存"
                "${service.label} · $speed"
            }
            lastFailure != null -> "自动测速失败，暂用备用地址"
            else -> "自动测速尚未完成，暂用备用地址"
        }
    }
    override suspend fun refresh(force: Boolean): Outcome<Boolean> = Outcome.Success(refreshSelection(force))
    suspend fun refreshSelection(force: Boolean = false): Boolean {
        if (!force && isFresh()) return true
        return refreshMutex.withLock {
            if (!force && isFresh()) return@withLock true
            probing = true
            try {
                val sample = sampleUrl()
                if (sample.isNullOrBlank()) {
                    lastFailure = "no-sample"
                    return@withLock false
                }
                val candidates = services.filter { isAutoCandidate(it) }
                val results = coroutineScope {
                    val limiter = Semaphore(4)
                    candidates.map { service -> async(Dispatchers.IO) {
                        limiter.withPermit { probe(service, sample)?.let { service to it } }
                    } }.awaitAll()
                }.filterNotNull()
                measuredSpeeds = results.associate { it.first.id to it.second }
                val best = results.maxByOrNull { it.second }
                if (best == null) {
                    lastFailure = "no-result"
                    return@withLock false
                }
                bestId = best.first.id
                bestSpeed = best.second
                measuredAt = now()
                log("CDN", "auto best=${best.first.id} speed=${best.second}")
                lastFailure = null
                cache?.write(AutoCdnSnapshot(bestId, bestSpeed, measuredAt, measuredSpeeds))
                true
            } finally { probing = false }
        }
    }
    private fun isAutoCandidate(service: CdnService) = !service.useBackup && !service.auto && !service.host.isNullOrBlank()
}
