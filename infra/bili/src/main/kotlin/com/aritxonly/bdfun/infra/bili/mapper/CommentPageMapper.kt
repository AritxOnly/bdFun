package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.comment.*
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import org.json.JSONObject

internal fun mapCommentPage(data: JSONObject, page: Int, upMid: Long, includeTop: Boolean = true): CommentPage {
    val list = mutableListOf<CommentItem>()
    if (includeTop) data.optJSONArray("top")?.objects()?.forEach { list += parseComment(it, upMid, true) }
    data.optJSONArray("replies")?.objects()?.forEach { list += parseComment(it, upMid, false) }
    val pageData = data.optJSONObject("page")
    val count = pageData?.optInt("count") ?: 0
    return CommentPage(
        items = list.distinctBy { it.rpid },
        page = page,
        isEnd = page >= count,
        total = pageData?.optInt("acount") ?: 0,
    )
}
