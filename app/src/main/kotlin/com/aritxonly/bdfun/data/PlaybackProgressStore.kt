package com.aritxonly.bdfun.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 本地观看进度：按 bvid 存一份，进入视频时优先用本地进度
 * （从「我的评论与弹幕」跳进来的显式时间点优先级更高）。
 *
 * 每条只有几十字节；同时最多保留 [MAX_ENTRIES] 条，超出后按更新时间淘汰。
 * 这样长期使用也不会让 SharedPreferences 无限长。
 */
object PlaybackProgressStore {
    private const val PREFS = "playback_progress"
    private const val KEY_PREFIX = "p_"
    private const val MIN_SAVE_MS = 3_000L
    private const val END_GUARD_MS = 5_000L
    private const val MAX_ENTRIES = 500
    private const val KEEP_ENTRIES = 400

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var writesSincePrune = 0

    fun init(context: Context) {
        if (prefs != null) return
        val store = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = store
        prune(store)
    }

    /** 返回可续播的位置；太靠前或已经接近结尾时返回 0。 */
    fun resumeFor(bvid: String): Long {
        val store = prefs ?: return 0L
        if (bvid.isBlank()) return 0L
        val raw = store.getString(KEY_PREFIX + bvid, null) ?: return 0L
        val parts = raw.split(",")
        val position = parts.getOrNull(0)?.toLongOrNull() ?: return 0L
        val duration = parts.getOrNull(1)?.toLongOrNull() ?: 0L
        if (position < MIN_SAVE_MS) return 0L
        if (duration > 0 && position > duration - END_GUARD_MS) return 0L
        return position
    }

    fun save(bvid: String, positionMs: Long, durationMs: Long) {
        val store = prefs ?: return
        if (bvid.isBlank()) return
        if (positionMs < MIN_SAVE_MS) return
        if (durationMs > 0 && positionMs > durationMs - END_GUARD_MS) {
            clear(bvid)
            return
        }
        store.edit()
            .putString(
                KEY_PREFIX + bvid,
                "$positionMs,$durationMs,${System.currentTimeMillis()}",
            )
            .apply()
        // 不是每次写都扫一遍；大约每 100 次检查一次上限。
        writesSincePrune += 1
        if (writesSincePrune >= 100) {
            writesSincePrune = 0
            prune(store)
        }
    }

    fun clear(bvid: String) {
        if (bvid.isBlank()) return
        prefs?.edit()?.remove(KEY_PREFIX + bvid)?.apply()
    }

    private fun prune(store: SharedPreferences) {
        val entries = store.all.filterKeys { it.startsWith(KEY_PREFIX) }
        if (entries.size <= MAX_ENTRIES) return
        val oldestFirst = entries.entries
            .sortedBy { (_, value) ->
                (value as? String)?.split(",")?.getOrNull(2)?.toLongOrNull() ?: 0L
            }
        val editor = store.edit()
        oldestFirst.take((entries.size - KEEP_ENTRIES).coerceAtLeast(0))
            .forEach { editor.remove(it.key) }
        editor.apply()
    }
}
