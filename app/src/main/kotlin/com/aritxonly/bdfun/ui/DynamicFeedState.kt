package com.aritxonly.bdfun.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.DynamicItem

/** 本次运行内已看过的 UP：头像排到后面用。 */
object SeenDynamics {
    private val seen = mutableSetOf<Long>()

    fun mark(mid: Long) {
        if (mid > 0) seen += mid
    }

    fun isSeen(mid: Long): Boolean = mid in seen
}

data class DynamicAuthor(val mid: Long, val name: String, val face: String)

/**
 * 动态页状态：保留在 App 层，切页面/进详情返回都不会重新拉取。
 * 支持「全部 / 视频」两套缓存与分页。
 */
@Stable
class DynamicFeedState internal constructor(
    private val api: BiliApi,
) {
    val listState = LazyListState()

    var type by mutableStateOf("all")
        private set
    var items by mutableStateOf<List<DynamicItem>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var hasMore by mutableStateOf(true)
        private set
    var pullFraction by mutableStateOf(0f)
    /** 当前下拉位移（dp），给首页同款小电视指示器用。 */
    var pullOffsetDp by mutableStateOf(0f)

    /** 顶栏头像筛选：null = 全部动态。放在 state 里让 App 层顶栏和列表共用。 */
    var filterMid by mutableStateOf<Long?>(null)

    /** 本地点赞状态（dynId -> 是否已赞）。 */
    val liked = mutableStateMapOf<String, Boolean>()

    /** 本地点赞数（dynId -> 数量），点赞/取消时同步增减。 */
    val likeCounts = mutableStateMapOf<String, Int>()

    private var offset = ""
    private var started = false
    private data class CachedPage(
        val items: List<DynamicItem>,
        val offset: String,
        val hasMore: Boolean,
    )
    private val cache = mutableMapOf<String, CachedPage>()

    /**
     * 顶栏头像顺序。关键：选中/已读只记录到 [SeenDynamics]，不在这里立刻重排；
     * 只有刷新（fetch）才会按已读状态重新排序，避免点一下头像整排乱跳。
     */
    var authors by mutableStateOf<List<DynamicAuthor>>(emptyList())
        private set

    private fun authorsFromItems(): List<DynamicAuthor> {
        val map = LinkedHashMap<Long, DynamicAuthor>()
        for (item in items) {
            if (item.mid > 0 && item.name.isNotBlank()) {
                map.putIfAbsent(item.mid, DynamicAuthor(item.mid, item.name, item.face))
            }
            if (map.size >= 14) break
        }
        return map.values.toList()
    }

    /** 刷新时允许按「已看过的排后面」重排。 */
    private fun rebuildAuthors() {
        authors = authorsFromItems().sortedBy { if (SeenDynamics.isSeen(it.mid)) 1 else 0 }
    }

    /** 加载更多时只把新出现的 UP 追加到末尾，不动已有位置。 */
    private fun appendAuthors() {
        val existing = authors.map { it.mid }.toSet()
        authors = authors + authorsFromItems().filter { it.mid !in existing }
    }

    fun markSeen(mid: Long) {
        // 只记录已读，不触发头像重排；刷新时 rebuildAuthors 才会用到
        SeenDynamics.mark(mid)
    }

    suspend fun ensureLoaded(loggedIn: Boolean) {
        // 首帧登录态可能还是 false；不要先占住 started，否则登录态回来后永远不会加载
        if (!loggedIn || started) return
        fetch(loggedIn)
        // 失败时保留可重试的机会；成功拿到数据后才锁定。
        if (items.isNotEmpty()) started = true
    }

    suspend fun switchTo(next: String, loggedIn: Boolean) {
        if (next == type) return
        type = next
        val cached = cache[next]
        if (cached != null) {
            items = cached.items
            offset = cached.offset
            hasMore = cached.hasMore
            error = null
            rebuildAuthors()
        } else {
            items = emptyList()
            authors = emptyList()
            offset = ""
            hasMore = true
            fetch(loggedIn)
        }
    }

    /** 下拉刷新：不管有没有缓存都重新拉第一页。 */
    suspend fun refresh(loggedIn: Boolean) {
        fetch(loggedIn)
    }

    suspend fun loadMore(loggedIn: Boolean) {
        if (!loggedIn || loading || refreshing || !hasMore) return
        loading = true
        runCatching { api.dynamicPage(type, offset) }
            .onSuccess { page ->
                items = (items + page.items).distinctBy { it.id }
                offset = page.offset
                hasMore = page.hasMore
                cache[type] = CachedPage(items, offset, hasMore)
                appendAuthors()
            }
        loading = false
    }

    private suspend fun fetch(loggedIn: Boolean) {
        if (!loggedIn || refreshing) return
        refreshing = true
        error = null
        val page = runCatching { api.dynamicPage(type, "") }.getOrNull()
        if (page == null) {
            if (items.isEmpty()) error = "动态加载失败，稍后再试"
        } else {
            items = page.items
            offset = page.offset
            hasMore = page.hasMore
            cache[type] = CachedPage(items, offset, hasMore)
            // 刷新后才重新按已读状态排头像
            rebuildAuthors()
            // 刷新后不要用 key 把旧的第一条锚在原位，否则新动态会出现在它上面看不见。
            // 直接回到列表顶部，新动态就占据当前第一张卡片的位置。
            runCatching { listState.scrollToItem(0, 0) }
        }
        refreshing = false
    }
}

@Composable
fun rememberDynamicFeedState(api: BiliApi): DynamicFeedState = remember(api) {
    DynamicFeedState(api)
}
