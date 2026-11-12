package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.navigateToSpace
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.comment.CommentItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommentRowAuthor(item: CommentItem) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = item.uname,
            color = if (item.isVip) colors.primary else colors.onBackgroundVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.clickable(
                interactionSource = null,
                indication = null,
            ) {
                if (item.mid > 0L) {
                    navigateToSpace(context, item.mid)
                }
            },
        )
        if (item.isUp) {
            Spacer(Modifier.width(4.dp))
            UpBadge()
        }
        Spacer(Modifier.width(4.dp))
        Image(
            painter = androidx.compose.ui.res.painterResource(
                id = when (item.level.coerceIn(1, 6)) {
                    1 -> com.aritxonly.bdfun.R.drawable.bili_lv1
                    2 -> com.aritxonly.bdfun.R.drawable.bili_lv2
                    3 -> com.aritxonly.bdfun.R.drawable.bili_lv3
                    4 -> com.aritxonly.bdfun.R.drawable.bili_lv4
                    5 -> com.aritxonly.bdfun.R.drawable.bili_lv5
                    else -> com.aritxonly.bdfun.R.drawable.bili_lv6
                },
            ),
            contentDescription = null,
            modifier = Modifier.size(width = 22.dp, height = 10.dp),
        )
    }
}
