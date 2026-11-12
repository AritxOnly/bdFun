package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.DanmakuItem

data class DanmakuFilterOptions(val dedupe: Boolean, val blockScroll: Boolean, val blockFixed: Boolean,
    val blockColor: Boolean, val blockAdvanced: Boolean, val blockCount: Boolean,
    val keywordFilter: Boolean, val keywords: Set<String>)
/** 先过滤再按原输入顺序去重，最后排序；保留原 mode 1..5 与区分大小写规则。 */
fun filterDanmaku(items: List<DanmakuItem>, options: DanmakuFilterOptions): List<DanmakuItem> = with(options) {
    items.asSequence()
        .filter { it.text.isNotBlank() && it.mode in 1..5 }
        .filter { item ->
            when {
                blockScroll && item.mode == 1 -> false
                blockFixed && (item.mode == 4 || item.mode == 5) -> false
                blockColor && item.color != 0xFFFFFF -> false
                blockAdvanced && item.mode == 7 -> false
                blockCount && item.mode == 8 -> false
                keywordFilter && keywords.any { key -> key.isNotBlank() && item.text.contains(key) } -> false
                else -> true
            }
        }
        .let { source -> if (dedupe) source.distinctBy { it.text } else source }
        .sortedBy { it.timeMs }
        .toList()
}
