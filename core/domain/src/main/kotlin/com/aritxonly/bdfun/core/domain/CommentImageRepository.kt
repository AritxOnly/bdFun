package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome

interface CommentImageRepository {
    /** 返回旧上传入口已编码的 pictures 值，表现层只传递，不解析或拼装字段。 */
    suspend fun upload(reference: String): Outcome<String?>
}
