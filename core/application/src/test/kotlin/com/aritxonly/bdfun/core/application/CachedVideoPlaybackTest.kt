package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.model.cache.*
import org.junit.Assert.*
import org.junit.Test

class CachedVideoPlaybackTest {
    @Test fun coverChecksLocalFileEachTimeAndFallsBackToRemote() {
        val files = CacheFilesFake(); val entry = cacheEntry(); val playback = CachedVideoPlayback(files)
        assertEquals("https://cover", playback.cover(entry))
        files.sizes[entry.coverPath] = 1; assertEquals("file:///${entry.coverPath}", playback.cover(entry))
        files.sizes.remove(entry.coverPath); assertEquals("https://cover", playback.cover(entry))
    }
    @Test fun videoDetailAndDashPlaybackKeepOriginalOfflineDefaults() {
        val files = CacheFilesFake(); val entry = cacheEntry(); val playback = CachedVideoPlayback(files)
        files.sizes[entry.audioPath] = 20; files.sizes[entry.coverPath] = 1
        val video = playback.video(entry); val detail = playback.detail(entry); val info = playback.playInfo(entry)
        assertEquals(entry.bvid, video.bvid); assertEquals(7L, video.cid); assertEquals(12, video.duration)
        assertEquals(0L, video.play); assertEquals("", video.ownerFace)
        assertEquals(video.cover, detail.cover); assertEquals("title", detail.pages.single().part)
        assertEquals(1, detail.pages.single().page); assertTrue(detail.episodes.isEmpty())
        assertEquals("file:///${entry.videoPath}", info.qualities.single().videoUrl)
        assertEquals("file:///${entry.audioPath}", info.audioUrl); assertEquals(12_000L, info.durationMs)
        assertEquals(80, info.qualities.single().id); assertNull(info.fallbackUrl); assertTrue(info.acceptQuality.isEmpty())
        files.sizes.remove(entry.audioPath); assertNull(playback.playInfo(entry).audioUrl)
    }
    @Test fun findPlayableRequiresCompletedAndFilesWithOriginalCidWildcard() {
        val entry = cacheEntry(); val files = CacheFilesFake().apply { restored = listOf(entry) }
        val queue = cacheQueue(files, CacheTransferFake(files)); queue.initialize()
        assertNull(queue.findPlayable(entry.bvid, 7)); files.sizes[entry.videoPath] = 100
        assertNull(queue.findPlayable(entry.bvid, 7)); files.sizes[entry.audioPath] = 20
        assertEquals(entry, queue.findPlayable(entry.bvid, 7)); assertEquals(entry, queue.findPlayable(entry.bvid, 0))
        assertEquals(entry, queue.findPlayable(entry.bvid, -1)); assertNull(queue.findPlayable(entry.bvid, 8))
        assertNull(queue.findPlayable("other", 0)); queue.close()
        val silent = entry.copy(files = entry.files.copy(audioPath = ""))
        val silentFiles = CacheFilesFake().apply { restored = listOf(silent); sizes[silent.videoPath] = 100 }
        val silentQueue = cacheQueue(silentFiles, CacheTransferFake(silentFiles)); silentQueue.initialize()
        assertEquals(silent, silentQueue.findPlayable(entry.bvid, 7)); silentQueue.close()
    }
}
