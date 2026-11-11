package com.aritxonly.bdfun.data

data class VideoItem(
    val bvid: String,
    val aid: Long,
    val title: String,
    val cover: String,
    val ownerName: String,
    val ownerFace: String,
    val ownerMid: Long,
    val play: Long,
    val danmaku: Long,
    val duration: Int,
    val pubdate: Long,
    val cid: Long = 0L,
    val reason: String = "",
    val isFollowed: Boolean = false,
    /** 从「我的评论与弹幕」等入口跳转时，进入视频后直接定位到这个时间点。 */
    val startPositionMs: Long = 0L,
    val categoryName: String = "",
    val recommendationTrackId: String = "",
    val appRecommendation: Boolean = false,
    val dislikeReasons: List<RecommendationReason> = emptyList(),
)

data class RecommendationReason(val id: Long, val name: String)

data class MessageCursor(val id: Long, val time: Long = 0L)
data class MessageFeedPage(val items: List<MessageItem>, val next: MessageCursor?)
data class ChatMessagePage(val items: List<ChatMessage>, val next: Long?)

data class MessageSessionPage(val items: List<MessageSession>, val next: Long?)

data class VideoPage(
    val cid: Long,
    val page: Int,
    val part: String,
    val duration: Int,
)

/** 合集里的单集（`ugc_season`），可能属于另一个 bvid。 */
data class VideoEpisode(
    val bvid: String,
    val aid: Long,
    val cid: Long,
    val title: String,
    val cover: String,
    val duration: Int,
    val play: Long = 0L,
    val danmaku: Long = 0L,
    val pubdate: Long = 0L,
)

data class VideoDetail(
    val bvid: String,
    val aid: Long,
    val title: String,
    val cover: String,
    val desc: String,
    val ownerName: String,
    val ownerFace: String,
    val ownerMid: Long,
    val ownerSign: String,
    val play: Long,
    val danmaku: Long,
    val like: Long,
    val coin: Long,
    val favorite: Long,
    val share: Long,
    val reply: Long,
    val pubdate: Long,
    val duration: Int,
    val cid: Long,
    val tname: String,
    val pages: List<VideoPage>,
    /** 该视频所属 UGC 合集的全部单集（没有合集时为空）。 */
    val episodes: List<VideoEpisode> = emptyList(),
    /** UGC 合集标题（没有合集时为空）。 */
    val seasonTitle: String = "",
    val isFollowed: Boolean = false,
    val liked: Boolean = false,
    val coined: Boolean = false,
    val favorited: Boolean = false,
)

data class Quality(
    val id: Int,
    val label: String,
    val width: Int,
    val height: Int,
    val frameRate: String,
    val codec: String,
    val videoUrl: String,
    /** CDN 替换前的原始地址，CDN 403 时用来重试。 */
    val rawVideoUrl: String = "",
)

data class DanmakuItem(
    val timeMs: Long,
    val mode: Int,
    val color: Int,
    val size: Float,
    val text: String,
    /** 服务端弹幕 id，用于热门弹幕去重 / 点赞。 */
    val id: Long = 0L,
    /** 服务端点赞数（分段弹幕接口 like 字段）。 */
    val likeCount: Long = 0L,
    /** 服务端合并的重复弹幕数量（count 字段）。 */
    val count: Int = 0,
    /** 自己刚发送的弹幕：渲染时加圆角底。 */
    val self: Boolean = false,
)

/** 空降助手片段（来自 BilibiliSponsorBlock 公共 API）。 */
data class SponsorSegment(
    val startMs: Long,
    val endMs: Long,
    val category: String,
    val actionType: String,
    val uuid: String,
)

data class PlayInfo(
    val qualities: List<Quality>,
    val audioUrl: String?,
    val fallbackUrl: String?,
    val durationMs: Long,
    val acceptQuality: List<Int>,
    /** CDN 替换前的原始音轨/兜底地址。 */
    val rawAudioUrl: String? = null,
    val rawFallbackUrl: String? = null,
) {
    val isDash: Boolean get() = qualities.isNotEmpty() && audioUrl != null
}

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

/** 评论正文里被 @ 的用户。 */
data class CommentMention(
    val mid: Long,
    val name: String,
)

/** 评论表情（B 站表情包，插入时用 `[文本]`）。 */
data class EmoteItem(
    val text: String,
    val url: String,
)

data class CommentPage(
    val items: List<CommentItem>,
    val page: Int,
    val isEnd: Boolean,
    val total: Int,
)

data class UserProfile(
    val mid: Long,
    val name: String,
    val face: String,
    val sign: String,
    val level: Int,
    val coins: Double,
    val vip: Boolean,
    val vipLabel: String,
    val follower: Long = 0,
    val following: Long = 0,
    /** 获赞数（空间页用）。 */
    val likes: Long = 0,
    /** 空间头图。 */
    val banner: String = "",
    val bannerFallbacks: List<String> = emptyList(),
    val bannerAlignmentY: Float = 0f,
    /** 空间页资料：认证标题 / 等级经验（接口没有时为空 / 0）。 */
    val officialTitle: String = "",
    val officialDesc: String = "",
    val officialType: Int = 0,
    val fansMedalName: String = "",
    val fansMedalLevel: Int = 0,
    val fansMedalColor: Int = 0,
    val chargingCount: Long = 0,
    val levelExp: Long = 0,
    val nextLevelExp: Long = 0,
)

