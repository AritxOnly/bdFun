package com.aritxonly.bdfun.presentation

import androidx.compose.runtime.mutableStateMapOf
import com.aritxonly.bdfun.core.application.CommentLikeStore
import com.aritxonly.bdfun.core.model.comment.CommentItem
import kotlinx.coroutines.*

/** 按 rpid 投影，点赞只使订阅该评论的行失效，保留原来的列表重组范围。 */
internal class CommentLikePresentationState(store: CommentLikeStore, scope: CoroutineScope) {
    val likedMap = mutableStateMapOf<Long, Boolean>()
    val countMap = mutableStateMapOf<Long, Int>()
    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            store.state.collect { entries ->
                likedMap.keys.removeAll { it !in entries }
                countMap.keys.removeAll { it !in entries }
                entries.forEach { (id, value) -> likedMap[id] = value.liked; countMap[id] = value.count }
            }
        }
    }
    fun isLiked(item: CommentItem) = likedMap[item.rpid] ?: item.liked
    fun likeCount(item: CommentItem) = countMap[item.rpid] ?: item.like
}
