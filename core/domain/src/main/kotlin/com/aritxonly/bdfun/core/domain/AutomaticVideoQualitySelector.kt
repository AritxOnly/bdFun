package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.Quality

/** 平台读取失败或没有活动网络时返回0，单位沿用 Android 的 Kbps。 */
fun interface NetworkBandwidthSource { fun downstreamKbps(): Int }

/** 自动选档沿用常规 SDR 阶梯；没有阶梯候选时仍按旧规则选最小 id。 */
class AutomaticVideoQualitySelector(private val bandwidth: NetworkBandwidthSource) {
    fun select(qualities: List<Quality>): Quality? {
        if (qualities.isEmpty()) return null
        val mbps = bandwidth.downstreamKbps() / 1024.0
        val target = when {
            mbps >= 40.0 -> 127
            mbps >= 24.0 -> 120
            mbps >= 10.0 -> 112
            mbps >= 6.0 -> 80
            mbps >= 3.0 -> 64
            mbps >= 1.5 -> 32
            mbps > 0.0 -> 16
            else -> 64
        }
        return ladder.firstOrNull { id -> id <= target && qualities.any { it.id == id } }
            ?.let { id -> qualities.first { it.id == id } }
            ?: qualities.minByOrNull { it.id }
    }
    private val ladder = listOf(127, 120, 116, 112, 80, 74, 64, 32, 16)
}
