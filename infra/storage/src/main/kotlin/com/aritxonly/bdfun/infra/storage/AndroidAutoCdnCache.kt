package com.aritxonly.bdfun.infra.storage

import android.content.Context
import com.aritxonly.bdfun.core.domain.AutoCdnCache
import com.aritxonly.bdfun.core.model.settings.AutoCdnSnapshot

/** 沿用旧测速文件和 float 精度，升级时不迁移或覆盖数据。 */
class AndroidAutoCdnCache(context: Context) : AutoCdnCache {
    private val prefs = context.applicationContext.getSharedPreferences("cdn_auto", Context.MODE_PRIVATE)
    override fun read() = AutoCdnSnapshot(prefs.getString("best_id", null),
        prefs.getFloat("best_speed", 0f).toDouble(), prefs.getLong("measured_at", 0L),
        decodeCdnSpeeds(prefs.getString("speeds", "").orEmpty()))
    override fun write(snapshot: AutoCdnSnapshot) {
        prefs.edit().putString("best_id", snapshot.bestId).putFloat("best_speed", snapshot.bestSpeed.toFloat())
            .putLong("measured_at", snapshot.measuredAt).putString("speeds", encodeCdnSpeeds(snapshot.speeds)).apply()
    }
}
internal fun encodeCdnSpeeds(values: Map<String, Double>): String =
    values.entries.joinToString(",") { "${it.key}:${it.value}" }
internal fun decodeCdnSpeeds(raw: String): Map<String, Double> {
    if (raw.isBlank()) return emptyMap()
    return raw.split(",").mapNotNull { part ->
        val index = part.lastIndexOf(':')
        if (index <= 0) return@mapNotNull null
        val speed = part.substring(index + 1).toDoubleOrNull() ?: return@mapNotNull null
        part.substring(0, index) to speed
    }.toMap()
}
