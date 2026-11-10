package com.aritxonly.bdfun

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.CdnAutoSelector
import com.aritxonly.bdfun.data.PlaybackProgressStore
import com.aritxonly.bdfun.data.SentHistoryStore
import com.aritxonly.bdfun.data.SettingsStore
import com.aritxonly.bdfun.data.UserProfile
import com.aritxonly.bdfun.data.VideoCacheManager
import com.aritxonly.bdfun.data.VideoItem

/**
 * 进程级单例：多 Activity 共用同一套 api / settings / 登录态 / 待打开的视频。
 */
object AppGraph {
    private var initialized = false

    lateinit var api: BiliApi
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var sentHistory: SentHistoryStore
        private set

    /** 登录态：MainActivity 与 LoginActivity 共享，登录成功直接写这里。 */
    val session = mutableStateOf<UserProfile?>(null)

    /** 「我的」点进视频、搜索点进视频等都先写这里，再由 VideoActivity 取。 */
    val pendingVideo = mutableStateOf<VideoItem?>(null)

    /** 动态详情页要展示的动态（由动态列表点进来时写入）。 */
    val pendingDynamic = mutableStateOf<com.aritxonly.bdfun.data.DynamicItem?>(null)

    /** 空间页要展示的 mid。 */
    val pendingSpaceMid = mutableStateOf<Long?>(null)

    fun init(api: BiliApi, settings: SettingsStore, context: Context? = null) {
        this.api = api
        this.settings = settings
        ThemePalette.style = settings.colorStyle
        com.aritxonly.teak.common.settings.TeakDebugLog.enabled =
            com.aritxonly.bdfun.BuildConfig.DEBUG
        if (com.aritxonly.bdfun.BuildConfig.DEBUG) {
            com.aritxonly.teak.common.settings.TeakDebugLog.sink = { tag, message ->
                com.aritxonly.bdfun.data.LogCollector.record(tag, message)
                if (tag == "MENU") com.aritxonly.bdfun.data.LogCollector.flushSync()
            }
        } else {
            // release：不接 sink，不保留任何日志回调。
            com.aritxonly.teak.common.settings.TeakDebugLog.sink = null
        }
        if (context != null) {
            sentHistory = SentHistoryStore(context.applicationContext)
            VideoCacheManager.init(context.applicationContext)
            CdnAutoSelector.init(context.applicationContext)
            PlaybackProgressStore.init(context.applicationContext)
            if (com.aritxonly.bdfun.BuildConfig.DEBUG) {
                com.aritxonly.bdfun.data.FreezeWatchdog.start(context.applicationContext)
            }
        }
        initialized = true
    }

    fun ensure(context: Context) {
        if (initialized) return
        com.aritxonly.teak.common.settings.TeakDebugLog.enabled =
            com.aritxonly.bdfun.BuildConfig.DEBUG
        if (com.aritxonly.bdfun.BuildConfig.DEBUG) {
            com.aritxonly.teak.common.settings.TeakDebugLog.sink = { tag, message ->
                com.aritxonly.bdfun.data.LogCollector.record(tag, message)
                if (tag == "MENU") com.aritxonly.bdfun.data.LogCollector.flushSync()
            }
        } else {
            // release：不接 sink，不保留任何日志回调。
            com.aritxonly.teak.common.settings.TeakDebugLog.sink = null
        }
        val app = context.applicationContext
        settings = SettingsStore(app)
        api = BiliApi(app)
        sentHistory = SentHistoryStore(app)
        VideoCacheManager.init(app)
        CdnAutoSelector.init(app)
        PlaybackProgressStore.init(app)
        ThemePalette.style = settings.colorStyle
        if (com.aritxonly.bdfun.BuildConfig.DEBUG) {
            com.aritxonly.bdfun.data.FreezeWatchdog.start(app)
        }
        initialized = true
    }
}
