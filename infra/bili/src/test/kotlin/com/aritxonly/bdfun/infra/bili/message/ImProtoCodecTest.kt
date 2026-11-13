package com.aritxonly.bdfun.infra.bili.message

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPOutputStream

/** 固定上游描述符及独立手写 wire 样本，不依赖网络或登录凭证。 */
class ImProtoCodecTest {
    private val codec = imCodec
    private val app = "bilibili.app.im.v1."
    @Test fun compositeOffsetsUseMapEntryWireAndKeepBothPositions() {
        val sample = byteArrayOf(18, 10, 10, 8, 8, 1, 18, 4, 8, 99, 16, 100)
        val json = JSONObject("""{"pagination_params":{"offsets":[{"key":1,"value":{"normal_offset":99,"top_offset":100}}]}}""")
        assertArrayEquals(sample, codec.encode(app + "SessionMainReq", json))
        val decoded = codec.decode(app + "SessionMainReq", sample)
        val offset = decoded.getJSONObject("pagination_params").getJSONArray("offsets").getJSONObject(0).getJSONObject("value")
        assertEquals(99, offset.getInt("normal_offset")); assertEquals(100, offset.getInt("top_offset"))
    }
    @Test fun int64IsNotRoundedAndUnknownFieldsAreSkipped() {
        val out = ByteArrayOutputStream()
        out.varint(8); out.varint(9007199254740993L)
        out.varint(1600); out.varint(99)
        val value = codec.decode("bilibili.im.type.Msg", out.toByteArray())
        assertEquals(9007199254740993L, value.getLong("sender_uid"))
        assertEquals(1, value.length())
    }
    @Test fun packedRepeatedInt64AndBooleansDecode() {
        val value = codec.decode("bilibili.im.type.Msg", byteArrayOf(74, 3, 1, 2, 3, 104, 1))
        assertEquals("[1,2,3]", value.getJSONArray("at_uids").toString())
        assertTrue(value.getBoolean("sys_cancel"))
    }
    @Test fun settingsMapAndRedirectSubsettingsHaveRegisteredDescriptors() {
        val json = JSONObject("""{"settings":[{"key":4,"value":{"redirect":{"title":"提醒","setting_page":{"parent_setting_type":4,"sub_settings":[{"key":8,"value":{"switch":{"switch_on":true,"title":"接收"}}}]}}}}]}""")
        val decoded = codec.decode(app + "GetImSettingsReply", codec.encode(app + "GetImSettingsReply", json))
        assertTrue(decoded.getJSONArray("settings").getJSONObject(0).getJSONObject("value").getJSONObject("redirect")
            .getJSONObject("setting_page").getJSONArray("sub_settings").getJSONObject(0).getJSONObject("value")
            .getJSONObject("switch").getBoolean("switch_on"))
    }
    @Test fun allUsedRequestSchemasExistAndEncode() {
        listOf("SessionMainReq", "SessionSecondaryReq", "ClearUnreadReq", "PinSessionReq", "UnPinSessionReq",
            "SessionUpdateReq", "GetImSettingsReq", "SetImSettingsReq", "DeleteSessionListReq", "KeywordBlockingListReq",
            "KeywordBlockingAddReq", "KeywordBlockingDeleteReq").forEach { codec.encode(app + it, JSONObject()) }
        listOf("ReqSessionMsg", "ReqSendMsg", "ReqTotalUnread").forEach {
            codec.encode("bilibili.im.interfaces.v1.$it", JSONObject())
        }
    }
    @Test fun framingSupportsIdentityAndGzip() {
        val value = byteArrayOf(8, 1)
        val compressed = ByteArrayOutputStream().also { output -> GZIPOutputStream(output).use { it.write(value) } }.toByteArray()
        assertArrayEquals(value, decodeImFrame(ByteBuffer.allocate(7).put(0).putInt(2).put(value).array()))
        assertArrayEquals(value, decodeImFrame(ByteBuffer.allocate(5 + compressed.size).put(1).putInt(compressed.size).put(compressed).array()))
    }
    @Test fun malformedLengthsTagsAndVarintsAreRejected() {
        listOf(byteArrayOf(0), byteArrayOf(10, 8, 1), ByteArray(11) { 128.toByte() }).forEach { bytes ->
            assertThrows(IllegalArgumentException::class.java) { codec.decode(app + "SessionMainReq", bytes) }
        }
        assertThrows(IllegalArgumentException::class.java) { decodeImFrame(byteArrayOf(0, 0, 0, 0, 9)) }
    }
}
