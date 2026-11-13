package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.feature.comment.application.CommentStateFactory
import com.aritxonly.bdfun.presentation.CommentLikePresentationState
import com.aritxonly.bdfun.presentation.SessionPresentationStateHolder
import kotlinx.coroutines.flow.StateFlow

/** app组装的评论展示接缝，不存储页面、Activity或另一个登录状态。 */
internal class CommentDependencies(
    val controllers: CommentStateFactory,
    val session: SessionPresentationStateHolder,
    val likes: CommentLikePresentationState,
    val likeGeneration: StateFlow<Long>,
)
