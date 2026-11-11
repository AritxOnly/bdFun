package com.aritxonly.bdfun.core.domain

import org.junit.Assert.*
import org.junit.Test

class PlaybackPolicyTest {
    private data class Track(val id: Int, val codec: String, val bandwidth: Long)
    private fun video(items: List<Track>, preference: String = "auto") =
        PlaybackPolicy.chooseVideo(items, preference, Track::codec, Track::bandwidth)

    @Test fun autoPrefersAvcOverLargerAv1() {
        val avc = Track(80, "avc1", 100)
        assertSame(avc, video(listOf(Track(80, "av01", 900), avc)))
    }
    @Test fun autoUsesHighestBandwidthWithinCompatibleCodec() {
        val best = Track(80, "avc1", 200)
        assertSame(best, video(listOf(Track(80, "avc1", 100), best)))
    }
    @Test fun explicitHevcSelectsHighestMatchingRepresentation() {
        val best = Track(80, "hvc1", 500)
        assertSame(best, video(listOf(Track(80, "avc1", 900), Track(80, "hev1", 200), best), "hevc"))
    }
    @Test fun unavailablePreferenceFallsBackToAvc() {
        val avc = Track(80, "avc1", 100)
        assertSame(avc, video(listOf(Track(80, "av01", 900), avc), "hevc"))
    }
    @Test fun hevcBeatsAv1WhenAvcIsAbsent() {
        val hevc = Track(120, "hvc1", 100)
        assertSame(hevc, video(listOf(Track(120, "av01", 900), hevc)))
    }
    @Test fun standardAudioIdWinsOverExoticHigherBandwidth() {
        val standard = Track(30280, "", 100)
        val selected = PlaybackPolicy.chooseAudio(listOf(Track(30251, "", 999), Track(30232, "", 90), standard), Track::id, Track::bandwidth)
        assertSame(standard, selected)
    }
    @Test fun unknownAudioFallsBackToSmallestBandwidthAndEmptyListsStayEmpty() {
        val small = Track(1, "", 10)
        assertSame(small, PlaybackPolicy.chooseAudio(listOf(Track(2, "", 100), small), Track::id, Track::bandwidth))
        assertNull(video(emptyList()))
        assertNull(PlaybackPolicy.chooseAudio(emptyList<Track>(), Track::id, Track::bandwidth))
    }
    @Test fun codecAliasesAndDolbyKeepLegacySemantics() {
        assertEquals(0, PlaybackPolicy.codecPriority("H264"))
        assertEquals(1, PlaybackPolicy.codecPriority("h265"))
        assertEquals(2, PlaybackPolicy.codecPriority("AV01"))
        assertEquals(3, PlaybackPolicy.codecPriority("unknown"))
        assertTrue(PlaybackPolicy.codecMatches("dvhe.05", "dvh1"))
        assertFalse(PlaybackPolicy.codecMatches("h264", "avc"))
    }
}
