package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.domain.CdnSettingsSource
import com.aritxonly.bdfun.core.model.playback.CdnService
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class CdnSettingsUiState(val speeds: Map<String, Double>, val labels: List<String>)

/** 仍按原 400ms 观察进程测速；Map 未变时保持原 remember 的选项采样时机。 */
internal class CdnSettingsStateHolder(private val source: CdnSettingsSource) {
    val services = source.services
    private val initial = source.speeds()
    private val mutable = MutableStateFlow(CdnSettingsUiState(initial, cdnMenuLabels(services, initial, source.currentServiceId())))
    val state = mutable.asStateFlow()
    private var closed = false
    private var loading = false
    fun close() { closed = true }
    fun selectedIndex(id: String) = services.indexOfFirst { it.id == id }.let { if (it < 0) 0 else it }
    suspend fun load() {
        if (closed || loading) return
        loading = true
        try {
            checkActive()
            if (!source.isFresh() || mutable.value.speeds.isEmpty()) {
                source.refresh(force = mutable.value.speeds.isEmpty())
            }
            sample()
            while (source.isProbing()) { sample(); delay(400); checkActive() }
            sample()
        } finally { loading = false }
    }
    private suspend fun sample() {
        checkActive()
        val speeds = source.speeds()
        if (speeds != mutable.value.speeds) mutable.value = CdnSettingsUiState(speeds,
            cdnMenuLabels(services, speeds, source.currentServiceId()))
    }
    private suspend fun checkActive() {
        currentCoroutineContext().ensureActive()
        if (closed) throw kotlinx.coroutines.CancellationException("设置页已关闭")
    }
}

internal fun cdnMenuLabels(services: List<CdnService>, speeds: Map<String, Double>, currentId: String?): List<String> =
    services.map { service ->
        val speed = if (service.auto) currentId?.let(speeds::get) else speeds[service.id]
        if (speed != null && speed > 0.01) {
            "${service.label}  ${String.format(java.util.Locale.US, "%.1f", speed)} MB/s"
        } else service.label
    }
