package com.aritxonly.bdfun.core.model.library

data class HistoryPage(
    val items: List<HistoryItem>,
    val max: Long,
    val viewAt: Long,
    val business: String,
    val hasMore: Boolean,
)
