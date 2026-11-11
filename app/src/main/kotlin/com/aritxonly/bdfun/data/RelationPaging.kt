package com.aritxonly.bdfun.data

internal fun relationListParams(mid: Long, fans: Boolean, page: Int, frequent: Boolean): Map<String, String> = buildMap {
    put("vmid", mid.toString()); put("pn", page.toString()); put("ps", "20"); put("order", "desc")
    if (!fans) put("order_type", if (frequent) "attention" else "")
}

internal fun relationNextPage(page: Int, rawCount: Int, total: Int): Int? =
    (page + 1).takeIf { rawCount >= 20 && (total <= 0 || page.toLong() * 20 < total) }
