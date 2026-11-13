package com.aritxonly.bdfun.infra.bili.message

import com.aritxonly.bdfun.core.domain.ChatPreferenceAction
import com.aritxonly.bdfun.core.model.message.ChatPreferences
import com.aritxonly.bdfun.infra.bili.*
import org.json.JSONObject

internal fun privateIdentity(mid: Long) = JSONObject().put("private_id", JSONObject().put("talker_uid", mid))

internal suspend fun BiliApiContext.imChatPreferences(talker: Long): ChatPreferences {
    val ss = dataRequest("/link_setting/v1/link_setting/get_session_ss", mapOf("talker_uid" to talker.toString()), base = BILI_VC)
    val dnd = dataRequest("/link_setting/v1/link_setting/get_msg_dnd", mapOf("own_uid" to mid.toString(), "uids_str" to talker.toString()), base = BILI_VC)
    val pinned = imRequest("SessionUpdate", "SessionUpdateReq", "SessionUpdateReply", JSONObject().put("session_id", privateIdentity(talker)))
        .optJSONObject("session")?.optBoolean("is_pinned") ?: false
    val user = rawRequest("/x/im/user_infos", mapOf("uids" to talker.toString()), base = BILI_VC)
        .optJSONArray("data")?.optJSONObject(0)
    return ChatPreferences(user?.optString("name").orEmpty(), user?.optString("face").orEmpty(), pinned,
        dnd.optJSONArray("uid_settings")?.optJSONObject(0)?.optInt("setting") == 1,
        if (ss.optInt("show_push_setting") == 1) ss.optInt("push_setting") == 0 else null,
        ss.optInt("follow_status") == 128)
}

internal suspend fun BiliApiContext.imSetChatPreference(talker: Long, action: ChatPreferenceAction, enabled: Boolean) {
    val params = mapOf("csrf" to csrf, "csrf_token" to csrf, "build" to "0", "mobi_app" to "web")
    when (action) {
        ChatPreferenceAction.Pin -> {
            val method = if (enabled) "PinSession" else "UnpinSession"
            val type = if (enabled) "PinSession" else "UnPinSession"
            imRequest(method, type + "Req", type + "Reply", JSONObject().put("session_id", privateIdentity(talker)))
        }
        ChatPreferenceAction.Mute -> dataRequest("/link_setting/v1/link_setting/set_msg_dnd", params + mapOf(
            "uid" to mid.toString(), "setting" to if (enabled) "1" else "0", "dnd_uid" to talker.toString()), base = BILI_VC, method = "POST")
        ChatPreferenceAction.Push -> dataRequest("/link_setting/v1/link_setting/set_push_ss", params + mapOf(
            "setting" to if (enabled) "0" else "1", "talker_uid" to talker.toString()), base = BILI_VC, method = "POST")
        ChatPreferenceAction.Block -> dataRequest("/x/relation/modify", params + mapOf(
            "fid" to talker.toString(), "act" to if (enabled) "5" else "6", "re_src" to "11"), method = "POST")
    }
}
