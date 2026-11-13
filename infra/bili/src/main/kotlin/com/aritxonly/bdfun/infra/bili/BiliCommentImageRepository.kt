package com.aritxonly.bdfun.infra.bili

import android.net.Uri
import com.aritxonly.bdfun.core.domain.CommentImageRepository

class BiliCommentImageRepository(private val uploadImage: suspend (String) -> String?) : CommentImageRepository {
    constructor(backend: BiliBackend) : this({ reference -> backend.uploadCommentImage(Uri.parse(reference)) })
    override suspend fun upload(reference: String) = biliOutcome { uploadImage(reference) }
}
