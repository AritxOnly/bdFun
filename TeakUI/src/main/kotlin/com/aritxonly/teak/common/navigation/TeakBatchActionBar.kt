package com.aritxonly.teak.common.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.TeakMotion
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
data class TeakBatchAction(
    val key: String,
    val label: String,
    val enabled: Boolean = true,
    val danger: Boolean = false,
)

/**
 * 批量管理用的小型悬浮操作条：从右下角浮出，外观与底栏同一套玻璃胶囊。
 *
 * 全选 / 删除 / 移动到 / 复制到都放在这里，避免再占一行列表空间。
 * 隐藏时保留到退场动画结束，避免底部突然“啪”地消失。
 */
@Composable
fun TeakBatchActionBar(
    visible: Boolean,
    actions: List<TeakBatchAction>,
    onAction: (TeakBatchAction) -> Unit,
    modifier: Modifier = Modifier,
    selectedCount: Int = 0,
    backdrop: top.yukonga.miuix.kmp.blur.Backdrop? = LocalTeakBackdrop.current,
) {
    var progress by remember { mutableFloatStateOf(if (visible) 1f else 0f) }
    androidx.compose.runtime.LaunchedEffect(visible) {
        val target = if (visible) 1f else 0f
        if (!TeakMotion.enabled) {
            progress = target
            return@LaunchedEffect
        }
        var velocity = 0f
        var last = System.nanoTime()
        while (true) {
            val now = System.nanoTime()
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            last = now
            val accel = -420f * (progress - target) - 30f * velocity
            velocity += accel * dt
            progress += velocity * dt
            if (kotlin.math.abs(progress - target) < 0.002f &&
                kotlin.math.abs(velocity) < 0.02f
            ) {
                progress = target
                break
            }
            kotlinx.coroutines.delay(8)
        }
    }
    if (progress <= 0.005f && !visible) return

    val colors = MiuixTheme.colorScheme
    val p = progress.coerceIn(0f, 1f)
    TeakGlassSurface(
        modifier = modifier
            .graphicsLayer {
                alpha = p
                translationY = (1f - p) * 40.dp.toPx()
                scaleX = 0.92f + 0.08f * p
                scaleY = 0.92f + 0.08f * p
            }
            .height(46.dp),
        shape = Capsule(),
        recipe = TeakGlassRecipes.floatingNavigation(
            com.aritxonly.teak.common.material.LocalTeakMaterialSpec.current.fineTuning,
        ),
        backdrop = backdrop,
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (selectedCount >= 0) {
                Text(
                    text = "已选 $selectedCount",
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            actions.forEachIndexed { index, action ->
                Box(
                    modifier = Modifier
                        .clip(Capsule())
                        .clickable(
                            enabled = action.enabled,
                            interactionSource = null,
                            indication = null,
                        ) { onAction(action) }
                        .padding(horizontal = 9.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = action.label,
                        color = when {
                            !action.enabled -> colors.onBackgroundVariant.copy(alpha = 0.35f)
                            action.danger -> Color(0xFFE5484D)
                            else -> colors.onBackground
                        },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }
            Spacer(Modifier.width(2.dp))
        }
    }
}
