package com.aritxonly.bdfun.infra.storage

import android.content.Context
import android.content.SharedPreferences
import com.aritxonly.bdfun.core.domain.*

/** 原文件名、键名和 position,duration,timestamp 字符串不变，可直接读取旧安装数据。 */
class AndroidPlaybackProgressRepository(context: Context?, private val clock: () -> Long = System::currentTimeMillis) : PlaybackProgressRepository {
    private val prefs = context?.applicationContext?.getSharedPreferences("playback_progress", Context.MODE_PRIVATE)
    @Volatile private var writesSincePrune = 0
    init { prefs?.let(::prune) }
    override fun resumeFor(bvid: String): Long {
        val store = prefs ?: return 0L
        if (bvid.isBlank()) return 0L
        return PlaybackProgressPolicy.resume(store.getString("p_$bvid", null))
    }
    override fun save(bvid: String, positionMs: Long, durationMs: Long) {
        val store = prefs ?: return
        if (bvid.isBlank()) return
        when (PlaybackProgressPolicy.decision(positionMs, durationMs)) {
            ProgressSaveDecision.Skip -> return
            ProgressSaveDecision.Clear -> { clear(bvid); return }
            ProgressSaveDecision.Write -> store.edit().putString("p_$bvid", "$positionMs,$durationMs,${clock()}").apply()
        }
        writesSincePrune++
        if (writesSincePrune >= 100) { writesSincePrune = 0; prune(store) }
    }
    override fun clear(bvid: String) {
        if (bvid.isNotBlank()) prefs?.edit()?.remove("p_$bvid")?.apply()
    }
    private fun prune(store: SharedPreferences) {
        val entries = store.all.filterKeys { it.startsWith("p_") }.mapValues { it.value as? String }
        val keys = PlaybackProgressPolicy.evictions(entries)
        if (keys.isEmpty()) return
        val editor = store.edit()
        keys.forEach { editor.remove(it) }
        editor.apply()
    }
}
