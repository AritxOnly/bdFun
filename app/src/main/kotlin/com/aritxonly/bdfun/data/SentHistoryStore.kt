package com.aritxonly.bdfun.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 本机记录：用户在本 App 内发出的评论与弹幕。
 *
 * 官方接口没有「按用户拉取全部评论/弹幕」的公开列表，所以这里只记录经本 App 成功发送的内容；
 * 弹幕额外保存视频内时间点，点击记录即可带着进度跳回原视频。
 */
data class SentRecord(
    val id: Long,
    /** "comment" 或 "danmaku" */
    val kind: String,
    val text: String,
    val bvid: String,
    val title: String,
    /** 弹幕在视频里的时间点（毫秒）；评论为 0。 */
    val videoTimeMs: Long,
    /** 发送时间（毫秒时间戳）。 */
    val sentAt: Long,
)

class SentHistoryStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("sent_history", Context.MODE_PRIVATE)

    fun all(): List<SentRecord> {
        val raw = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.optJSONObject(i) ?: continue
                    add(
                        SentRecord(
                            id = o.optLong("id"),
                            kind = o.optString("kind", "comment"),
                            text = o.optString("text"),
                            bvid = o.optString("bvid"),
                            title = o.optString("title"),
                            videoTimeMs = o.optLong("videoTimeMs"),
                            sentAt = o.optLong("sentAt"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    /** 记录一条本机发出的评论 / 弹幕；重复 id 会覆盖。 */
    fun record(
        kind: String,
        text: String,
        bvid: String,
        title: String,
        videoTimeMs: Long = 0L,
    ) {
        if (text.isBlank()) return
        val now = System.currentTimeMillis()
        add(
            SentRecord(
                id = now,
                kind = kind,
                text = text,
                bvid = bvid,
                title = title,
                videoTimeMs = videoTimeMs,
                sentAt = now,
            ),
        )
    }

    fun add(record: SentRecord) {
        val next = (listOf(record) + all().filterNot { it.id == record.id }).take(MAX_RECORDS)
        val array = JSONArray()
        next.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("kind", item.kind)
                    put("text", item.text)
                    put("bvid", item.bvid)
                    put("title", item.title)
                    put("videoTimeMs", item.videoTimeMs)
                    put("sentAt", item.sentAt)
                },
            )
        }
        prefs.edit().putString(KEY_RECORDS, array.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_RECORDS).apply()
    }

    companion object {
        private const val KEY_RECORDS = "records"
        private const val MAX_RECORDS = 500
    }
}
