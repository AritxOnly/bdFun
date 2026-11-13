package com.aritxonly.bdfun.core.model.feed

data class DynamicPage(
    val items: List<DynamicItem>,
    val offset: String,
    val hasMore: Boolean,
)
