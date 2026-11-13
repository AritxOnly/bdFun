package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.comment.CommentPage

data class CommentQuery(val oid: String, val type: Int, val upMid: Long, val root: Long = 0, val sortHot: Boolean = true)
data class CommentPost(val query: CommentQuery, val text: String, val root: Long, val parent: Long, val pictures: String? = null)

interface CommentContentRepository {
    suspend fun page(query: CommentQuery, page: Int): Outcome<CommentPage>
    suspend fun post(post: CommentPost): Outcome<Long?>
    suspend fun like(query: CommentQuery, rpid: Long, liked: Boolean): Outcome<Unit>
    suspend fun report(query: CommentQuery, rpid: Long): Outcome<Unit>
    suspend fun delete(query: CommentQuery, rpid: Long): Outcome<Unit>
    suspend fun block(mid: Long): Outcome<Unit>
}
