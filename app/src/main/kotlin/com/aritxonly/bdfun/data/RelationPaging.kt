package com.aritxonly.bdfun.data

internal fun relationListParams(mid: Long, fans: Boolean, page: Int, frequent: Boolean) =
    com.aritxonly.bdfun.core.domain.relationListParams(mid, fans, page, frequent)

internal fun relationNextPage(page: Int, rawCount: Int, total: Int) =
    com.aritxonly.bdfun.core.domain.relationNextPage(page, rawCount, total)
