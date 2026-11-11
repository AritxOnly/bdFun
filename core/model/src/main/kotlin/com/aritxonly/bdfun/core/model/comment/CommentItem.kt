package com.aritxonly.bdfun.core.model.comment

data class CommentItem(
    val rpid: Long,
    val uname: String,
    val avatar: String,
    val message: String,
    val like: Int,
    val ctime: Long,
    val level: Int,
    val replyCount: Int,
    val isUp: Boolean,
    val isTop: Boolean,
    val liked: Boolean = false,
    val mid: Long = 0L,
    val oid: Long = 0L,
    val root: Long = 0L,
    val parent: Long = 0L,
    /** 官方接口随主评论一起返回的前几条回复（不用额外请求）。 */
    val previewReplies: List<CommentItem> = emptyList(),
    /** 该评论正文里用到的表情：code -> 图片地址（来自 content.emote，不需要额外请求）。 */
    val emotes: Map<String, String> = emptyMap(),
    /** 评论 IP 属地（reply_control.location）。 */
    val ipLocation: String = "",
    /** 大会员（评论名称显示主题色）。 */
    val isVip: Boolean = false,
    /** 评论附图（content.pictures，已转成可直接加载的地址）。 */
    val pictures: List<String> = emptyList(),
    /** 评论里 @ 到的人（content.members），用来染色并跳到对方主页。 */
    val mentions: List<CommentMention> = emptyList(),
)
