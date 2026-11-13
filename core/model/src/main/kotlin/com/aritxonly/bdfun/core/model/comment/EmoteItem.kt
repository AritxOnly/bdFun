package com.aritxonly.bdfun.core.model.comment

/** 评论表情（B 站表情包，插入时用 `[文本]`）。 */
data class EmoteItem(
    val text: String,
    val url: String,
)
