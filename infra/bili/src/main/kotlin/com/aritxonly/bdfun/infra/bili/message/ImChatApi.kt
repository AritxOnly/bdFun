package com.aritxonly.bdfun.infra.bili.message

import com.aritxonly.bdfun.infra.bili.BiliApiContext
import org.json.JSONObject

internal suspend fun BiliApiContext.imSend(talker: Long, content: String, type: Int = 1): Boolean {
    val message = JSONObject().put("sender_uid", mid).put("receiver_type", 1).put("receiver_id", talker)
        .put("msg_type", type).put("content", content).put("timestamp", System.currentTimeMillis() / 1000)
        .put("msg_status", 0).put("new_face_version", 1)
    imRequest("SendMsg", "ReqSendMsg", "RspSendMsg", JSONObject().put("msg", message)
        .put("dev_id", java.util.UUID.randomUUID().toString()), legacy = true)
    return true
}

internal suspend fun BiliApiContext.imHistory(talker: Long, before: Long?): JSONObject {
    val request = JSONObject().put("talker_id", talker).put("session_type", 1).put("size", 20).put("dev_id", "1")
    if (before != null) request.put("end_seqno", before).put("begin_seqno", 0)
    val result = imRequest("SyncFetchSessionMsgs", "ReqSessionMsg", "RspSessionMsg", request, legacy = true)
    if (!result.has("min_seqno")) {
        result.put("min_seqno", result.optJSONArray("messages").imObjects().minOfOrNull { it.optLong("msg_seqno") } ?: 0)
    }
    return result
}
