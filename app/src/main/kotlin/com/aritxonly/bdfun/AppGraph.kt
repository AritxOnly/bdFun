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
    private var container: com.aritxonly.bdfun.di.AppContainer? = null

    val api: BiliApi get() = checkNotNull(container) { "AppGraph 尚未初始化" }.api
    val settings: SettingsStore get() = checkNotNull(container) { "AppGraph 尚未初始化" }.settings
    val sentHistory: SentHistoryStore
        get() = checkNotNull(container?.sentHistory) { "AppGraph 尚未使用 Context 初始化" }

    /** 登录态：MainActivity 与 LoginActivity 共享，登录成功直接写这里。 */
    val session = mutableStateOf<UserProfile?>(null)

    /** 「我的」点进视频、搜索点进视频等都先写这里，再由 VideoActivity 取。 */
    val pendingVideo = mutableStateOf<VideoItem?>(null)

    /** 动态详情页要展示的动态（由动态列表点进来时写入）。 */
    val pendingDynamic = mutableStateOf<com.aritxonly.bdfun.data.DynamicItem?>(null)

    /** 空间页要展示的 mid。 */
    val pendingSpaceMid = mutableStateOf<Long?>(null)

    @Synchronized
    fun init(api: BiliApi, settings: SettingsStore, context: Context? = null) {
        container = com.aritxonly.bdfun.di.AppContainer(api, settings, context)
    }

    @Synchronized
    fun ensure(context: Context) {
        if (container == null) container = com.aritxonly.bdfun.di.AppContainer.create(context)
    }
}
