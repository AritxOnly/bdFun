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
    val items = androidx.compose.runtime.mutableStateListOf<DynamicItem>()
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
        private set

    /** 只看某个 UP 时，单独拉这个 UP 的动态，不再从关注流里筛。 */
    val userItems = androidx.compose.runtime.mutableStateListOf<DynamicItem>()
    var userLoading by mutableStateOf(false)
        private set
    private var userOffset = ""
    private var userHasMore = true
    private var userMid = 0L

    val activeLoading: Boolean
        get() = if (filterMid == null) loading else userLoading

    /** 本地点赞状态（dynId -> 是否已赞）。 */
    val liked = mutableStateMapOf<String, Boolean>()

    /** 本地点赞数（dynId -> 数量），点赞/取消时同步增减。 */
    val likeCounts = mutableStateMapOf<String, Int>()

    private var offset = ""
    private var started = false
    private var serverHasMore = true
    private val pending = ArrayDeque<DynamicItem>()
    private data class CachedPage(
        val items: List<DynamicItem>,
        val pending: List<DynamicItem>,
        val offset: String,
        val hasMore: Boolean,
    )
    private val cache = mutableMapOf<String, CachedPage>()

    /** 一次只放 15 条；服务端多出来的先留在 pending 里。 */
    private fun drainPending(limit: Int = 15): Int {
        var added = 0
        while (pending.isNotEmpty() && added < limit) {
            val item = pending.removeFirst()
            if (items.none { it.id == item.id }) {
                items.add(item)
                added++
            }
        }
        return added
    }

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

    fun clearFilter() {
        filterMid = null
        userMid = 0L
        userItems.clear()
        userOffset = ""
        userHasMore = true
        userLoading = false
    }

    /** 点头像：单独进入这个 UP 的动态列表，从第一页重新拉。 */
    suspend fun filterTo(mid: Long) {
        if (mid <= 0L) return
        filterMid = mid
        userMid = mid
        userItems.clear()
        userOffset = ""
        userHasMore = true
        userLoading = false
        loadUserMore()
    }

    private suspend fun loadUserMore() {
        if (userMid <= 0L || userLoading || !userHasMore) return
        userLoading = true
        runCatching { api.userDynamics(userMid, userOffset) }
            .onSuccess { page ->
                val known = userItems.mapTo(HashSet()) { it.id }
                userItems.addAll(page.items.filter { known.add(it.id) })
                userOffset = page.offset
                userHasMore = page.hasMore
            }
        userLoading = false
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
        if (filterMid != null) clearFilter()
        type = next
        val cached = cache[next]
        if (cached != null) {
            items.clear()
            items.addAll(cached.items)
            pending.clear()
            pending.addAll(cached.pending)
            offset = cached.offset
            serverHasMore = cached.hasMore
            hasMore = serverHasMore || pending.isNotEmpty()
            error = null
            rebuildAuthors()
        } else {
            items.clear()
            pending.clear()
            authors = emptyList()
            offset = ""
            serverHasMore = true
            hasMore = true
            fetch(loggedIn)
        }
    }

    /** 下拉刷新：不管有没有缓存都重新拉第一页。 */
    suspend fun refresh(loggedIn: Boolean) {
        fetch(loggedIn)
    }

    suspend fun loadMore(loggedIn: Boolean) {
        if (filterMid != null) {
            loadUserMore()
            return
        }
        if (!loggedIn || loading || refreshing) return
        if (!serverHasMore && pending.isEmpty()) {
            hasMore = false
            return
        }
        loading = true
        var added = drainPending(15)
        // 服务端一页大约 20 条；如果 pending 不足 15，再补一页，保证一次至少放满一批。
        while (added < 15 && serverHasMore) {
            val page = runCatching { api.dynamicPage(type, offset) }.getOrNull() ?: break
            offset = page.offset
            serverHasMore = page.hasMore
            val known = items.mapTo(HashSet()) { it.id }
            val fresh = page.items.filter {
                known.add(it.id) && pending.none { p -> p.id == it.id }
            }
            if (fresh.isEmpty() && !page.hasMore) break
            pending.addAll(fresh)
            added += drainPending(15 - added)
            if (page.items.isEmpty()) break
        }
        hasMore = serverHasMore || pending.isNotEmpty()
        cache[type] = CachedPage(items.toList(), pending.toList(), offset, serverHasMore)
        appendAuthors()
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
            items.clear()
            pending.clear()
            offset = page.offset
            serverHasMore = page.hasMore
            pending.addAll(page.items)
            drainPending(15)
            hasMore = serverHasMore || pending.isNotEmpty()
            cache[type] = CachedPage(items.toList(), pending.toList(), offset, serverHasMore)
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
