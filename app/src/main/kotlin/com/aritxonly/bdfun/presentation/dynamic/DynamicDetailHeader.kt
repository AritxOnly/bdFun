package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.feature.dynamic.application.DynamicDetailUiState
import com.aritxonly.bdfun.ui.DynamicCard
import com.aritxonly.bdfun.ui.DynamicCardActions
import com.aritxonly.bdfun.ui.DynamicCardPresentation
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun LazyListScope.dynamicDetailHeader(snapshot: DynamicDetailUiState, actions: DynamicDetailActions) {
    val detail = snapshot.detail
    val liked = snapshot.liked
    val likeCount = snapshot.likeCount
    item(key = "detail-big-title") { TeakLargeTitle(title = "动态详情") }
    item(key = "detail-dynamic") {
        Box(modifier = Modifier.padding(bottom = 4.dp)) {
            DynamicCard(
                item = detail.copy(
                    counts = detail.counts.copy(
                        liked = liked,
                        like = likeCount,
                    ),
                ),
                presentation = DynamicCardPresentation(liked, expanded = true, animatedOpen = false),
                actions = DynamicCardActions(
                    onOpen = {},
                    onOpenVideo = actions::openVideo,
                    onOpenForwarded = actions::openForwarded,
                    onLike = actions::like,
                    onComment = {},
                    onRepost = {},
                ),
            )
        }
    }
    item(key = "detail-comment-label") {
        Text(
            text = "评论",
            color = MiuixTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 2.dp, top = 16.dp, bottom = 2.dp),
        )
    }
}
