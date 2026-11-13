package com.aritxonly.bdfun.feature.message.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.MessageManagementRepository
import com.aritxonly.bdfun.core.model.message.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal sealed interface MessageTool {
    data class Likes(val id: String, val count: Int) : MessageTool
    data class Settings(val type: Int) : MessageTool
    data class Contacts(val group: Int, val query: String) : MessageTool
    data class Chat(val mid: Long) : MessageTool
    data object Keywords : MessageTool
}
internal data class MessageToolsUiState(
    val title: String = "", val loading: Boolean = false, val busy: Boolean = false, val error: String? = null,
    val people: List<MessageLikePerson> = emptyList(), val settings: List<ImSetting> = emptyList(),
    val chat: ChatPreferences? = null,
    val keywords: MessageKeywordList? = null, val more: Boolean = false, val draft: String = "", val retryMore: Boolean = false,
)

internal class MessageToolsStateHolder(
    private val repository: MessageManagementRepository, val tool: MessageTool,
    private val isCurrentOwner: () -> Boolean = { true },
) {
    private val mutable = MutableStateFlow(MessageToolsUiState())
    val state = mutable.asStateFlow()
    private var closed = false
    private var page = 0
    private fun current() = !closed && isCurrentOwner()
    fun close() { closed = true }
    fun draft(value: String) { if (current()) mutable.update { it.copy(draft = value) } }

    suspend fun load(more: Boolean = false) {
        val initial = mutable.value
        if (!current() || initial.loading || initial.busy || more && !initial.more) return
        mutable.update { it.copy(loading = true, error = null, retryMore = more) }
        try {
            when (val target = tool) {
                is MessageTool.Likes -> {
                    val nextPage = if (more) page + 1 else 1
                    val result = repository.likes(target.id, nextPage, if (more) initial.people.lastOrNull()?.mid ?: 0 else 0)
                    if (!current()) return
                    when (result) {
                        is Outcome.Success -> {
                            val people = ((if (more) initial.people else emptyList()) + result.value.people).distinctBy { it.mid }
                            page = nextPage
                            mutable.update { it.copy(title = result.value.title.ifBlank { it.title }, people = people,
                                more = people.size < target.count && result.value.people.isNotEmpty() && (!more || people.size > initial.people.size)) }
                        }
                        is Outcome.Failure -> failed(result)
                    }
                }
                is MessageTool.Contacts -> {
                    val nextPage = if (more) page + 1 else 1
                    when (val result = repository.contacts(target.group, target.query, nextPage)) {
                        is Outcome.Success -> if (current()) {
                            page = nextPage
                            val people = ((if (more) initial.people else emptyList()) + result.value.people).distinctBy { it.mid }
                            mutable.update { it.copy(people = people, more = result.value.more && (!more || people.size > initial.people.size)) }
                        }
                        is Outcome.Failure -> failed(result)
                    }
                }
                is MessageTool.Chat -> when (val result = repository.chatPreferences(target.mid)) {
                    is Outcome.Success -> if (current()) mutable.update { it.copy(chat = result.value) }
                    is Outcome.Failure -> failed(result)
                }
                is MessageTool.Settings -> when (val result = repository.settings(target.type)) {
                    is Outcome.Success -> if (current()) mutable.update { it.copy(title = result.value.title, settings = result.value.settings) }
                    is Outcome.Failure -> failed(result)
                }
                MessageTool.Keywords -> when (val result = repository.keywords()) {
                    is Outcome.Success -> if (current()) mutable.update { it.copy(title = "消息屏蔽词", keywords = result.value) }
                    is Outcome.Failure -> failed(result)
                }
            }
        } finally { if (current()) mutable.update { it.copy(loading = false) } }
    }

    suspend fun chatPreference(mid: Long, action: com.aritxonly.bdfun.core.domain.ChatPreferenceAction, enabled: Boolean): String? =
        mutate { repository.setChatPreference(mid, action, enabled) }
    suspend fun setting(setting: ImSetting, enabled: Boolean? = null, choice: Int? = null): String? {
        return mutate { repository.setSetting(setting, enabled, choice) }
    }
    suspend fun addKeyword(): String? {
        val initial = mutable.value
        if (!current() || initial.busy || initial.loading) return "请等待当前操作完成"
        val keyword = initial.draft.trim()
        val limits = initial.keywords ?: return null
        if (keyword.isEmpty()) return "请输入屏蔽词"
        if (limits.charLimit > 0 && keyword.codePointCount(0, keyword.length) > limits.charLimit) return "屏蔽词最多 ${limits.charLimit} 字"
        if (keyword in limits.keywords) return "该屏蔽词已存在"
        if (limits.listLimit > 0 && limits.keywords.size >= limits.listLimit) return limits.limitText.ifBlank { "屏蔽词数量已达上限" }
        return mutate { repository.addKeyword(keyword) }.also { message ->
            if (message == null && current()) mutable.update { if (it.draft == initial.draft) it.copy(draft = "") else it }
        }
    }
    suspend fun deleteKeyword(value: String): String? = mutate { repository.deleteKeyword(value) }

    private suspend fun mutate(operation: suspend () -> Outcome<Unit>): String? {
        if (!current() || mutable.value.busy || mutable.value.loading) return null
        mutable.update { it.copy(busy = true) }
        val result = try { operation() } finally { if (current()) mutable.update { it.copy(busy = false) } }
        if (!current()) return null
        return when (result) {
            is Outcome.Success -> { load(); null }
            is Outcome.Failure -> result.error.messageOr("操作失败")
        }
    }
    private fun failed(result: Outcome.Failure) {
        if (current()) mutable.update { it.copy(error = result.error.messageOr("消息加载失败")) }
    }
}
