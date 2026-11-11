package com.aritxonly.bdfun.core.model.user

/** 空间页合集 / 系列。 */
data class SpaceSeason(
    val id: Long,
    val title: String,
    val cover: String,
    val total: Int = 0,
    val isSeries: Boolean = false,
)
