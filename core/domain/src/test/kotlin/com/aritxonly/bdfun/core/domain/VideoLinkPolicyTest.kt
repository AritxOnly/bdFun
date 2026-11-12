package com.aritxonly.bdfun.core.domain

import org.junit.Assert.*
import org.junit.Test

class VideoLinkPolicyTest {
    private val bv = "BV1orW46aEba"
    @Test fun tokensKeepOrderDistinctnessAndTrimTheOriginalPunctuation() {
        val url = "https://www.bilibili.com/video/$bv?p=2"
        assertEquals(listOf(bv, "av123", url, "https://b23.tv/Ab12"),
            VideoLinkPolicy.tokens("$bv $bv av123 $url）。 https://b23.tv/Ab12，"))
    }
    @Test fun bareIdsKeepAsciiBoundaryAndMinimumAidLength() {
        assertTrue(VideoLinkPolicy.tokens("x$bv 1$bv av12 xav123").isEmpty())
        assertEquals(listOf(bv, "av123"), VideoLinkPolicy.tokens("_$bv（av123）"))
        assertTrue(VideoLinkPolicy.isToken("https://b23.tv/Ab12"))
        assertFalse(VideoLinkPolicy.isToken("https://b23.tv/Ab12?x=1"))
    }
    @Test fun titleTargetsPreserveTheDifferentBareAndUrlRules() {
        assertEquals(VideoTitleTarget.Bvid(bv), VideoLinkPolicy.titleTarget("bv" + bv.substring(2)))
        assertEquals(VideoTitleTarget.Bvid("bv" + bv.substring(2)), VideoLinkPolicy.titleTarget("https://www.bilibili.com/video/bv" + bv.substring(2)))
        assertEquals(VideoTitleTarget.Aid(123), VideoLinkPolicy.titleTarget("AV123"))
        assertEquals(VideoTitleTarget.Unsupported, VideoLinkPolicy.titleTarget("https://www.bilibili.com/video/av123"))
        assertEquals(VideoTitleTarget.Unsupported, VideoLinkPolicy.titleTarget("HTTPS://B23.TV/ABC"))
        assertEquals(VideoTitleTarget.Unsupported, VideoLinkPolicy.titleTarget("av999999999999999999999999"))
    }
    @Test fun activityRecognitionDoesNotPretendToSupportBareAidResolution() {
        assertTrue(VideoLinkPolicy.recognizes("x$bv"))
        assertFalse(VideoLinkPolicy.recognizes("av123"))
        assertTrue(VideoLinkPolicy.recognizes("https://www.bilibili.com/video/av123"))
        assertNull(VideoLinkPolicy.directBvid("av123"))
        assertEquals(bv, VideoLinkPolicy.directBvid("before bv" + bv.substring(2) + " after"))
        assertEquals("HTTPS://B23.TV/Ab1", VideoLinkPolicy.shortUrl("url HTTPS://B23.TV/Ab1!"))
    }
}
