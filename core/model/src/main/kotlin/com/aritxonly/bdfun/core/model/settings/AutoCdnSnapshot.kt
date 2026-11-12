package com.aritxonly.bdfun.core.model.settings

data class AutoCdnSnapshot(val bestId: String? = null, val bestSpeed: Double = 0.0,
    val measuredAt: Long = 0L, val speeds: Map<String, Double> = emptyMap())
