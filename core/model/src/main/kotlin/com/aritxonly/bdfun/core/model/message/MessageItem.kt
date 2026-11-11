package com.aritxonly.bdfun.core.model.message

data class MessageItem(
    val id: String,
    val title: String,
    val content: String,
    val time: Long,
    val name: String,
    val avatar: String,
    val bvid: String = "",
    /** 通知动作文案，如「回复了我的评论」。 */    val action: String = "",
    /** 被回复/点赞的原内容摘要。 */
    val source: String = "",
    val userMid: Long = 0L,
    val followed: Boolean = false,
    val likeNames: List<String> = emptyList(),
    val likeCount: Int = 0,
    /** 原视频 aid：点消息直接回到对应评论。 */
    val aid: Long = 0L,
    /** 评论根 / 被回复 / 这条回复的 rpid；动态评论也能用。 */
    val rootRpid: Long = 0L,
    val targetRpid: Long = 0L,
    val sourceRpid: Long = 0L,
    /** 1 = 视频评论，11/17 = 动态等。 */
    val businessId: Int = 0,
    /** 原生链接里带的播放时间（毫秒），没有就是 0。 */
    val seekMs: Long = 0L,
    /** 消息卡片底部的浅色引用：我在这条楼中楼里发过的话。 */
    val quote: String = "",
    /** 消息卡片右侧显示的文字（层主评论 / 我的评论）。 */
    val subjectText: String = "",
    /** 消息卡片右侧显示的视频封面。 */
    val subjectCover: String = "",
    /** 右侧是视频封面，而不是文字。 */
    val videoSubject: Boolean = false,
    /** 这条回复来自视频 UP 主。 */
    val isUp: Boolean = false,
    /** 「@我」消息。 */
    val atMe: Boolean = false,
    /** 别人在我的视频下发了评论。 */
    val commentOnMyVideo: Boolean = false,
    val nativeUri: String = "",
    val systemCursor: Long = 0,
    val noticeState: Int = 0,
)
