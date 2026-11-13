package com.aritxonly.bdfun.infra.bili.message

import com.aritxonly.bdfun.infra.bili.mapper.mapPrivateMessagePage
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ImMappingTest {
    @Test fun foldedInboxPreservesIdentityCursorAndServerMenus() {
        val data = JSONObject("""{"pagination_params":{"has_more":true,"offsets":[{"key":1,"value":{"normal_offset":42}}]},
            "sessions":[{"id":{"fold_id":{"type":4}},"session_info":{"session_name":"未关注人"},"timestamp":42000000}],
            "three_dot_items":[{"type":9,"title":"清空列表"}]}""")
        val page = mapImInbox(data, null)
        assertEquals(2, page.sessions.single().preferences.pageType)
        assertEquals(42L, page.sessions.single().time)
        assertEquals(9, page.menu.single().type)
        assertNotNull(page.cursor)
        assertNull(mapImInbox(data, page.cursor).cursor)
    }
    @Test fun zeroValuedRadioChoiceAndSwitchOffAreKeptInUpdate() {
        val page = mapImSettings(JSONObject("""{"settings":[{"key":19,"value":{"select":{"item":[{"item_type":0,"text":"全部","selected":true},{"item_type":1,"text":"关注","selected":false}]}}}]}"""))
        val update = settingUpdate(page.settings.single(), null, 1)
        val items = update.getJSONArray("settings").getJSONObject(0).getJSONObject("value").getJSONObject("select").getJSONArray("item")
        assertFalse(items.getJSONObject(0).getBoolean("selected")); assertTrue(items.getJSONObject(1).getBoolean("selected"))
    }
    @Test fun withdrawalCommandsAreHiddenWhileCardLinksAndChatEmotesAreRetained() {
        val result = mapPrivateMessagePage(JSONObject("""{"messages":[
            {"msg_key":1,"msg_seqno":1,"msg_type":5,"content":"9"},
            {"msg_key":2,"msg_seqno":2,"msg_type":10,"content":"{\"title\":\"通知\",\"jump_uri\":\"https://example.com\",\"jump_uri_2\":\"https://example.com/2\"}"}],
            "e_infos":[{"text":"[表情]","url":"still","gif_url":"animated"}]}"""), 1, null)
        assertEquals(1, result.items.size)
        assertEquals(2, result.items.single().card!!.children.size)
        assertEquals("animated", result.emotes["[表情]"])
    }
}
