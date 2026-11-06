package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aritxonly.teak.common.chrome.TeakGlass
import com.aritxonly.teak.common.chrome.TeakGlassCircleButton
import com.aritxonly.teak.common.chrome.teakGlassGlyphColor
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ContactsCircle
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 页内返回键（不读 backdrop）：玻璃圆钮 + 上亮下暗高光描边 + 柔和投影 + 按压回弹。
 * 需要真模糊的全局返回键见 `TeakGlassBackButton`。
 */
@Composable
fun TeakBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val glyph = teakGlassGlyphColor()
    TeakGlassCircleButton(onClick = onClick, modifier = modifier, size = size) {
        Canvas(modifier = Modifier.size(size * 0.50f).align(Alignment.Center)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = w * 0.115f
            val chevron = Path().apply {
                moveTo(w * 0.62f, h * 0.16f)
                lineTo(w * 0.34f, h * 0.5f)
                lineTo(w * 0.62f, h * 0.84f)
            }
            drawPath(
                path = chevron,
                color = glyph,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/** 顶部搜索条：胶囊玻璃 + 放大镜 + 提示文案。 */
@Composable
fun TeakSearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "搜索视频、UP 主",
    height: Dp = 40.dp,
    /** 传了就替换掉内部的"图标 + 占位文字"，用于内嵌输入框。 */
    content: (@Composable () -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    TeakGlass(
        modifier = modifier.fillMaxWidth().height(height),
        shape = Capsule(),
        onClick = onClick,
        elevation = 4.dp,
    ) {
        if (content != null) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                content()
            }
            return@TeakGlass
        }
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                imageVector = MiuixIcons.Search,
                contentDescription = "搜索",
                tint = teakGlassGlyphColor().copy(alpha = 0.72f),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = hint,
                color = colors.onBackgroundVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 顶部消息键：玻璃圆钮 + 小红点。 */
@Composable
fun TeakGlassMessageButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    unread: Boolean = true,
) {
    TeakGlassCircleButton(onClick = onClick, modifier = modifier, size = size) {
        Icon(
            imageVector = MiuixIcons.Messages,
            contentDescription = "消息",
            tint = teakGlassGlyphColor(),
            modifier = Modifier.size(size * 0.46f).align(Alignment.Center),
        )
        if (unread) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = size * 0.14f, end = size * 0.16f)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
            )
        }
    }
}

/** 顶部头像：圆形裁切 + 细圈；未登录给占位图标。 */
@Composable
fun TeakAvatar(
    faceUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val colors = MiuixTheme.colorScheme
    val isDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    val context = LocalContext.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.surfaceContainerHigh)
            .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0x66FFFFFF), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (!faceUrl.isNullOrBlank()) {
            AsyncImage(
                model = rememberImageRequest(faceUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = MiuixIcons.ContactsCircle,
                contentDescription = "未登录",
                tint = colors.onBackgroundVariant,
                modifier = Modifier.size(size * 0.58f),
            )
        }
    }
}
