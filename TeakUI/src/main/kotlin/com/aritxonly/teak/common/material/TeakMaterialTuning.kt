package com.aritxonly.teak.common.material

import kotlin.math.abs

object TeakMaterialTuning {
    const val MinBlurRadius = 28f
    const val DefaultBlurRadius = 92f
    const val MaxBlurRadius = 256f
    fun normalizeBlurRadius(value: Float): Float = if (value.isFinite()) {
        value.coerceIn(MinBlurRadius, MaxBlurRadius)
    } else {
        DefaultBlurRadius
    }

    fun saturationForBlurRadius(value: Float): Float {
        val radius = normalizeBlurRadius(value)
        val levels = TeakMaterialLevel.entries
        val upperIndex = levels.indexOfFirst { radius <= it.blurRadius }
        if (upperIndex <= 0) return levels.first().blurSaturation

        val upper = levels[upperIndex]
        val lower = levels[upperIndex - 1]
        val fraction = (radius - lower.blurRadius) / (upper.blurRadius - lower.blurRadius)
        return lower.blurSaturation + (upper.blurSaturation - lower.blurSaturation) * fraction
    }

    fun nearestLevel(value: Float): TeakMaterialLevel {
        val radius = normalizeBlurRadius(value)
        return TeakMaterialLevel.entries.minBy { level -> abs(level.blurRadius - radius) }
    }

}
