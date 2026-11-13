package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.aritxonly.bdfun.core.domain.SearchFilterState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ResultsTabHeader(
    tab: Int,
    onTabChange: (Int) -> Unit,
    position: androidx.compose.runtime.State<Float>,
    filterOpen: Boolean,
    onFilterOpenChange: (Boolean) -> Unit,
    filters: SearchFilterState,
    onFiltersChange: (SearchFilterState) -> Unit,
    onApplyFilters: (SearchFilterState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val anchor = remember { SearchFilterAnchorStateHolder() }
    Box(
        modifier = modifier.height(42.dp),
    ) {
        TeakPagerTabs(
            options = listOf("视频", "用户"),
            selected = tab,
            onSelect = onTabChange,
            itemWidth = 64.dp,
            position = position,
            modifier = Modifier.align(Alignment.Center),
        )
        // 漏斗：真正采样 Tab 后面的内容，和设置页二级菜单一样是 Miuix 玻璃。
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                // 顶栏清空按钮的右边距也是 12dp，这里正好对齐。
                .padding(end = 12.dp)
                .onGloballyPositioned { anchor.bounds = it.boundsInWindow() },
        ) {
            com.aritxonly.teak.common.material.TeakGlassIconButton(
                onClick = { onFilterOpenChange(!filterOpen) },
                size = 40.dp,
                backdrop = LocalTeakBackdrop.current,
            ) {
                FunnelGlyph()
            }
        }
        TeakGlassMenu(
            visible = filterOpen,
            onDismiss = { onFilterOpenChange(false) },
            anchorInWindow = anchor.bounds,
            minWidth = 280.dp,
            maxWidth = 340.dp,
            maxHeight = 520.dp,
        ) {
            FilterMenu(
                value = filters,
                onChange = {
                    onFiltersChange(it)
                    onApplyFilters(it)
                },
            )
        }
    }
}

@Composable
internal fun FunnelGlyph() {
    val colors = MiuixTheme.colorScheme
    Canvas(modifier = Modifier.size(17.dp)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.085f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.12f, h * 0.18f)
            lineTo(w * 0.88f, h * 0.18f)
            lineTo(w * 0.58f, h * 0.52f)
            lineTo(w * 0.58f, h * 0.86f)
            lineTo(w * 0.42f, h * 0.74f)
            lineTo(w * 0.42f, h * 0.52f)
            close()
        }
        drawPath(
            path = path,
            color = colors.onBackground,
            style = Stroke(
                width = stroke,
                cap = StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}

internal class SearchFilterAnchorStateHolder {
    var bounds by mutableStateOf(androidx.compose.ui.geometry.Rect.Zero)
}
