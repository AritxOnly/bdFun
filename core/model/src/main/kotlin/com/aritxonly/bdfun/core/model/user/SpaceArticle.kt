package com.aritxonly.bdfun.core.model.user

/** 空间页图文投稿。 */
data class SpaceArticle(
    val id: Long,
    val title: String,
    val cover: String = "",
    val view: Long = 0,
    val publishTime: Long = 0,
)
