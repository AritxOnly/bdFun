package com.aritxonly.bdfun.core.model.comment

/** 评论正文里被 @ 的用户。 */
data class CommentMention(
    val mid: Long,
    val name: String,
)
