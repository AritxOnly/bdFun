package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.material.TeakGlassPanel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.EmoteItem
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 进程级表情缓存：评论/弹幕输入框与评论区渲染共用一份。 */
object EmoteCache {
    var items by mutableStateOf<List<EmoteItem>>(emptyList())
        private set

    /** [text] -> 图片地址，评论区把 `[doge]` 渲染成图片用。 */
    var byText by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    private var loading = false

    suspend fun ensure(api: BiliApi) {
        if (items.isNotEmpty() || loading) return
        loading = true
        val loaded = runCatching { api.replyEmotes() }.getOrDefault(emptyList())
        items = loaded
        byText = loaded.associate { it.text to it.url }
        loading = false
    }
}

private val FallbackEmojis = listOf(
    "\uD83D\uDE00", "\uD83D\uDE02", "\uD83E\uDD70", "\uD83D\uDE0E", "\uD83E\uDD14", "\uD83D\uDE2D", "\uD83D\uDE21", "\uD83E\uDD73",
    "\uD83D\uDC4D", "\uD83D\uDC4E", "\u2764\uFE0F", "\uD83D\uDD25", "\uD83C\uDF89", "\u2728", "\uD83D\uDCAF", "\uD83D\uDC36",
)

private val EmoteToken = Regex("\\[([^\\]]{1,12})]")

/**
 * 把评论里的 `[doge]` 渲染成官方表情图片；没有表情数据（未登录）时退化为纯文本。
 * [prefix] 用于"回复 @某人："，与正文同一条文本流，天然基线对齐。
 */
@Composable
fun EmoteText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    prefixColor: Color = color,
    prefixWeight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE,
    extraEmotes: Map<String, String> = emptyMap(),
    /** 追加在最后一个字后面的行内内容（比如折叠评论的点赞）。 */
    trailingContent: (@Composable () -> Unit)? = null,
    trailingWidth: TextUnit = 40.sp,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    val map = remember(EmoteCache.byText, extraEmotes) {
        if (extraEmotes.isEmpty()) EmoteCache.byText else EmoteCache.byText + extraEmotes
    }
    // 没有表情时走普通 Text：和首页视频标题完全同一条渲染路径。
    // 之前所有评论都走 AnnotatedString + inlineContent，即使用不到表情，
    // 渲染路径也和普通 Text 不同，字重观感会对不上。
    val hasEmote = remember(text, map) {
        map.isNotEmpty() && EmoteToken.containsMatchIn(text)
    }
    if (!hasEmote && trailingContent == null) {
        Text(
            text = text,
            color = color,
            style = TextStyle(
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = fontWeight,
            ),
            maxLines = maxLines,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = modifier,
        )
        return
    }
    val inline = remember(map, text, fontSize) { mutableMapOf<String, InlineTextContent>() }
    val annotated: AnnotatedString = remember(
        map,
        text,
        prefix,
        prefixColor,
        prefixWeight,
        fontSize,
        trailingContent != null,
    ) {
        inline.clear()
        buildAnnotatedString {
            if (!prefix.isNullOrBlank()) {
                withStyle(SpanStyle(color = prefixColor, fontWeight = prefixWeight)) {
                    append(prefix)
                    append("：")
                }
            }
            var index = 0
            var position = 0
            for (match in EmoteToken.findAll(text)) {
                if (match.range.first > index) {
                    append(text.substring(index, match.range.first))
                }
                val token = match.groupValues[1]
                val url = map[token] ?: map["[$token]"]
                if (url != null) {
                    val key = "emote-${position++}"
                    appendInlineContent(key, "[$token]")
                    inline[key] = InlineTextContent(
                        Placeholder(
                            width = fontSize * 1.25f,
                            height = fontSize * 1.25f,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                        ),
                    ) { _ ->
                        AsyncImage(
                            model = url,
                            contentDescription = token,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(1.dp),
                        )
                    }
                } else {
                    append(match.value)
                }
                index = match.range.last + 1
            }
            if (index < text.length) append(text.substring(index))
            if (trailingContent != null) {
                appendInlineContent("trailing", "赞")
                inline["trailing"] = InlineTextContent(
                    Placeholder(
                        width = trailingWidth,
                        height = fontSize * 1.35f,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    ),
                ) { _ ->
                    trailingContent()
                }
            }
        }
    }
    Text(
        text = annotated,
        inlineContent = inline,
        color = color,
        style = TextStyle(
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight,
        ),
        maxLines = maxLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * 表情面板：固定高度的统一玻璃卡片，内部可上下滑动查看全部表情；
 * 拿不到官方表情包时回退到系统 emoji。
 */
@Composable
fun EmojiPanel(
    api: BiliApi,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    var emotes by remember { mutableStateOf(EmoteCache.items) }

    LaunchedEffect(Unit) {
        EmoteCache.ensure(api)
        emotes = EmoteCache.items
    }

    // 与弹幕设置面板一致的弹出动画：弹簧缩放 + 渐显
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val spring = rememberSpringFloat(if (shown) 1f else 0f, stiffness = 300f, damping = 22f)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val hPad = 10.dp
        val vPad = 8.dp
        val gap = 6.dp
        val columns = (((maxWidth - hPad * 2) + gap) / (34.dp + gap))
            .toInt()
            .coerceIn(5, 10)
        val cell = ((maxWidth - hPad * 2) - gap * (columns - 1)) / columns
        // 固定 4 行高度；内容整体可滚动，不再用 LazyGrid，避免最后一行被圆角裁切。
        val panelHeight = vPad * 2 + cell * 4 + gap * 3 + 2.dp
        val cells = if (emotes.isEmpty()) {
            FallbackEmojis.map { EmojiCell(it, null) }
        } else {
            emotes.map { EmojiCell("[" + it.text + "]", it.url) }
        }
        val rows = cells.chunked(columns)

        TeakGlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .height(panelHeight)
                .graphicsLayer {
                    alpha = spring.coerceIn(0f, 1f)
                    scaleX = 0.82f + 0.18f * spring
                    scaleY = 0.82f + 0.18f * spring
                    translationY = (1f - spring) * 10f
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                }
                .dropShadow(
                    shape = RoundedCornerShape(24.dp),
                    shadow = androidx.compose.ui.graphics.shadow.Shadow(
                        radius = 8.dp,
                        color = Color.Black,
                        alpha = 0.10f,
                    ),
                ),
            shape = RoundedCornerShape(24.dp),
            // 和设置二级菜单/投币面板同一套底灰
            tintAlpha = 0.3f,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = hPad, vertical = vPad),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(cell),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        row.forEach { item ->
                            Box(
                                modifier = Modifier
                                    .size(cell)
                                    .clickable(interactionSource = null, indication = null) {
                                        onPick(item.pick)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (item.url == null) {
                                    Text(text = item.pick, fontSize = 22.sp)
                                } else {
                                    AsyncImage(
                                        model = item.url,
                                        contentDescription = item.pick,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(cell * 0.84f),
                                    )
                                }
                            }
                        }
                        repeat(columns - row.size) {
                            Spacer(Modifier.size(cell))
                        }
                    }
                }
            }
        }
    }
}

private data class EmojiCell(val pick: String, val url: String?)
