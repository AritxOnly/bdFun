package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.PlayInfo
import com.aritxonly.bdfun.core.model.video.Quality
import org.junit.Assert.*
import org.junit.Test

class PlaybackSourcePolicyTest {
    private fun quality(video: String = "cdn", raw: String = "raw-video") = Quality(80, "", 0, 0, "", "", video, raw)
    private fun info() = PlayInfo(emptyList(), "audio", "fallback", 90_000, emptyList(), "raw-audio", "raw-fallback")

    @Test fun normalPlaybackUsesSelectedCdnAndAudioOrFallbackWhenQualityMissing() {
        assertEquals(PlaybackSourceUrls("cdn", "audio"), resolvePlaybackSourceUrls(quality(), info(), false, false))
        assertEquals(PlaybackSourceUrls("fallback", "audio"), resolvePlaybackSourceUrls(null, info(), false, false))
    }
    @Test fun originalRetryPreservesVideoAndAudioPriority() {
        val raw = info()
        assertEquals(PlaybackSourceUrls("raw-video", "raw-audio"), resolvePlaybackSourceUrls(quality(), raw, true, false))
        assertEquals(PlaybackSourceUrls("raw-fallback", "raw-audio"), resolvePlaybackSourceUrls(quality(raw = " "), raw, true, false))
        val blank = raw.copy(rawFallbackUrl = " ", rawAudioUrl = " ")
        assertEquals(PlaybackSourceUrls("cdn", "audio"), resolvePlaybackSourceUrls(quality(raw = ""), blank, true, false))
        assertEquals(PlaybackSourceUrls("fallback", "audio"), resolvePlaybackSourceUrls(null, blank, true, false))
    }
    @Test fun fallbackWinsBothFlagsAndDropsDashAudio() {
        assertEquals(PlaybackSourceUrls("fallback", null), resolvePlaybackSourceUrls(quality(), info(), true, true))
        assertNull(resolvePlaybackSourceUrls(quality(), info().copy(fallbackUrl = null), false, true))
    }
    @Test fun nullAndEmptyPrimaryUrlsKeepDifferentOriginalMeanings() {
        assertNull(resolvePlaybackSourceUrls(null, null, false, false))
        assertEquals(PlaybackSourceUrls("", "audio"), resolvePlaybackSourceUrls(quality(video = ""), info(), false, false))
        assertEquals(PlaybackSourceUrls("", null), resolvePlaybackSourceUrls(null, info().copy(fallbackUrl = ""), false, true))
    }
    @Test fun recoveryAttemptsOriginalThenFallbackThenFailure() {
        assertEquals(PlaybackRecoveryAction.RetryOriginal, selectPlaybackRecovery(quality(), info(), false, false))
        assertEquals(PlaybackRecoveryAction.RetryFallback, selectPlaybackRecovery(quality(), info(), true, false))
        assertEquals(PlaybackRecoveryAction.Fail, selectPlaybackRecovery(quality(), info(), true, true))
    }
    @Test fun audioAloneAllowsOriginalRetryButRawFallbackAloneDoesNot() {
        assertEquals(PlaybackRecoveryAction.RetryOriginal, selectPlaybackRecovery(null, info(), false, false))
        val onlyFallback = info().copy(rawAudioUrl = " ")
        assertEquals(PlaybackRecoveryAction.RetryFallback, selectPlaybackRecovery(null, onlyFallback, false, false))
        assertEquals(PlaybackRecoveryAction.Fail, selectPlaybackRecovery(quality(raw = " "), onlyFallback.copy(fallbackUrl = " "), false, false))
    }
}
