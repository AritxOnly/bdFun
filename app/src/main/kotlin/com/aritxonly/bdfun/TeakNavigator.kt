package com.aritxonly.bdfun

import android.content.Context
import android.content.Intent
import com.aritxonly.bdfun.data.VideoItem

/** 页面跳转集中在这里：全部走独立 Activity（系统过渡 + 系统返回栈）。 */
object TeakNavigator {
    fun openSettings(context: Context) {
        context.startActivity(Intent(context, SettingsActivity::class.java))
    }

    fun openLibrary(context: Context) {
        context.startActivity(Intent(context, LibraryActivity::class.java))
    }

    fun openHistory(context: Context) {
        context.startActivity(Intent(context, HistoryActivity::class.java))
    }

    fun openWatchLater(context: Context) {
        context.startActivity(Intent(context, WatchLaterActivity::class.java))
    }

    fun openCache(context: Context) {
        context.startActivity(Intent(context, CacheActivity::class.java))
    }

    fun openMessages(context: Context) {
        context.startActivity(Intent(context, MessageActivity::class.java))
    }

    fun openMessageFeed(context: Context, kind: String) {
        context.startActivity(
            Intent(context, MessageDetailActivity::class.java).putExtra("kind", kind),
        )
    }

    fun openPrivateChat(context: Context, session: com.aritxonly.bdfun.data.MessageSession) {
        context.startActivity(
            Intent(context, PrivateChatActivity::class.java)
                .putExtra("talker_id", session.talkerId)
                .putExtra("session_type", session.sessionType)
                .putExtra("name", session.name)
                .putExtra("avatar", session.avatar),
        )
    }

    fun openLogin(context: Context) {
        context.startActivity(Intent(context, LoginActivity::class.java))
    }

    fun openChangelog(context: Context) {
        context.startActivity(Intent(context, ChangelogActivity::class.java))
    }

    fun openDonation(context: Context) {
        context.startActivity(Intent(context, DonationActivity::class.java))
    }

    fun openCredits(context: Context) {
        context.startActivity(Intent(context, CreditsActivity::class.java))
    }

    fun openLogs(context: Context) {
        context.startActivity(Intent(context, LogActivity::class.java))
    }

    fun openMySent(context: Context) {
        context.startActivity(Intent(context, MySentActivity::class.java))
    }

    fun openSpace(context: Context, mid: Long) {
        if (mid <= 0) return
        AppGraph.pendingSpaceMid.value = mid
        context.startActivity(Intent(context, SpaceActivity::class.java))
    }

    fun openDynamic(context: Context, dynamic: com.aritxonly.bdfun.data.DynamicItem) {
        AppGraph.pendingDynamic.value = dynamic
        context.startActivity(Intent(context, DynamicDetailActivity::class.java))
    }

    fun openVideo(context: Context, video: VideoItem) {
        AppGraph.pendingVideo.value = video
        context.startActivity(Intent(context, VideoActivity::class.java))
    }

    /** 只有 BV 号时直接开播放页，由 VideoActivity 自己拉详情。 */
    fun openVideoByBvid(context: Context, bvid: String) {
        AppGraph.pendingVideo.value = null
        context.startActivity(
            Intent(context, VideoActivity::class.java).putExtra("bvid", bvid),
        )
    }
}
