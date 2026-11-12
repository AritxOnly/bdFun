package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.model.cache.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class VideoCacheCodecTest {
    private val legacy = """[{"id":"BV1234567890_7_80","bvid":"BV1234567890","aid":3,"cid":7,"title":"标题","cover":"https://cover","coverPath":"/cover.jpg","ownerName":"UP","ownerMid":5,"durationSec":12,"qualityId":80,"qualityLabel":"1080P","videoPath":"/video.m4s","audioPath":"/audio.m4s","progress":1.0,"bytes":120,"totalBytes":120,"status":"Completed","createdAt":100,"error":""}]"""
    @Test fun legacyFlatRecordRoundTripsAllTwentyFields() {
        val entry = decodeVideoCache(legacy) { true }.single()
        assertEquals(CacheVideoInfo("BV1234567890", 3, 7, "标题", "https://cover", "UP", 5, 12), entry.video)
        assertEquals(CacheVideoFiles(80, "1080P", "/video.m4s", "/audio.m4s", "/cover.jpg"), entry.files)
        assertEquals(CacheTransferState(1f, 120, 120, CacheStatus.Completed), entry.transfer)
        assertEquals(100L, entry.createdAt); assertNull(entry.error)
        val before = JSONArray(legacy).getJSONObject(0); val after = JSONArray(encodeVideoCache(listOf(entry))).getJSONObject(0)
        assertEquals(20, after.length()); assertEquals(before.keys().asSequence().toSet(), after.keys().asSequence().toSet())
        for (key in before.keys().asSequence().toSet()) {
            // org.json 编码会把 1.0 规范化为 1；比较原格式的数值语义，不要求数字字面量不变。
            if (before.get(key) is Number) assertEquals(key, before.getDouble(key), after.getDouble(key), 0.0)
            else assertEquals(key, before.get(key).toString(), after.get(key).toString())
        }
    }
    @Test fun interruptedRowsBecomeFailedWithoutLosingRecordedProgress() {
        for (status in listOf("Queued", "Downloading")) {
            val entry = decodeVideoCache(legacy.replace("Completed", status).replace("\"progress\":1.0", "\"progress\":0.5")) { true }.single()
            assertEquals(CacheStatus.Failed, entry.status); assertEquals("下载已中断", entry.error)
            assertEquals(0.5f, entry.progress); assertEquals(120L, entry.bytes)
        }
    }
    @Test fun missingVideoOrRequiredAudioFailsButBlankAudioIsAllowed() {
        for (missing in listOf("/video.m4s", "/audio.m4s")) {
            val entry = decodeVideoCache(legacy) { it != missing }.single()
            assertEquals(CacheStatus.Failed, entry.status); assertEquals("缓存文件已丢失", entry.error)
        }
        val noAudio = legacy.replace("\"audioPath\":\"/audio.m4s\"", "\"audioPath\":\"\"")
        assertEquals(CacheStatus.Completed, decodeVideoCache(noAudio) { it == "/video.m4s" }.single().status)
    }
    @Test fun malformedDataUnknownStatusBlankIdAndNonObjectsKeepLegacyFallbacks() {
        assertTrue(decodeVideoCache("bad") { true }.isEmpty())
        assertTrue(decodeVideoCache("[null,1,{},false]") { true }.isEmpty())
        val entry = decodeVideoCache("""[{"id":"x","status":"Unknown","error":" "}]""") { true }.single()
        assertEquals(CacheStatus.Failed, entry.status); assertNull(entry.error)
        assertEquals(0f, entry.progress); assertEquals("", entry.title)
    }
    @Test fun errorMessagesAndRecordOrderRemainUnchanged() {
        val first = decodeVideoCache(legacy) { true }.single().copy(error = "下载失败：空响应")
        val second = first.copy(id = "second", transfer = first.transfer.copy(status = CacheStatus.Failed))
        val encoded = encodeVideoCache(listOf(first, second))
        assertEquals(listOf(first, second), decodeVideoCache(encoded) { true })
        assertEquals("下载失败：空响应", JSONObject(JSONArray(encoded).getJSONObject(0).toString()).getString("error"))
    }
}
