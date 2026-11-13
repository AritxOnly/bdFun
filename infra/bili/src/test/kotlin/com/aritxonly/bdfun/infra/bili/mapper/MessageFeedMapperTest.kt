package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** 沿用旧代码已识别字段的结构用例，不冒充线上采样响应。 */
class MessageFeedMapperTest {
    @Test fun mentionUsesAtTimeAndBusinessLabel() {
        val result = mapMessageFeedItem(MessageFeedItemDto(1, JSONObject("""{
            "id":3,"at_time":1234,"item":{"business":"动态","source_content":"提及正文"}
        }""")))!!
        assertEquals(1234L, result.time)
        assertEquals("在动态中@了我", result.action)
    }

    @Test fun likeBucketsRemainDistinctAndDuplicateUsesLatest() {
        val result = mapMessageFeed(JSONObject("""{
            "latest":{"items":[{"id":1,"users":[{"mid":3}]}]},
            "total":{"items":[{"id":1},{"id":2,"users":[{"mid":4}]}],
                "cursor":{"id":2,"time":100,"is_end":false}}
        }"""), 2)
        assertEquals(listOf("1", "2"), result.items.map { it.id })
        assertTrue(result.items[0].presentation.latestLike)
        assertFalse(result.items[1].presentation.latestLike)
        assertEquals(2L, result.nextId)
    }

    @Test fun likesAndMentionsRetainUpstreamImage() {
        for (type in listOf(1, 2)) {
            val result = mapMessageFeedItem(MessageFeedItemDto(type,
                JSONObject("""{"id":1,"content":"内容","item":{"image":"thumb"}}""")))!!
            assertEquals("thumb", result.subjectCover)
        }
    }

    @Test fun replyUsesSourceContentAndNativeUriBeforeFallbackIds() {
        val item = JSONObject("""{"id":9,"reply_time":100,"ctime":200,
          "user":{"mid":3,"nickname":"回复者","attribute":6},
          "item":{"source_content":"新回复","root_reply_content":"层主",
            "target_reply_content":"我说过的话","subject_id":99,"root_id":1,"target_id":2,
            "native_uri":"bilibili://video/123/?comment_root_id=45&comment_secondary_id=67&t=8"}}""")
        val result = mapMessageFeedItem(MessageFeedItemDto(0, item))!!
        assertEquals("新回复", result.content)
        assertEquals("回复了我的评论", result.action)
        assertEquals("层主", result.subjectText)
        assertEquals("我说过的话", result.quote)
        assertEquals(123L, result.aid)
        assertEquals(45L, result.rootRpid)
        assertEquals(67L, result.targetRpid)
        assertEquals(67L, result.sourceRpid)
        assertEquals(8000L, result.seekMs)
        assertEquals(100L, result.time)
        assertTrue(result.followed)
    }
    @Test fun likesKeepEmptyTextAndCountsAcrossUserList() {
        val item = JSONObject("""{"id":8,"users":[{"uname":"甲","mid":1},{"nickname":"乙"}],
          "counts":1,"item":{"type":"archive","cover":"cover","counts":5}}""")
        val result = mapMessageFeedItem(MessageFeedItemDto(2, item))!!
        assertEquals("赞了我的视频", result.action)
        assertEquals(listOf("甲", "乙"), result.likeNames)
        assertEquals(5, result.likeCount)
        assertTrue(result.videoSubject)
        assertEquals("cover", result.subjectCover)
        assertEquals("", result.content)
    }
    @Test fun emptyReplyIsDroppedButAtAndVideoCommentsKeepOriginalClassification() {
        assertNull(mapMessageFeedItem(MessageFeedItemDto(0, JSONObject("{}"))))
        val at = mapMessageFeedItem(MessageFeedItemDto(1, JSONObject("""{"content":"提及","item":{"type":"video"}}""")))!!
        assertEquals("提到了我", at.action)
        assertFalse(at.commentOnMyVideo)
        val comment = mapMessageFeedItem(MessageFeedItemDto(0, JSONObject("""{"content":"评论","item":{"business_id":1,"pic":"pic"}}""")))!!
        assertEquals("对我的视频发表了评论", comment.action)
        assertTrue(comment.commentOnMyVideo)
        assertEquals("pic", comment.subjectCover)
    }
}
