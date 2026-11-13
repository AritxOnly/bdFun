package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import com.aritxonly.bdfun.core.domain.SearchFilterState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun FilterMenu(
    value: SearchFilterState,
    onChange: (SearchFilterState) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        FilterSection("排序方式") {
            FilterChips(
                options = listOf("默认排序", "新发布", "播放多", "弹幕多"),
                selected = value.order,
                columns = 2,
                onSelect = { onChange(value.copy(order = it)) },
            )
        }
        FilterSection("发布时间") {
            FilterChips(
                options = listOf("不限", "最近一天", "最近一周", "最近半年"),
                selected = value.published,
                columns = 2,
                onSelect = { onChange(value.copy(published = it)) },
            )
        }
        FilterSection("内容时长") {
            FilterChips(
                options = listOf("不限", "0-10 分钟", "10-30 分钟", "30-60 分钟", "60-120 分钟", "120 分钟+"),
                selected = value.duration,
                columns = 2,
                onSelect = { onChange(value.copy(duration = it)) },
            )
        }
        FilterSection("音画质量", bottom = 4.dp) {
            FilterChips(
                options = listOf("不限", "8K", "4K", "1080P"),
                selected = value.quality,
                columns = 2,
                onSelect = { onChange(value.copy(quality = it)) },
            )
        }
    }
}

@Composable
internal fun FilterSection(
    title: String,
    bottom: androidx.compose.ui.unit.Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = bottom)) {
        Text(
            text = title,
            color = colors.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        )
        content()
    }
}

@Composable
internal fun FilterChips(
    options: List<String>,
    selected: Int,
    columns: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        options.chunked(columns).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEachIndexed { colIndex, label ->
                    val index = rowIndex * columns + colIndex
                    val active = index == selected
                    // 胶囊底：选中粉底白字，未选中淡灰底，两者都半透明。
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clip(Capsule())
                            .background(
                                if (active) {
                                    colors.primary.copy(alpha = 0.78f)
                                } else {
                                    colors.onBackground.copy(alpha = 0.06f)
                                },
                            )
                            .clickable(interactionSource = null, indication = null) {
                                onSelect(index)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = if (active) Color.White else colors.onBackground,
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
