package com.aritxonly.bdfun.core.domain

/** 保留原跨P隔离及按时长判断秒/毫秒的续播规则。 */
fun resolveCloudResumePositionMs(lastCid: Long, cid: Long, raw: Long, durationSec: Int): Long {
    if (lastCid > 0 && lastCid != cid) return 0L
    if (raw <= 0) return 0L
    // 接口有的版本给秒、有的给毫秒：按视频时长判断一下，避免差 1000 倍。
    val ms = if (durationSec > 0 && raw <= durationSec * 2L) raw * 1000L else raw
    return ms.coerceAtLeast(0L)
}
