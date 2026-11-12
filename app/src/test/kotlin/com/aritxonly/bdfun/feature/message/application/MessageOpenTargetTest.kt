package com.aritxonly.bdfun.feature.message.application

import com.aritxonly.bdfun.core.model.message.MessageItem
import org.junit.Assert.*
import org.junit.Test

class MessageOpenTargetTest {
    private val item = MessageItem("one", "标题", "正文", 1, "姓名", "", aid = 1)

    @Test fun ordinaryVideoAidOverridesBusinessIdIncludingUpperBoundary() {
        for (aid in listOf(1L, 9_999_999_999L)) {
            assertEquals(MessageOpenTarget.Video(0, 0), messageOpenTarget(item.copy(aid = aid, businessId = 17), MessageFeedKind.Reply))
        }
        assertEquals(MessageOpenTarget.Dynamic, messageOpenTarget(item.copy(aid = 10_000_000_000, businessId = 17), MessageFeedKind.Reply))
    }

    @Test fun nonVideoBusinessUsesDynamicEvenWithBvid() {
        assertEquals(MessageOpenTarget.Dynamic, messageOpenTarget(item.copy(aid = 0, bvid = "BV1", businessId = 2), MessageFeedKind.Like))
    }

    @Test fun videoBusinessAcceptsBvidAndRejectsMissingBothIdentifiers() {
        for (business in listOf(0, 1)) {
            assertEquals(MessageOpenTarget.MissingVideo, messageOpenTarget(item.copy(aid = 0, businessId = business), MessageFeedKind.Reply))
            assertEquals(MessageOpenTarget.Video(0, 0), messageOpenTarget(item.copy(aid = 0, bvid = "BV1", businessId = business), MessageFeedKind.Reply))
        }
    }

    @Test fun replyFocusPrefersTargetThenSourceThenRoot() {
        val reply = item.copy(rootRpid = 11, targetRpid = 22, sourceRpid = 33)
        assertEquals(MessageOpenTarget.Video(11, 22), messageOpenTarget(reply, MessageFeedKind.Reply))
        assertEquals(MessageOpenTarget.Video(11, 33), messageOpenTarget(reply.copy(targetRpid = 0), MessageFeedKind.Reply))
        assertEquals(MessageOpenTarget.Video(11, 11), messageOpenTarget(reply.copy(targetRpid = 0, sourceRpid = 0), MessageFeedKind.Reply))
    }

    @Test fun likeFocusKeepsOriginalSourceFallbackWithoutRootFallback() {
        assertEquals(MessageOpenTarget.Video(11, 0), messageOpenTarget(item.copy(rootRpid = 11), MessageFeedKind.Like))
        assertEquals(MessageOpenTarget.Video(22, 22), messageOpenTarget(item.copy(targetRpid = 22, sourceRpid = 33), MessageFeedKind.Like))
        assertEquals(MessageOpenTarget.Video(33, 33), messageOpenTarget(item.copy(sourceRpid = 33), MessageFeedKind.Like))
    }
}
