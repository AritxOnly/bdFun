package com.aritxonly.bdfun.feature.home.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.RecommendationActionsRepository
import com.aritxonly.bdfun.core.domain.VideoRepository
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.flow.*

internal enum class HomeVideoAction { WatchLater, HideVideo, HideCreator, HideCategory }
internal data class HomeActionsUiState(
    val video: VideoItem? = null,
    val category: String = "",
    val categoryLoading: Boolean = false,
    val busy: Boolean = false,
) {
    val categoryReason get() = video?.dislikeReasons?.firstOrNull { category.isNotBlank() && it.name.contains(category) }
}

/** 反馈成功才修改本地过滤规则，避免服务端失败时列表却已消失。 */
internal class HomeActionsStateHolder(
    private val videos: VideoRepository,
    private val actions: RecommendationActionsRepository,
    private val feed: HomeFeedStateHolder,
) {
    private val mutable = MutableStateFlow(HomeActionsUiState())
    val state: StateFlow<HomeActionsUiState> = mutable.asStateFlow()
    private var selection = 0L
    private var closed = false
    fun close() {
        closed = true
        selection++
        mutable.update { it.copy(categoryLoading = false, busy = false) }
    }

    suspend fun select(video: VideoItem?) {
        if (closed) return
        val ticket = ++selection
        mutable.update { it.copy(video = video, category = video?.categoryName.orEmpty(), categoryLoading = false) }
        if (video == null || video.categoryName.isNotBlank()) return
        mutable.update { it.copy(categoryLoading = true) }
        try {
            when (val result = videos.videoDetail(video.bvid)) {
                is Outcome.Success -> if (ticket == selection) mutable.update { it.copy(category = result.value.tname) }
                is Outcome.Failure -> Unit
            }
        } finally {
            if (ticket == selection) mutable.update { it.copy(categoryLoading = false) }
        }
    }

    suspend fun perform(action: HomeVideoAction): Outcome<String>? {
        val current = mutable.value
        val target = current.video ?: return null
        if (closed || current.busy) return null
        mutable.update { it.copy(busy = true) }
        return try {
            val result = when (action) {
                HomeVideoAction.WatchLater -> when (val result = actions.addWatchLater(target.aid)) {
                    is Outcome.Failure -> result
                    is Outcome.Success -> if (result.value) Outcome.Success("已添加至稍后再看") else failure("添加失败")
                }
                HomeVideoAction.HideVideo -> hide(target, creator = false)
                HomeVideoAction.HideCreator -> hide(target, creator = true)
                HomeVideoAction.HideCategory -> hideCategory(target, current.category, current.categoryReason)
            }
            if (closed) null else result
        } finally { mutable.update { it.copy(busy = false) } }
    }

    private suspend fun hide(target: VideoItem, creator: Boolean): Outcome<String> {
        val reason = if (!target.appRecommendation) RecommendationReason(if (creator) 4 else 1, "")
        else target.dislikeReasons.firstOrNull {
            if (creator) it.name.contains("UP", ignoreCase = true)
            else it.name.contains("不感兴趣") || it.name.contains("当前视频")
        }
        if (reason == null) return failure(if (creator) "推荐接口未提供 UP 反馈，请刷新推荐后重试" else "推荐接口未提供此项反馈，请刷新推荐后重试")
        when (val result = actions.dislike(target, reason)) {
            is Outcome.Failure -> return result
            is Outcome.Success -> Unit
        }
        if (closed) throw kotlinx.coroutines.CancellationException("首页已关闭")
        feed.dismissRecommendation(target, creator = creator)
        return Outcome.Success(if (creator) "已反馈，不再推荐此 UP" else "已反馈，减少此类推荐")
    }

    private suspend fun hideCategory(target: VideoItem, category: String, reason: RecommendationReason?): Outcome<String> {
        if (category.isBlank()) return failure("分区暂不可用")
        if (reason != null) when (val result = actions.dislike(target, reason)) {
            is Outcome.Failure -> return result
            is Outcome.Success -> Unit
        }
        if (closed) throw kotlinx.coroutines.CancellationException("首页已关闭")
        feed.dismissRecommendation(target, category = category)
        return Outcome.Success(if (reason != null) "已反馈，减少 $category 推荐" else "本次浏览不再显示 $category 推荐")
    }

    private fun failure(message: String) = Outcome.Failure(AppError.Unknown(message))
}
