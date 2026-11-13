package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.MessageManagementRepository
import com.aritxonly.bdfun.core.model.message.*
import com.aritxonly.bdfun.infra.bili.message.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class BiliMessageManagementRepository(private val backend: BiliBackend) : MessageManagementRepository {
    override suspend fun contacts(group: Int, query: String, page: Int) = biliOutcome {
        val result = if (group < 2 && query.isBlank()) backend.relationUsers(backend.mid, group == 1, page)
        else {
            val data = backend.api.dataRequest(if (group == 2) "/x/relation/blacks" else "/x/relation/followings/search",
                mapOf("vmid" to backend.mid.toString(), "pn" to page.toString(), "ps" to "20", "name" to query,
                    "order" to "desc", "order_type" to "attention", "gaia_source" to "main_web", "web_location" to "333.999"), signed = true)
            com.aritxonly.bdfun.infra.bili.mapper.mapRelationPage(data, null, { true }, page)
        }
        MessageContactsPage(result.items.map { MessageLikePerson(it.mid, it.name, it.face, 0) }, result.nextPage != null)
    }
    override suspend fun chatPreferences(mid: Long) = biliOutcome { backend.api.imChatPreferences(mid) }
    override suspend fun setChatPreference(mid: Long, action: com.aritxonly.bdfun.core.domain.ChatPreferenceAction, enabled: Boolean) = biliOutcome {
        backend.api.imSetChatPreference(mid, action, enabled); Unit
    }
    override suspend fun likes(id: String, page: Int, lastMid: Long) = biliOutcome {
        withContext(Dispatchers.IO) {
            val data = backend.api.dataRequest("/x/msgfeed/like_detail", mapOf("card_id" to id, "pn" to page.toString(),
                "last_mid" to lastMid.toString(), "platform" to "web", "build" to "0", "mobi_app" to "web", "web_location" to "333.40164"))
            mapLikeDetail(data)
        }
    }
    override suspend fun settings(type: Int) = biliOutcome {
        mapImSettings(backend.api.imRequest("GetImSettings", "GetImSettingsReq", "GetImSettingsReply", JSONObject().put("type", type)))
    }
    override suspend fun setSetting(setting: ImSetting, enabled: Boolean?, choice: Int?) = biliOutcome {
        backend.api.imRequest("SetImSettings", "SetImSettingsReq", "SetImSettingsReply", settingUpdate(setting, enabled, choice)); Unit
    }
    override suspend fun keywords() = biliOutcome {
        val data = backend.api.imRequest("KeywordBlockingList", "KeywordBlockingListReq", "KeywordBlockingListReply")
        MessageKeywordList(data.optJSONArray("items").imObjects().map { it.optString("keyword") },
            data.optInt("list_limit"), data.optInt("char_limit"), data.optString("list_limit_text"))
    }
    override suspend fun addKeyword(keyword: String) = biliOutcome {
        backend.api.imRequest("KeywordBlockingAdd", "KeywordBlockingAddReq", "KeywordBlockingAddReply", JSONObject().put("keyword", keyword)); Unit
    }
    override suspend fun deleteKeyword(keyword: String) = biliOutcome {
        backend.api.imRequest("KeywordBlockingDelete", "KeywordBlockingDeleteReq", "KeywordBlockingDeleteReply", JSONObject().put("keyword", keyword)); Unit
    }
}

internal fun mapLikeDetail(data: JSONObject): MessageLikeDetail {
    val card = data.optJSONObject("card")
    return MessageLikeDetail(listOf(card?.optString("business").orEmpty(), card?.optString("title").orEmpty())
        .filter { it.isNotBlank() }.joinToString(": "), data.optJSONArray("items").imObjects().mapNotNull { value ->
        val user = value.optJSONObject("user") ?: return@mapNotNull null
        MessageLikePerson(user.optLong("mid"), user.optString("nickname"), user.optString("avatar"), value.optLong("like_time"))
    })
}
