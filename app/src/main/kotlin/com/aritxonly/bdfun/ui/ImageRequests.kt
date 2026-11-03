package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

/**
 * 列表项会在滚动/状态变化时反复重组。把 ImageRequest 记住，避免每次都新建请求对象，
 * 也让 Coil 能稳定复用同一份缓存 key。
 */
@Composable
internal fun rememberImageRequest(
    data: Any?,
    crossfade: Boolean = true,
): ImageRequest {
    val context = LocalContext.current
    return remember(data, crossfade) {
        ImageRequest.Builder(context)
            .data(data)
            .apply {
                if (crossfade) crossfade(true)
            }
            .build()
    }
}
