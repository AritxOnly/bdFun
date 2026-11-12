package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.model.video.ResolvedVideoLink
import org.junit.Assert.*
import org.junit.Test

class RichTextPolicyTest {
    private val bv = "BV1orW46aEba"
    @Test fun plainPathFlagsMatchTheOriginalEvenForAnUnknownEmote() {
        assertTrue(analyzeRichText("正文", emptyMap(), emptyMap(), false).plainText(false))
        assertFalse(analyzeRichText("正文", emptyMap(), emptyMap(), false).plainText(true))
        val unknown = analyzeRichText("[unknown]", mapOf("other" to "image"), emptyMap(), false)
        assertTrue(unknown.hasEmote); assertTrue(unknown.hits.isEmpty()); assertFalse(unknown.plainText(false))
    }
    @Test fun urlWinsOverEmotesAndMentionsAndOnlyItsTrailingPunctuationIsExcluded() {
        val text = "https://example.test/@小明[doge]，"
        val plan = analyzeRichText(text, mapOf("doge" to "image"), mapOf("小明" to 9), true)
        assertEquals(1, plan.hits.size)
        assertEquals("https://example.test/@小明[doge", plan.hits.single().linkUrl)
        assertEquals(plan.hits.single().linkUrl!!.length, plan.hits.single().end)
        assertTrue(plan.hasEmote); assertTrue(plan.hasMention)
    }
    @Test fun emotesWinOverContainedMentionsAndTokenKeysWinOverBracketedKeys() {
        val plan = analyzeRichText("[@甲]", mapOf("@甲" to "first", "[@甲]" to "second"), mapOf("甲" to 1), true)
        assertEquals(1, plan.hits.size); assertEquals("first", plan.hits.single().emoteUrl)
        assertNull(plan.hits.single().mentionName)
    }
    @Test fun mentionsPreferLongNamesEscapeRegexCharactersAndRetainZeroMid() {
        val names = linkedMapOf("小" to 1L, "小明" to 2L, "A+B" to 0L)
        val plan = analyzeRichText("@小明 @A+B", emptyMap(), names, true)
        assertEquals(listOf("小明", "A+B"), plan.hits.map { it.mentionName })
        assertEquals(listOf(2L, 0L), plan.hits.map { it.mentionMid })
        assertTrue(analyzeRichText("@小明", emptyMap(), names, false).hits.isEmpty())
        assertFalse(analyzeRichText("@小明", emptyMap(), names, false).hasMention)
    }
    @Test fun repeatedBareIdUsesOnlyFirstLiteralOccurrenceWhileUrlsRetainEveryMatch() {
        val plan = analyzeRichText("x$bv $bv $bv", emptyMap(), emptyMap(), false)
        assertEquals(1, plan.hits.size); assertEquals(1, plan.hits.single().start)
        assertEquals(2, collectRichLinks("https://x.test/one https://x.test/one").size)
    }
    @Test fun offsetsRemainUtf16AndHitsStayInSourceOrder() {
        val plan = analyzeRichText("🙂 $bv [doge] @甲", mapOf("doge" to "image"), mapOf("甲" to 1), true)
        assertEquals(3, plan.hits.first().start)
        assertEquals(listOf(bv, null, null), plan.hits.map { it.linkUrl })
        assertEquals(listOf(null, "doge", null), plan.hits.map { it.emoteToken })
        assertEquals("甲", plan.hits.last().mentionName)
    }
    @Test fun emoteLengthAndGenericUrlCaseStayUnchanged() {
        assertTrue(analyzeRichText("[abcdefghijklm]", mapOf("abcdefghijklm" to "image"), emptyMap(), false).hits.isEmpty())
        assertEquals("image", analyzeRichText("[abcdefghijkl]", mapOf("[abcdefghijkl]" to "image"), emptyMap(), false).hits.single().emoteUrl)
        assertTrue(analyzeRichText("HTTP://example.test", emptyMap(), emptyMap(), false).plainText(false))
        val short = analyzeRichText("http://a", emptyMap(), emptyMap(), false)
        assertTrue(short.hasUrl); assertTrue(short.hits.isEmpty()); assertFalse(short.plainText(false))
    }
    @Test fun titleAndClickFallbacksKeepRawTextUntilMetadataArrives() {
        assertEquals(bv, VideoLinkText.display(bv, emptyMap()))
        assertEquals(bv, VideoLinkText.clickBvid("bv" + bv.substring(2), emptyMap()))
        assertNull(VideoLinkText.clickBvid("av123", emptyMap()))
        assertNull(VideoLinkText.clickBvid("https://b23.tv/a", emptyMap()))
        val resolved = mapOf("av123" to ResolvedVideoLink(bv, "标题"))
        assertEquals("标题", VideoLinkText.display("av123", resolved)); assertEquals(bv, VideoLinkText.clickBvid("av123", resolved))
    }
}
