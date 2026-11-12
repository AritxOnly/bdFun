package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.data.Quality
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
internal fun FullscreenPanelHost(
    panel: FullscreenPanel,
    info: FullscreenPlayerInfo,
    playback: FullscreenPlaybackState,
    actions: FullscreenActions,
    onClose: () -> Unit,
    visible: Boolean = true,
) {

    var shown by remember { mutableStateOf(panel) }
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { revealed = visible }
    val reveal = rememberManualProgress(if (revealed) 1f else 0f, 264f)
    LaunchedEffect(panel) {
        if (panel != FullscreenPanel.None) shown = panel
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val panelWidth = (maxWidth.value * 0.41f).dp.coerceAtLeast(360.dp)
        // 左透明、右深黑：与参考图三一致，不铺满一块纯黑
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.25f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.25f),
                        0.58f to Color.Black.copy(alpha = 0.74f),
                        0.68f to Color.Black.copy(alpha = 0.95f),
                        1f to Color.Black,
                    ),
                )
                .graphicsLayer { alpha = reveal }
                .clickable(interactionSource = null, indication = null, onClick = onClose),
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(panelWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    alpha = reveal
                    translationX = (1f - reveal) * 90f
                }
                .clickable(interactionSource = null, indication = null) {}
                .padding(start = 32.dp, end = 30.dp, top = 30.dp, bottom = 18.dp),
        ) {
            when (shown) {
                FullscreenPanel.Favorite -> FavoritePanel(info, actions, onClose)
                FullscreenPanel.More -> MorePanel(info, playback, actions, onClose)
                FullscreenPanel.Danmaku -> DanmakuPanel(info, onClose)
                FullscreenPanel.Episodes -> EpisodesPanel(info, actions, onClose)
                FullscreenPanel.Speed -> SpeedPanel(playback, actions, onClose)
                FullscreenPanel.Quality -> QualityPanel(info, playback, actions, onClose)
                FullscreenPanel.Cache -> CachePanel(playback, actions, onClose)
                FullscreenPanel.Subtitles -> SubtitlesPanel(info, actions, onClose)
                FullscreenPanel.None -> Unit
            }
        }
    }
}

@Composable
internal fun PanelHeader(title: String, onClose: () -> Unit) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(interactionSource = null, indication = null, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            CloseGlyph()
        }
    }
}

@Composable
internal fun PanelCard(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            content = content,
        )
    }
}
