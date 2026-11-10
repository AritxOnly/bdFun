package com.aritxonly.bdfun.data

/**
 * 播放器滚动收起模式（完全照搬 BiliPai 的 PortraitPlayerCollapseMode）。
 *
 * - OFF：播放时不随滚动收起；暂停后仍可上滑简介/评论收起画面。
 * - INTRO_ONLY：竖屏视频在简介或评论上滑时收起。
 * - COMMENT_ONLY：仅横屏视频详情页滚动时收起。
 * - BOTH：横屏和竖屏视频都随浏览收起。
 * - PAUSED_ONLY：视频暂停后，上滑评论隐藏视频，上滑简介缩小播放器。
 */
enum class PlayerCollapseMode(val value: Int, val label: String, val description: String) {
    OFF(0, "关闭", "关闭播放时滚动缩小；暂停后仍可上滑简介或评论收起画面"),
    INTRO_ONLY(1, "竖屏", "竖屏视频评论区或简介上滑时缩小播放器"),
    COMMENT_ONLY(2, "横屏", "仅横屏视频详情页滚动时缩小播放器"),
    BOTH(3, "全部", "横屏和竖屏视频均可随浏览缩小"),
    PAUSED_ONLY(4, "暂停时", "视频暂停后，上滑评论隐藏视频，上滑简介缩小播放器");

    val enablesPortraitVideo: Boolean
        get() = this == INTRO_ONLY || this == BOTH || this == PAUSED_ONLY

    val enablesLandscapeVideo: Boolean
        get() = this == COMMENT_ONLY || this == BOTH || this == PAUSED_ONLY

    fun enablesVideoOrientation(isVerticalVideo: Boolean): Boolean =
        if (isVerticalVideo) enablesPortraitVideo else enablesLandscapeVideo

    val enablesIntro: Boolean
        get() = this != OFF

    val enablesComment: Boolean
        get() = this != OFF

    companion object {
        fun fromValue(value: Int): PlayerCollapseMode =
            entries.find { it.value == value } ?: OFF
    }
}
