package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommentThreadSortRow(content: ThreadSheetContent, onToggleSort: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val comment = content.comment
    val extraCount = content.extraCount
    val sortHot = content.sortHot
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "共 ${comment.replyCount + extraCount} 条评论",
            color = colors.onBackgroundVariant,
            fontSize = 11.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = if (sortHot) "按热度" else "按时间",
            color = colors.onBackgroundVariant,
            fontSize = 11.sp,
            modifier = Modifier
                .clip(Capsule())
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onToggleSort,
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
