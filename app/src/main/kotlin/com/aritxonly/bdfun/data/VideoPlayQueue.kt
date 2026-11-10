package com.aritxonly.bdfun.data

/** 收藏/历史/稍后再看打开视频时的简易播放队列，供自动连播使用。 */
object VideoPlayQueue {
    private var items: List<VideoItem> = emptyList()
    private var index: Int = -1

    fun set(list: List<VideoItem>, bvid: String) {
        items = list
        index = items.indexOfFirst { it.bvid == bvid }
    }

    fun contains(bvid: String): Boolean = items.any { it.bvid == bvid }

    fun hasNext(): Boolean = index >= 0 && index + 1 < items.size

    fun next(): VideoItem? {
        if (!hasNext()) return null
        index += 1
        return items[index]
    }

    fun clear() {
        items = emptyList()
        index = -1
    }
}
