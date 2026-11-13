package com.aritxonly.bdfun.ui

/** 0置顶、2置底，其余定位当前集；懒列表通过索引访问未组合的远处条目。 */
internal fun videoCollectionScrollIndex(target: Int?, currentIndex: Int, count: Int): Int? = when {
    count <= 0 -> null
    target == 0 -> 0
    target == 2 -> count - 1
    else -> currentIndex.coerceIn(0, count - 1)
}
