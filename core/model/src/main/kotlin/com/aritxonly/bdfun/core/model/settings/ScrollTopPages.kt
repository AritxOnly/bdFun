package com.aritxonly.bdfun.core.model.settings

/** 「回到顶部」按钮可以单独开 / 关的页面。key 同时用于设置存储和页面注册。 */
object ScrollTopPages {
    val entries = listOf(
        "home" to "首页",
        "dynamic" to "动态页",
        "space" to "个人空间",
        "library" to "收藏 / 历史 / 稍后再看",
        "message" to "消息页",
        "message_detail" to "消息详情页",
    )
    val all: Set<String> = entries.map { it.first }.toSet()
}

