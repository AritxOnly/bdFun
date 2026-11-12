package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.core.model.cache.CacheStatus
import com.aritxonly.bdfun.core.model.cache.CachedVideo
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
internal fun androidx.compose.foundation.layout.RowScope.CacheRowContent(entry: CachedVideo) {
    val colors = MiuixTheme.colorScheme
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = entry.title,
            color = colors.onBackground,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = when (entry.status) {
                CacheStatus.Completed ->
                    "${entry.qualityLabel} · ${formatCacheSize(entry.totalBytes)}"
                CacheStatus.Failed -> entry.error ?: "缓存失败"
                else -> "${entry.qualityLabel} · ${(entry.progress * 100).toInt()}%"
            },
            color = if (entry.status == CacheStatus.Failed) {
                Color(0xFFE5484D)
            } else {
                colors.onBackgroundVariant
            },
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (entry.status == CacheStatus.Queued || entry.status == CacheStatus.Downloading) {
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.surfaceContainerHigh),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(entry.progress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(BrandPink),
                )
            }
        }
    }
}
