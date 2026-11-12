package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** 原实现字段形状的离线回归，不冒充真实登录账号响应。 */
class MessageResponseMapperTest {
    private fun message(type: Int, sequence: Long, content: String, status: Int = 0) = JSONObject()
        .put("msg_type", type).put("msg_seqno", sequence).put("content", content)
        .put("msg_status", status).put("sender_uid", 42)

    @Test fun privateMessagesRetainBlankTextWithdrawalsAndUnknownPayloadThenSortBySequence() {
        val messages = JSONArray()
            .put(message(2, 4, """{"url":"hidden"}""", 1))
            .put(message(2, 2, """{"url":"image"}"""))
            .put(message(1, 1, "invalid-json"))
            .put(message(99, 3, "legacy-text"))
            .put(message(7, 5, """{"title":"标题"}"""))
            .put(message(6, 6, "{}"))
        val page = mapPrivateMessagePage(JSONObject().put("messages", messages)
            .put("min_seqno", "1").put("has_more", 1), 42, 10)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), page.items.map { it.sequence })
        assertEquals("", page.items.first().text)
        assertEquals("image", page.items[1].imageUrl)
        assertEquals("legacy-text", page.items[2].text)
        assertEquals("消息已撤回", page.items[3].text)
        assertEquals("", page.items[3].imageUrl)
        assertEquals("标题", page.items[4].text)
        assertTrue(page.items.all { it.isMine })
        assertEquals(1L, page.next)
    }

    @Test fun privateIdentityTimeSharedVideoAndRepeatedCursorKeepLegacyFallbacks() {
        val shared = """{"bvid":"BV1orW46aEba","title":"视频样本","view":9}"""
        val first = message(1, 900000000000L, JSONObject().put("content", shared).toString())
            .put("msg_key", -7).put("timestamp", 0).put("msg_time", 123)
        val second = message(1, 900000000001L, "{}").put("sender_uid", 99).put("timestamp", 456)
        val page = mapPrivateMessagePage(JSONObject().put("messages", JSONArray().put(first).put(second))
            .put("min_seqno", "5").put("has_more", 1), 42, 5)
        assertEquals(-7L, page.items.first().id)
        assertEquals(123L, page.items.first().timestamp)
        assertEquals("BV1orW46aEba", page.items.first().sharedVideo!!.bvid)
        assertEquals(900000000001L, page.items[1].id)
        assertEquals(456L, page.items[1].timestamp)
        assertFalse(page.items[1].isMine)
        assertNull(page.next)
    }

    @Test fun sessionMappingKeepsGroupFallbackFlagsAndUnfilteredContinuation() {
        val followed = JSONObject("""{"talker_id":3690989657000489,"session_type":2,"is_follow":1,
            "group_name":"群聊","group_cover":"group-cover","session_ts":50000000,"top_ts":1,"is_dnd":1,
            "system_msg_type":1,"unread_count":7,"max_seqno":99,
            "last_msg":{"msg_type":1,"content":"{\"content\":\"第一行\\n第二行\"}"}}""")
        val ignored = JSONObject("""{"talker_id":8,"is_follow":0,"attribute":6,"session_ts":33000000}""")
        val data = JSONObject().put("session_list", JSONArray().put(followed).put(ignored))
        val mapped = mapMessageSessions(data, true, 2)
        val session = mapped.items.single()
        assertEquals("2_3690989657000489", session.id)
        assertEquals("群聊", session.name)
        assertEquals("group-cover", session.avatar)
        assertEquals("第一行 第二行", session.preview)
        assertEquals(50L, session.time)
        assertTrue(session.isSystem && session.pinned && session.muted && session.followed)
        assertEquals(99L, session.maxSeqno)
        assertEquals(33000000L, mapped.nextTimestamp)
        assertTrue(mapped.hasMore)
        assertFalse(mapMessageSessions(data.put("has_more", 0), true, 2).hasMore)
    }

    @Test fun sessionPreviewTypesAndTimestampFallbackRemainDistinctFromChatBody() {
        assertEquals("[图片]", previewOfMessage(message(2, 1, "invalid")))
        assertEquals("", previewOfMessage(message(306, 1, "invalid")))
        assertEquals("desc", previewOfMessage(message(10, 1, """{"desc":"desc"}""")))
        assertEquals("raw text", previewOfMessage(message(99, 1, "raw\ntext")))
        val session = parseMessageSession(JSONObject("""{"talker_id":4,"session_ts":1,
            "account_info":{"name":"账号","pic_url":"pic"},"last_msg":{"timestamp":77}}"""))
        assertEquals("账号", session.name)
        assertEquals("pic", session.avatar)
        assertEquals(77L, session.time)
    }

    @Test fun likeBucketsDeduplicateLatestFirstAndReadOnlyTotalCursor() {
        val latest = JSONObject("""{"id":1,"item":{"source_content":"latest"}}""")
        val total = JSONObject("""{"id":1,"item":{"source_content":"duplicate"}}""")
        val second = JSONObject("""{"id":2,"item":{"source_content":"second"}}""")
        val data = JSONObject().put("latest", JSONObject().put("items", JSONArray().put(latest)))
            .put("total", JSONObject().put("items", JSONArray().put(total).put(second))
                .put("cursor", JSONObject().put("id", 90).put("time", 800).put("is_end", false)))
        val mapped = mapMessageFeed(data, 2)
        assertEquals(2, mapped.rawSize)
        assertEquals(listOf("1", "2"), mapped.items.map { it.id })
        assertEquals("latest", mapped.items.first().source)
        assertEquals(90L, mapped.nextId)
        assertEquals(800L, mapped.nextTime)
        assertFalse(mapped.isEnd)
        assertTrue(mapMessageFeed(JSONObject(), 0).isEnd)
    }

    @Test fun systemAndNewFollowerMappingPreserveFallbacksAndLargeIds() {
        val system = mapSystemMessages(JSONObject("""{"data":[
            {"id":"a","content":"plain","time_at":"时间","cursor":900000000000},
            {"id":"b","content":"{\"web\":\"web-content\"}"}]}"""))
        assertEquals(listOf("plain", "web-content"), system.map { it.content })
        assertEquals(900000000000L, system.first().systemCursor)
        assertEquals("时间", system.first().source)
        assertTrue(system.all { it.time == 0L && it.name == "系统通知" })
        val followers = mapNewFollowers(JSONObject("""{"list":[{"mid":0},
            {"mid":3690989657000489,"attribute":6,"mtime":0,"ctime":123,"sign":"签名"},
            {"mid":8,"attribute":1},{"mid":9,"is_follow":true}]}"""))
        assertEquals(listOf(3690989657000489L, 8L, 9L), followers.map { it.userMid })
        assertEquals(123L, followers.first().time)
        assertEquals("签名", followers.first().source)
        assertEquals(listOf(true, false, true), followers.map { it.followed })
    }

    @Test fun unreadCountersKeepNegativeClampingAndOriginalCategorySets() {
        val privateData = JSONObject("""{"follow_unread":2,"unfollow_unread":3,"other":999}""")
        val feed = JSONObject("""{"reply":-7,"at":4,"like":5,"sys_msg":1,"other":999}""")
        assertEquals(mapOf("reply" to 0, "at" to 4, "like" to 5, "sys_msg" to 1), mapFeedUnreadCounts(feed))
        assertEquals(15, mapTotalUnread(privateData, feed))
        assertEquals(0, mapTotalUnread(JSONObject(), JSONObject()))
    }
}
