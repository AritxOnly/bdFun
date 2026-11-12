package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun rememberCommentThreadCollapseConnection(page: VideoPagePresentationState): androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
    return remember(page) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                if (available.y >= 0f) return androidx.compose.ui.geometry.Offset.Zero
                val sink = page.playerCollapseScroll
                    ?: return androidx.compose.ui.geometry.Offset.Zero
                return androidx.compose.ui.geometry.Offset(0f, sink(available.y))
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                if (available.y <= 0f) return androidx.compose.ui.geometry.Offset.Zero
                val sink = page.playerCollapseScroll
                    ?: return androidx.compose.ui.geometry.Offset.Zero
                return androidx.compose.ui.geometry.Offset(0f, sink(available.y))
            }
        }
    }

}
