package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.message.*

interface MessageManagementRepository {
    suspend fun contacts(group: Int, query: String, page: Int): Outcome<MessageContactsPage>
    suspend fun chatPreferences(mid: Long): Outcome<ChatPreferences>
    suspend fun setChatPreference(mid: Long, action: ChatPreferenceAction, enabled: Boolean): Outcome<Unit>
    suspend fun likes(id: String, page: Int, lastMid: Long): Outcome<MessageLikeDetail>
    suspend fun settings(type: Int): Outcome<ImSettingsPage>
    suspend fun setSetting(setting: ImSetting, enabled: Boolean? = null, choice: Int? = null): Outcome<Unit>
    suspend fun keywords(): Outcome<MessageKeywordList>
    suspend fun addKeyword(keyword: String): Outcome<Unit>
    suspend fun deleteKeyword(keyword: String): Outcome<Unit>
}

enum class ChatPreferenceAction { Pin, Mute, Push, Block }