data class QrLogin(
    val url: String,
    val key: String,
)

data class QrPoll(
    val state: QrState,
    val message: String,
    val profile: UserProfile? = null,
)

enum class QrState { Waiting, Scanned, Expired, Success, Error }

data class DynamicItem(
    val id: String,
    val name: String,
    val face: String,
    val mid: Long,
    val time: String,
    val text: String,
    val images: List<String>,
    val video: VideoItem?,
    val like: Int,
    val comment: Int,
    val forward: Int,
    val liked: Boolean = false,
    /** 评论区 oid / type：动态接口的 basic.comment_id_str / comment_type。 */
    val commentOid: String = id,
    val commentType: Int = 17,
    /** 专栏 / 图文 opus 的标题（正文仍放 [text]）。 */
    val title: String = "",
    /** 转发动态的原动态内容。 */
    val orig: DynamicItem? = null,
    /** 年度大会员 UP：只有这种 UP 的名字在动态卡片里显示粉色。 */
    val authorVip: Boolean = false,
    /** 合集 / UGC 合集视频，封面上显示「合集」小胶囊。 */
    val isCollection: Boolean = false,
    /** 番剧 / 电影 / 纪录片等 PGC 内容（动态 PGC 卡）。 */
    val pgcCover: String = "",
    val pgcTitle: String = "",
    val pgcLabel: String = "",
    val moduleTag: String = "",
    val liveRecommendation: DynamicLiveRecommendation? = null,
    /** 动态发布 IP 属地（接口 `module_author.pub_location_text`）。 */
    val ipLocation: String = "",
)

data class DynamicPage(
    val items: List<DynamicItem>,
    val offset: String,
    val hasMore: Boolean,
)

data class HistoryPage(
    val items: List<HistoryItem>,
    val max: Long,
    val viewAt: Long,
    val business: String,
    val hasMore: Boolean,
)

data class HistoryItem(
    val bvid: String,
    val title: String,
    val cover: String,
    val author: String,
    val viewedAt: Long,
    val progress: Int,
    val duration: Int,
    /** 播放量 / 弹幕数（接口里的 stat 字段，历史与稍后再看都有） */
    val play: Int = 0,
    val danmaku: Int = 0,
    /** 稿件 aid：批量删除历史 / 稍后再看时要用；历史游标接口放在 history.oid 里。 */
    val aid: Long = 0,
)

/** 搜索结果里的用户卡片（search_type = bili_user）。 */
data class SearchUser(
    val mid: Long,
    val name: String,
    val face: String,
    val sign: String,
    val fans: Long = 0,
    val videos: Long = 0,
    val level: Int = 0,
)

data class FavoriteFolder(
    val id: Long,
    val title: String,
    val count: Int,
    val cover: String,
)

data class FollowedUser(
    val mid: Long,
    val name: String,
    val face: String,
    val sign: String,
)

data class SpaceInfo(
    val profile: UserProfile,
    val videos: List<VideoItem>,
    val videoCount: Int = 0,
    val isFollowed: Boolean = false,
)

/** 空间页合集 / 系列。 */
data class SpaceSeason(
    val id: Long,
    val title: String,
    val cover: String,
    val total: Int = 0,
    val isSeries: Boolean = false,
)

/** 空间页追番。 */
data class SpaceBangumi(
    val seasonId: Long,
    val title: String,
    val cover: String,
    val progress: String = "",
    val total: Int = 0,
)

data class SpaceShopItem(
    val title: String,
    val cover: String = "",
    val price: String = "",
    val source: String = "",
    val url: String = "",
)

/** 空间页图文投稿。 */
data class SpaceArticle(
    val id: Long,
    val title: String,
    val cover: String = "",
    val view: Long = 0,
    val publishTime: Long = 0,
)

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

/** 私信会话里的一条消息。 */
data class ChatMessage(
    val id: Long,
    val senderId: Long,
    val timestamp: Long,
    val text: String,
    val imageUrl: String = "",
    val isMine: Boolean = false,
    val sequence: Long = 0L,
)

/** 消息中心的一个会话（系统通知 / 私信）。 */
data class MessageSession(
    val id: String,
    val talkerId: Long,
    val sessionType: Int,
    val name: String,
    val avatar: String,
    val preview: String,
    val time: Long,
    val unread: Int,
    val isSystem: Boolean,
    val maxSeqno: Long = 0L,
    /** 原始 session_ts（微秒），用于消息列表翻页。 */
    val sessionTs: Long = 0L,
    val followed: Boolean = false,
    val pinned: Boolean = false,
    val muted: Boolean = false,
)

/** 官方字幕轨（`/x/player/v2` 的 subtitle.subtitles）。 */
data class SubtitleTrack(
    val id: String,
    val label: String,
    val url: String,
    val isAi: Boolean = false,
)

/** 字幕单句。 */
data class SubtitleCue(
    val fromMs: Long,
    val toMs: Long,
    val text: String,
)

/** Relationship here always belongs to the logged-in account, never the viewed space owner. */
data class RelationUser(
    val mid: Long, val name: String, val face: String, val sign: String,
    val attribute: Int? = null,
) {
    val followed: Boolean get() = attribute == 2 || attribute == 6
}
data class RelationPage(val items: List<RelationUser>, val total: Int, val nextPage: Int?)
