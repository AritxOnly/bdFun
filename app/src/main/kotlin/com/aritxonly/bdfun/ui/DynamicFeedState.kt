package com.aritxonly.bdfun.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Each category/UP owns its cursor and cache. Generations discard late responses after switching. */
@Stable
class DynamicFeedState internal constructor(private val api: DynamicFeedSource) {
    val listState = LazyListState()
    var type by mutableStateOf("all")
        private set
    var filterMid by mutableStateOf<Long?>(null)
        private set
    private var lastMid: Long? = null
    val items = mutableStateListOf<DynamicItem>()
    var loading by mutableStateOf(false)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var hasMore by mutableStateOf(true)
        private set
    var pullFraction by mutableStateOf(0f)
    var pullOffsetDp by mutableStateOf(0f)
    val activeLoading get() = loading
    val liked = mutableStateMapOf<String, Boolean>()
    val likeCounts = mutableStateMapOf<String, Int>()
    var authors by mutableStateOf<List<DynamicAuthor>>(emptyList())
        private set
    var liveUsers by mutableStateOf<List<DynamicLiveUser>>(emptyList())
        private set
    var liveCount by mutableStateOf(0)
        private set
    var authorsLoading by mutableStateOf(false)
        private set
    var authorsError by mutableStateOf<String?>(null)
        private set
    var authorsHasMore by mutableStateOf(false)
        private set
    var showLive by mutableStateOf(true)
    private var authorsOffset = ""
    private var portalLoaded = false
    private var portalGeneration = 0
    private var generation = 0
    private var offset = ""
    private var started = false
    private var retryReset = true
    private data class CachedPage(val items: List<DynamicItem>, val offset: String, val hasMore: Boolean)
    private val cache = mutableMapOf<String, CachedPage>()
    private val key get() = "$type:${filterMid ?: 0}"

    fun markSeen(mid: Long) {
        authors = authors.map { if (it.mid == mid) it.copy(hasUpdate = false) else it }
    }

    suspend fun ensureLoaded(loggedIn: Boolean) {
        if (!loggedIn) return
        coroutineScope {
            if (!portalLoaded && !authorsLoading) launch { refreshAuthors() }
            if (!started && !refreshing) fetch(reset = true)
        }
    }

    suspend fun switchTo(next: String, loggedIn: Boolean) {
        if (next !in DynamicFeedContract.types) return
        val mid = if (next == "up") lastMid else null
        if (type == next && filterMid == mid) return
        select(next, mid, loggedIn)
    }

    suspend fun clearFilter() {
        if (type == "all") refresh(true) else switchTo("all", true)
    }

    suspend fun filterTo(mid: Long) {
        if (mid <= 0) return
        markSeen(mid)
        lastMid = mid
        // Tapping the selected author reloads their feed, as in Hyper-PiliPlus.
        if (type == "up" && filterMid == mid) fetch(reset = true)
        else select("up", mid, true)
    }

    private suspend fun select(next: String, mid: Long?, loggedIn: Boolean) {
        generation++
        type = next
        filterMid = mid
        loading = false
        refreshing = false
        error = null
        val saved = cache[key]
        items.clear()
        if (saved != null) items.addAll(saved.items)
        offset = saved?.offset.orEmpty()
        hasMore = saved?.hasMore ?: true
        started = saved != null
        if (loggedIn && saved == null && (next != "up" || mid != null)) fetch(reset = true)
    }

    suspend fun refresh(loggedIn: Boolean) {
        if (!loggedIn) return
        coroutineScope {
            launch { refreshAuthors() }
            fetch(reset = true)
        }
    }

    suspend fun loadMore(loggedIn: Boolean) {
        if (loggedIn && !loading && !refreshing && hasMore) fetch(reset = false)
    }

    suspend fun retry(loggedIn: Boolean) {
        if (loggedIn && !loading && !refreshing) fetch(reset = retryReset)
    }

    private suspend fun fetch(reset: Boolean) {
        if (type == "up" && filterMid == null) return
        if (reset && refreshing) return
        val ticket = if (reset) ++generation else generation
        val requestKey = key
        val requestType = type
        val requestMid = filterMid
        if (reset) { refreshing = true; loading = false } else loading = true
        error = null
        retryReset = reset
        try {
            var cursor = if (reset) "" else offset
            val collected = if (reset) mutableListOf() else items.toMutableList()
            val known = collected.mapTo(HashSet()) { it.id }
            val visited = hashSetOf(cursor)
            var more = false
            // Skip duplicate/unsupported-only pages, bounded to avoid a broken server cursor loop.
            for (attempt in 0 until 5) {
                val page = api.dynamicPage(requestType, cursor, requestMid)
                if (ticket != generation) return
                val fresh = page.items.filter { it.id.isNotBlank() && known.add(it.id) }
                collected.addAll(fresh)
                more = page.hasMore && page.offset.isNotBlank() && visited.add(page.offset)
                cursor = page.offset
                if (fresh.isNotEmpty() || !more) break
            }
            items.clear()
            items.addAll(collected)
            offset = cursor
            hasMore = more
            started = true // Empty successful feeds must not refetch on every visit.
            cache[requestKey] = CachedPage(collected.toList(), cursor, more)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: Exception) {
            if (ticket == generation) error = failure.message?.takeIf { it.isNotBlank() } ?: "动态加载失败，点击重试"
        } finally {
            if (ticket == generation) { refreshing = false; loading = false }
        }
    }

    suspend fun refreshAuthors() {
        val ticket = ++portalGeneration
        authorsLoading = true
        authorsError = null
        try {
            val portal = api.dynamicPortal()
            if (ticket != portalGeneration) return
            authors = portal.upList.items.distinctBy { it.mid }
            authorsOffset = portal.upList.offset
            authorsHasMore = portal.upList.hasMore && authorsOffset.isNotBlank()
            liveUsers = portal.liveUsers.distinctBy { it.roomId }
            liveCount = portal.liveCount
            portalLoaded = true
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            if (ticket == portalGeneration) authorsError = "UP 列表加载失败，点击重试"
        } finally {
            if (ticket == portalGeneration) authorsLoading = false
        }
    }

    suspend fun loadMoreAuthors() {
        if (authorsLoading || !authorsHasMore) return
        val ticket = portalGeneration
        val cursor = authorsOffset
        authorsLoading = true
        authorsError = null
        try {
            val page = api.dynamicUpList(cursor)
            if (ticket != portalGeneration) return
            authors = (authors + page.items).distinctBy { it.mid }
            authorsOffset = page.offset
            authorsHasMore = page.hasMore && page.offset.isNotBlank() && page.offset != cursor
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            if (ticket == portalGeneration) authorsError = "UP 列表加载失败，点击重试"
        } finally {
            if (ticket == portalGeneration) authorsLoading = false
        }
    }
}

@Composable
fun rememberDynamicFeedState(api: BiliApi, accountMid: Long?): DynamicFeedState = remember(api, accountMid) {
    DynamicFeedState(api)
}
