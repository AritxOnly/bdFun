package com.aritxonly.bdfun.infra.bili

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PrivateChatPayloadTest {
    @Test fun existingUploadFieldsKeepDimensionsAndDefaultSizeInSendPayload() {
        val image = decodeChatUploadedImage("""{"img_src":"https://image","img_width":120,"img_height":80}""")
        val sent = JSONObject(encodeChatImage(image))
        assertEquals("https://image", sent.getString("url"))
        assertEquals(120, sent.getInt("width")); assertEquals(80, sent.getInt("height"))
        assertEquals(0.0, sent.getDouble("size"), 0.0); assertEquals(1, sent.getInt("original"))
    }
    @Test fun blankUploadSourceKeepsTheOriginalFailure() {
        try { decodeChatUploadedImage("""{"img_src":" "}"""); fail() }
        catch (failure: IllegalStateException) { assertEquals("图片上传失败", failure.message) }
    }
    @Test fun sharedVideoReadsTheOriginalViewFieldAndDefaultTitle() {
        val value = decodeSharedVideo(""" {"bvid":"BVtest","title":"","cover":"cover","view":12,"danmaku":3,"desc":"desc"} """)!!
        assertEquals("BVtest", value.bvid); assertEquals("视频", value.title)
        assertEquals(12L, value.play); assertEquals(3L, value.danmaku)
        assertEquals("cover", value.cover); assertEquals("desc", value.desc)
    }
    @Test fun ordinaryMalformedAndNonVideoTextHaveNoSharedCard() {
        for (text in listOf("plain", "{", "{}", """{"bvid":""}""")) assertNull(decodeSharedVideo(text))
    }
}
