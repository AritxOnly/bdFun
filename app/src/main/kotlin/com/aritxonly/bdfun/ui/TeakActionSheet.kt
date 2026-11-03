package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.material.TeakGlassPanel

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.bdfun.BrandPink
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.kyant.shapes.Capsule
import kotlinx.coroutines.delay

/**
 * 统一的玻璃底部操作面板：评论长按菜单、投币、收藏夹都用它。
 * 弹簧上滑 + 统一 TeakGlass 外观，点外面收起。
 */
@Composable
fun TeakActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    // HyperIsland 风格：底部弹簧弹出，允许一点点过冲。
    // 首次组合时先以 0 为起点，否则「长按评论」这种随状态一起出现的面板会直接瞬移到终点。
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mounted = true }
    // 打开后的头 260ms 不响应“点外面关闭”：否则打开面板的那一次抬手会落进新 Dialog 的
    // 背景层，面板刚弹出就被立刻关掉（表现成动画诡异、闪一下）。
    var dismissArmed by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            dismissArmed = false
            delay(260)
            dismissArmed = true
        } else {
            dismissArmed = false
        }
    }
    // 屏幕旋转时重建 Dialog：否则玻璃会继续采样旋转前录制的 backdrop，出现整块糊错位
    val orientation = LocalConfiguration.current.orientation
    // 进入保留上一版的弹簧回弹；退出用同一条弹簧反向跑，只把 0 以下钳掉，避免在底部回弹
    val spring = rememberSpringFloat(
        target = if (visible && mounted) 1f else 0f,
        stiffness = 420f,
        damping = 28f,
    )
    val p = spring
    if (!visible && p <= 0.005f) return

    key(orientation) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                // 关闭只走我们自己的点击层，避免系统把“打开面板的那次抬手”当成点外面
                dismissOnClickOutside = false,
            ),
        ) {
            // Dialog 默认会 dimBehind，这里关掉，保持背景原样
            val view = androidx.compose.ui.platform.LocalView.current
            val dialogWindow = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            androidx.compose.runtime.SideEffect { dialogWindow?.setDimAmount(0f) }
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // 不压暗背景，只做点击层
                        .background(Color.Transparent)
                        .clickable(interactionSource = null, indication = null) {
                            if (dismissArmed) onDismiss()
                        },
                )
                TeakGlassPanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 20.dp)
                        .graphicsLayer {
                            val progress = p.coerceAtLeast(0f)
                            // 进入渐显 + 上滑；退出同样在上滑回去的过程中渐隐，而不是到底部才消失
                            alpha = progress.coerceIn(0f, 1f)
                            translationY = (1f - progress) * size.height
                        }
                        .dropShadow(
                            shape = RoundedCornerShape(28.dp),
                            shadow = Shadow(
                                radius = 8.dp,
                                color = Color.Black,
                                alpha = 0.10f,
                            ),
                        ),
                    shape = RoundedCornerShape(28.dp),
                    // 和设置页二级菜单同一套：底灰 0.3、24% 磨砂
                    tintAlpha = 0.3f,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 12.dp),
                    ) {
                        if (!title.isNullOrBlank()) {
                            Text(
                                text = title,
                                color = colors.onBackground,
                                fontSize = 18.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                        content()
                    }
                }
            }
        }
    }
}

/** 面板里的一行：粉色胶囊、居中、白色高光描边（危险操作用红色胶囊）。 */
@Composable
fun SheetActionRow(
    label: String,
    glyph: MenuGlyphKind?,
    color: Color,
    danger: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val fill = if (danger) {
        Brush.verticalGradient(listOf(Color(0xFFE5484D), Color(0xFFC93B40)))
    } else {
        Brush.verticalGradient(
            listOf(
                com.aritxonly.bdfun.BrandPink,
                com.aritxonly.bdfun.BrandPinkDeep,
            ),
        )
    }
    val rim = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.62f), Color.White.copy(alpha = 0.10f)),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .height(46.dp)
            .clip(Capsule())
            .background(fill)
            .border(1.dp, rim, Capsule())
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (glyph != null) {
                MenuGlyph(kind = glyph, tint = Color.White, size = 18.dp)
                Spacer(Modifier.width(9.dp))
            }
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            )
            trailing?.invoke()
        }
    }
}
