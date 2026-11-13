package com.aritxonly.bdfun.core.model.comment

/** 评论表情（B 站表情包，插入时用 `[文本]`）。 */
data class EmoteItem(
    val text: String,
    val url: String,
    val packageId: Int = 0,
    val packageIcon: String = "",
    val type: Int = 1,
    val size: Int = 1,
)
