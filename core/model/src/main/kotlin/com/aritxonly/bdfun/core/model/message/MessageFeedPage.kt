package com.aritxonly.bdfun.core.model.message

data class MessageFeedPage(val items: List<MessageItem>, val next: MessageCursor?)
