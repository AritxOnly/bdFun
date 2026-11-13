package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
internal fun CacheRowActions(completed: Boolean, onOpen: () -> Unit, onDelete: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Column(horizontalAlignment = Alignment.End) {
        if (completed) {
            Text(
                text = "播放",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(interactionSource = null, indication = null, onClick = onOpen)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = BrandPink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            )
        }
        Text(
            text = "删除",
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onDelete,
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = colors.onBackgroundVariant,
            fontSize = 11.sp,
        )
    }
}
