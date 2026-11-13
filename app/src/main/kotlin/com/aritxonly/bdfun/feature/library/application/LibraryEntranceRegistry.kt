package com.aritxonly.bdfun.feature.library.application

/** 保留进程内跨媒体库页面的一次入场；4096 项上限替代旧无上限静态集合，超出后旧卡片可重播。 */
internal class LibraryEntranceRegistry(private val capacity: Int = 4096) {
    init { require(capacity > 0) }
    private val seen = LinkedHashSet<String>()
    fun mark(key: String): Boolean {
        val added = seen.add(key)
        while (seen.size > capacity) seen.remove(seen.first())
        return added
    }
}
