package com.aritxonly.bdfun.feature.comment.application

import com.aritxonly.bdfun.core.application.CommentLikeStore
import com.aritxonly.bdfun.core.domain.CommentContentRepository
import com.aritxonly.bdfun.core.domain.CommentImageRepository
import com.aritxonly.bdfun.core.domain.CommentQuery

/** 评论和楼中楼取得独立控制器，共享的只有账号内点赞快照。 */
internal class CommentStateFactory(
    private val content: CommentContentRepository,
    private val images: CommentImageRepository,
    private val likes: CommentLikeStore,
) {
    fun list(query: CommentQuery) = CommentListStateHolder(content, query)
    fun actions(query: CommentQuery) = CommentActionsStateHolder(content, query, likes)
    fun composer() = CommentComposerStateHolder(images)
}
