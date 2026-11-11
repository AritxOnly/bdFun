package com.aritxonly.bdfun.core.model.comment

data class CommentPage(
    val items: List<CommentItem>,
    val page: Int,
    val isEnd: Boolean,
    val total: Int,
)
