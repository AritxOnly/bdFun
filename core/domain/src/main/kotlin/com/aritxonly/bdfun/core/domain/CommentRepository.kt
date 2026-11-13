package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.comment.CommentPage

interface CommentRepository {
    suspend fun comments(oid: String, type: Int, page: Int): Outcome<CommentPage>
    suspend fun replies(oid: String, type: Int, root: Long, page: Int): Outcome<CommentPage>
}
