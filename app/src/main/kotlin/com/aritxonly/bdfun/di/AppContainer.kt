package com.aritxonly.bdfun.di

import android.content.Context
import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.bdfun.ThemePalette
import com.aritxonly.bdfun.data.*
import com.aritxonly.teak.common.settings.TeakDebugLog

/** 使用 applicationContext，避免进程级依赖持有 Activity。 */
internal class AppContainer(
    val api: BiliApi,
    val settings: SettingsStore,
    context: Context?,
) {
    val sentHistory: SentHistoryStore? = context?.let { SentHistoryStore(it.applicationContext) }

    init {
        ThemePalette.style = settings.colorStyle
        TeakDebugLog.enabled = BuildConfig.DEBUG
        TeakDebugLog.sink = if (BuildConfig.DEBUG) { tag, message ->
            LogCollector.record(tag, message)
            if (tag == "MENU") LogCollector.flushSync()
        } else null
        context?.applicationContext?.let { app ->
            VideoCacheManager.init(app)
            CdnAutoSelector.init(app)
            PlaybackProgressStore.init(app)
            if (BuildConfig.DEBUG) FreezeWatchdog.start(app)
        }
    }

    companion object {
        fun create(context: Context): AppContainer {
            val app = context.applicationContext
            return AppContainer(BiliApi(app), SettingsStore(app), app)
        }
    }
}
