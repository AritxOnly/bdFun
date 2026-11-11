package com.aritxonly.bdfun.core.model.feed

import com.aritxonly.bdfun.core.model.video.VideoItem

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
