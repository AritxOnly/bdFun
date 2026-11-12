package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.comment.CommentItem
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun androidx.compose.foundation.layout.RowScope.CommentRowMetadata(item: CommentItem, onReply: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Text(
        text = formatDate(item.ctime),
        color = colors.onBackgroundVariant,
        fontSize = 11.sp,
    )
    if (item.ipLocation.isNotBlank() && com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings.current.showIpLocation) {
        Spacer(Modifier.width(6.dp))
        Text(
            text = item.ipLocation,
            color = colors.onBackgroundVariant,
            fontSize = 11.sp,
        )
    }
    Spacer(Modifier.width(6.dp))
    Text(
        text = "回复",
        color = colors.onBackgroundVariant,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(Capsule())
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onReply,
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}
