package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.VideoDetailOwner

import com.aritxonly.bdfun.core.model.video.VideoDetailInfo

import com.aritxonly.bdfun.core.model.video.VideoDetailCounts

import com.aritxonly.bdfun.core.model.video.*
import org.junit.Assert.*
import org.junit.Test

class VideoPagePolicyTest {
    private val first = VideoPage(1, 1, "第一P", 10)
    private val second = VideoPage(2, 2, "第二P", 20)
    private val detail = VideoDetail(
        bvid = "one",
        aid = 42,
        title = "标题",
        cover = "",
        desc = "简介",
        owner = VideoDetailOwner(
            ownerName = "UP",
            ownerFace = "",
            ownerMid = 7,
            ownerSign = "",
        ),
        counts = VideoDetailCounts(
            play = 0,
            danmaku = 0,
            like = 0,
            coin = 0,
            favorite = 0,
            share = 0,
            reply = 0,
        ),
        info = VideoDetailInfo(
            pubdate = 0,
            duration = 10,
            cid = 1,
            tname = "分区",
            pages = listOf(first),
        ),
    )
    private fun episode(cid: Long) = VideoEpisode("BV$cid", cid, cid, "集$cid", "", 30)
    @Test fun multiPartKeepsOriginalPartsAndDeduplicatesCollectionByCid() {
        val pages = VideoPagePolicy.pages(detail.copy(
            info = detail.info.copy(
                pages = listOf(first, second),
                episodes = listOf(episode(2), episode(3)),
            ),
        ))
        assertEquals(listOf(1L, 2L, 3L), pages.map { it.cid })
        assertEquals(second, pages[1])
    }
    @Test fun collectionTakesPriorityForSinglePartVideo() {
        val pages = VideoPagePolicy.pages(detail.copy(
            info = detail.info.copy(
                episodes = listOf(episode(1), episode(3)),
            ),
        ))
        assertEquals(listOf("集1", "集3"), pages.map { it.part })
        assertEquals(listOf(1, 2), pages.map { it.page })
    }
    @Test fun singleCollectionEntryDoesNotReplaceOriginalPart() {
        assertEquals(listOf(first), VideoPagePolicy.pages(detail.copy(
            info = detail.info.copy(
                episodes = listOf(episode(9)),
            ),
        )))
    }
}
