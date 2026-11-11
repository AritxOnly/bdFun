package com.aritxonly.bdfun.core.model.user

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
