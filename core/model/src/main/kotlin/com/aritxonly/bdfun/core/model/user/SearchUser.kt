package com.aritxonly.bdfun.core.model.user

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
