package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aritxonly.bdfun.data.FavoriteFolder
import com.aritxonly.bdfun.data.HistoryItem
import com.aritxonly.bdfun.data.VideoItem
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 收藏 / 历史 / 稍后再看里的独立搜索页。
 *
 * 与首页搜索不同：这里不做热搜，只显示搜索历史；搜索范围是当前收藏夹 / 历史 / 稍后再看
 * 已经加载到的内容，结果继续用原来的双排视频卡片排版。
 */
@Composable
internal fun LibrarySearchPage(
    mode: Int,
    title: String,
    history: List<String>,
    folders: List<FavoriteFolder>,
    folderIndex: Int,
    folderVideos: List<VideoItem>,
    historyEntries: List<HistoryItem>,
    watchLaterEntries: List<HistoryItem>,
    loading: Boolean,
    error: String?,
    onClearHistory: () -> Unit,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    var draft by remember { mutableStateOf("") }
    var submittedLocal by remember { mutableStateOf(false) }

    val results: List<VideoItem> = when (mode) {
        0 -> folderVideos
        1 -> historyEntries.map { it.toVideoItem() }
        else -> watchLaterEntries.map { it.toVideoItem() }
    }
    val committed = submittedLocal && draft.isNotBlank()

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        val dialogWindow = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        androidx.compose.runtime.SideEffect { dialogWindow?.setDimAmount(0f) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding(),
        ) {
            // 顶栏：返回键 + 与首页同款胶囊搜索框 + 清空键
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp, end = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHigh)
                        .clickable(interactionSource = null, indication = null, onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val w = size.width
                        val h = size.height
                        drawLine(
                            color = colors.onBackground,
                            start = Offset(w * 0.62f, h * 0.18f),
                            end = Offset(w * 0.3f, h * 0.5f),
                            strokeWidth = w * 0.11f,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = colors.onBackground,
                            start = Offset(w * 0.3f, h * 0.5f),
                            end = Offset(w * 0.62f, h * 0.82f),
                            strokeWidth = w * 0.11f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(Capsule())
                        .background(colors.surfaceContainerHigh)
                        .padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        val c = colors.onBackgroundVariant
                        drawCircle(
                            color = c,
                            radius = size.width * 0.29f,
                            center = Offset(size.width * 0.43f, size.height * 0.43f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = size.width * 0.09f,
                            ),
                        )
                        drawLine(
                            color = c,
                            start = Offset(size.width * 0.64f, size.height * 0.64f),
                            end = Offset(size.width * 0.87f, size.height * 0.87f),
                            strokeWidth = size.width * 0.09f,
                            cap = StrokeCap.Round,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (draft.isEmpty()) {
                            Text(
                                text = "搜索$title",
                                color = colors.onBackgroundVariant,
                                fontSize = 13.5.sp,
                            )
                        }
                        BasicTextField(
                            value = draft,
                            onValueChange = {
                                draft = it
                                submittedLocal = false
                            },
                            singleLine = true,
                            textStyle = TextStyle(color = colors.onBackground, fontSize = 13.5.sp),
                            cursorBrush = SolidColor(colors.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (draft.isNotBlank()) {
                                    submittedLocal = true
                                    onSubmit(draft)
                                }
                            }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(interactionSource = null, indication = null) {
                                draft = ""
                                submittedLocal = false
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (draft.isNotEmpty()) {
                            Canvas(modifier = Modifier.size(14.dp)) {
                                val stroke = size.width * 0.12f
                                drawLine(
                                    color = colors.onBackgroundVariant,
                                    start = Offset(size.width * 0.2f, size.height * 0.2f),
                                    end = Offset(size.width * 0.8f, size.height * 0.8f),
                                    strokeWidth = stroke,
                                    cap = StrokeCap.Round,
                                )
                                drawLine(
                                    color = colors.onBackgroundVariant,
                                    start = Offset(size.width * 0.8f, size.height * 0.2f),
                                    end = Offset(size.width * 0.2f, size.height * 0.8f),
                                    strokeWidth = stroke,
                                    cap = StrokeCap.Round,
                                )
                            }
                        }
                    }
                }
            }

            if (!committed) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item(key = "history-title", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "搜索历史",
                                color = colors.onBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.weight(1f))
                            if (history.isNotEmpty()) {
                                Text(
                                    text = "清空",
                                    color = colors.onBackgroundVariant,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable {
                                        onClearHistory()
                                        draft = ""
                                    },
                                )
                            }
                        }
                    }
                    if (history.isEmpty()) {
                        item(key = "empty-history", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "还没有搜索历史",
                                color = colors.onBackgroundVariant,
                                fontSize = 12.5.sp,
                                modifier = Modifier.padding(top = 18.dp),
                            )
                        }
                    }
                    items(items = history, key = { "h" + it }) { word ->
                        val rank = history.indexOf(word) + 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Capsule())
                                .clickable {
                                    draft = word
                                    submittedLocal = true
                                    onSubmit(word)
                                }
                                .padding(vertical = 9.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = rank.toString(),
                                color = if (rank <= 3) colors.primary else colors.onBackgroundVariant,
                                fontSize = 12.sp,
                                fontWeight = if (rank <= 3) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.width(20.dp),
                            )
                            Text(
                                text = word,
                                color = colors.onBackground,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = 32.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (loading && results.isEmpty()) {
                        item(key = "loading", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                TeakLoading(size = 40.dp)
                            }
                        }
                    }
                    items(items = results, key = { it.bvid }) { video ->
                        VideoGridCard(video = video, onClick = { onOpenVideo(video) })
                    }
                    error?.let { message ->
                        if (results.isEmpty()) {
                            item(key = "error", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = message,
                                    color = colors.onBackgroundVariant,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                                )
                            }
                        }
                    }
                    if (!loading && results.isEmpty() && error == null) {
                        item(key = "none", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "没有找到相关内容",
                                color = colors.onBackgroundVariant,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
