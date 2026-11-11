package com.aritxonly.bdfun.ui

import kotlinx.coroutines.async
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.VideoItem

/**
 * 首页推荐流的状态。
 * 关键点：它由 App 层持有（[rememberHomeFeedState]），所以进视频详情、进搜索/设置再返回时，
 * 首页不会重新拉取、也不会丢滚动位置。首屏只在第一次 [ensureLoaded] 时请求。
 *
 * 另外放进进程级单例：从视频页回来时，即使承载首页的 Activity 被系统回收重建，
 * 只要进程还在，列表和滚动位置仍会保留，不需要重新请求。
 */
object HomeFeedMemory {
    private var state: HomeFeedState? = null

    fun get(api: BiliApi): HomeFeedState =
        state ?: HomeFeedState(api).also { state = it }
}

@Stable
class HomeFeedState internal constructor(
    private val api: BiliApi,
) {
    val gridState = LazyGridState()

    // 用可观察列表原地追加，避免每次翻页都复制整张列表。
    val videos = androidx.compose.runtime.mutableStateListOf<VideoItem>()
    var loading by mutableStateOf(true)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var loadMoreError by mutableStateOf<String?>(null)
        private set
    var endReached by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    /** 下拉进度（0..1，1 = 达到触发阈值）。顶栏据此显示静态小电视。 */
    var pullFraction by mutableFloatStateOf(0f)

    /** 当前下拉位移（dp）。小电视按它判断“会不会被卡片碰到”。 */
    var pullOffsetDp by mutableFloatStateOf(0f)

    /** 外部（点底栏当前页）请求刷新：自增即触发一次带下拉动画的刷新。 */
    var refreshRequest by mutableIntStateOf(0)
        private set

    fun requestRefresh() {
        refreshRequest++
    }

    private val exclusions = com.aritxonly.bdfun.data.RecommendationExclusions()
    private var lastRawPageEmpty = false

    fun dismissRecommendation(video: VideoItem, creator: Boolean = false, category: String? = null) {
        exclusions.exclude(video, creator, category)
        videos.removeAll { !exclusions.accepts(it) }
    }

    private suspend fun recommendations(page: Int): List<VideoItem> {
        val items = api.recommend(page, com.aritxonly.bdfun.AppGraph.settings.recommendWeb)
        lastRawPageEmpty = items.isEmpty()
        // Web recommendations do not always include the partition. Resolve only when a category
        // filter is active; leave failed lookups visible instead of dropping unrelated videos.
        return kotlinx.coroutines.coroutineScope {
            val limit = kotlinx.coroutines.sync.Semaphore(3)
            items.map { video -> async {
                if (exclusions.hasCategories && video.categoryName.isBlank()) {
                    limit.acquire()
                    try {
                        video.copy(categoryName = api.view(video.bvid).tname)
                    } catch (e: kotlinx.coroutines.CancellationException) { throw e
                    } catch (_: Exception) { video
                    } finally { limit.release() }
                } else video
            } }.map { it.await() }.filter(exclusions::accepts)
        }
    }

    private var page = 1
    private var started = false

    /** 首次进入首页时拉一次；失败不锁定，下次回到首页会再试。 */
    suspend fun ensureLoaded() {
        if (started) return
        loadFirstPage(isRefresh = false)
        if (videos.isNotEmpty()) started = true
    }

    /**
     * 下拉刷新：把新内容插到最前面，旧内容全部保留（refresh 之后继续上滑也不会丢）。
     */
    suspend fun refresh() {
        if (refreshing) return
        refreshing = true
        error = null
        loadMoreError = null
        runCatching { recommendations(1) }
            .onSuccess { list ->
                if (list.isNotEmpty()) {
                    val known = videos.mapTo(HashSet()) { it.bvid }
                    val fresh = list.filter { known.add(it.bvid) }
                    if (fresh.isNotEmpty()) {
                        videos.addAll(0, fresh)
                        endReached = false
                    }
                    // 刷新后新内容在最上面：把列表滚回顶部，否则按 key 锚定会停在旧内容
                    gridState.scrollToItem(0)
                }
            }
            .onFailure { error = it.message ?: "网络请求失败" }
        refreshing = false
    }

    suspend fun loadMore() {
        if (loadingMore || endReached || loading || refreshing) return
        loadingMore = true
        loadMoreError = null
        val next = page + 1
        runCatching { recommendations(next) }
            .onSuccess { list ->
                val known = videos.mapTo(HashSet()) { it.bvid }
                val fresh = list.filter { known.add(it.bvid) }
                videos.addAll(fresh)
                page = next
                // A filtered page can be empty while later pages still have useful videos.
                endReached = lastRawPageEmpty || (fresh.isEmpty() && exclusions.isEmpty)
                if (fresh.isEmpty() && !endReached) loadMoreError = "本页内容已过滤，点击继续加载"
            }
            .onFailure {
                // 网络失败不能当成“没有更多”，否则一次抖动就再也翻不了页。
                loadMoreError = it.message ?: "加载失败"
            }
        loadingMore = false
    }

    private suspend fun loadFirstPage(isRefresh: Boolean) {
        if (isRefresh) refreshing = true else loading = true
        error = null
        loadMoreError = null
        runCatching { recommendations(1) }
            .onSuccess { list ->
                if (list.isNotEmpty()) {
                    val distinct = list.distinctBy { it.bvid }
                    val same = videos.size == distinct.size &&
                        videos.indices.all { videos[it].bvid == distinct[it].bvid }
                    if (!same) {
                        videos.clear()
                        videos.addAll(distinct)
                    }
                    page = 1
                    endReached = false
                } else if (videos.isEmpty()) {
                    error = "推荐流为空"
                }
            }
            .onFailure { error = it.message ?: "网络请求失败" }
        loading = false
        refreshing = false
    }
}

@Composable
fun rememberHomeFeedState(api: BiliApi): HomeFeedState =
    remember(api) { HomeFeedMemory.get(api) }

