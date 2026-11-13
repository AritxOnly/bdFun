package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.infra.bili.mapper.mapReplyEmotes
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ReplyEmoteMapperTest {
    // 按 PiliPlus 的 Package/Emote/Meta 契约构造；匿名真实请求 packages 为 null。
    @Test fun imagesKeepOriginalAspectAndTextPackagesNeedNoImage() {
        val data = JSONObject("""{"packages":[
            {"type":1,"url":"//i.example/icon.png","emote":[
                {"text":"[doge]","url":"//i.example/doge.png","meta":{"size":2}}]},
            {"type":4,"emote":[{"text":"(笑)","url":""}]}
        ]}""")
        val items = mapReplyEmotes(data)
        assertEquals(2, items.size)
        assertEquals("https://i.example/doge.png", items[0].url)
        assertFalse(items[0].url.contains("@"))
        assertEquals(2, items[0].size)
        assertEquals("https://i.example/icon.png", items[0].packageIcon)
        assertEquals(1, items[1].packageId)
        assertEquals(4, items[1].type)
        assertEquals("(笑)", items[1].text)
    }
    @Test fun anonymousEmptyResponseIsAccepted() {
        assertTrue(mapReplyEmotes(JSONObject("""{"packages":null}""")).isEmpty())
    }
}
