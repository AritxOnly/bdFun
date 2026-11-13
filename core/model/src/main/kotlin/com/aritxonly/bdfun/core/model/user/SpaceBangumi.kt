package com.aritxonly.bdfun.core.model.user

/** 空间页追番。 */
data class SpaceBangumi(
    val seasonId: Long,
    val title: String,
    val cover: String,
    val progress: String = "",
    val total: Int = 0,
)
