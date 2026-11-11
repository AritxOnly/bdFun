package com.aritxonly.bdfun.di

import android.content.Context
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.infra.bili.BiliAccountRepository
import com.aritxonly.bdfun.infra.bili.BiliCommentRepository
import com.aritxonly.bdfun.infra.bili.BiliFeedRepository
import com.aritxonly.bdfun.infra.bili.BiliVideoRepository
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
    val accountRepository: AccountRepository = BiliAccountRepository(api::nav, api::qrGenerate, api::qrPoll, api::logout)
    val videoRepository: VideoRepository = BiliVideoRepository(api::view, api::related)
    val commentRepository: CommentRepository = BiliCommentRepository(
        { oid, type, page -> api.commentsByOid(oid, type, page) },
        { oid, type, root, page -> api.repliesByOid(oid, type, root, page) },
    )
    val feedRepository: FeedRepository = BiliFeedRepository(
        { page -> api.recommend(page, settings.recommendWeb) },
        { offset -> api.dynamicPage("all", offset) },
    )
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
