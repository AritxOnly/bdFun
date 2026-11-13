package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.user.SentRecord

/** 本机成功发送记录，沿用旧同步快照与写入契约。 */
interface SentHistoryRepository {
    fun all(): List<SentRecord>
    fun record(kind: String, text: String, bvid: String, title: String, videoTimeMs: Long = 0L)
    fun add(record: SentRecord)
    fun clear()
}
