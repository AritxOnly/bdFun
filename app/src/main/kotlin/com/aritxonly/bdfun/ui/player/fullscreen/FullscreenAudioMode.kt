package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.BrandPink
import com.kyant.shapes.Capsule

@Composable
internal fun FullscreenAudioMode(
    info: FullscreenPlayerInfo,
    playback: FullscreenPlaybackState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val actions = FullscreenAudioActions(onBack, onPlayPause, onSeek, onSeekRelative)
        FullscreenAudioBackground(info.cover)
        FullscreenAudioBody(info, playback, actions)
        FullscreenAudioTopBar(info.title, onBack)
        FullscreenAudioStatus()
    }
}

private class FullscreenAudioActions(
    val onBack: () -> Unit, val onPlayPause: () -> Unit,
    val onSeek: (Long) -> Unit, val onSeekRelative: (Long) -> Unit,
)

@Composable
private fun FullscreenAudioBackground(cover: String) {
    // 不透明底：进听视频后下方 PlayerView 的视频帧不会透出来
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF08090D)))
    AsyncImage(
        model = rememberImageRequest(cover),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .blur(46.dp)
            .graphicsLayer { alpha = 0.55f },
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.25f),
                    1f to Color.Black.copy(alpha = 0.9f),
                ),
            ),
    )
}

@Composable
private fun FullscreenAudioBody(info: FullscreenPlayerInfo, playback: FullscreenPlaybackState, actions: FullscreenAudioActions) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 76.dp, vertical = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = rememberImageRequest(info.cover),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(310.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Spacer(Modifier.width(70.dp))
        Column(modifier = Modifier.weight(1f)) {
            FullscreenAudioMetadata(info)
            FullscreenAudioTimeline(playback, actions.onSeek)
            FullscreenAudioTransport(playback, actions)
        }
    }
}

@Composable
private fun FullscreenAudioMetadata(info: FullscreenPlayerInfo) {
    Text(
        text = "听视频",
        color = BrandPink,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = info.title,
        color = Color.White,
        fontSize = 26.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(9.dp))
    Text(
        text = info.author.ifBlank { "UP 主" },
        color = Color.White.copy(alpha = 0.65f),
        fontSize = 14.sp,
        maxLines = 1,
    )
    Spacer(Modifier.height(34.dp))
}

@Composable
private fun FullscreenAudioTimeline(playback: FullscreenPlaybackState, onSeek: (Long) -> Unit) {
    FullscreenSeekBar(
        positionMs = playback.positionMs,
        durationMs = playback.durationMs,
        bufferedMs = playback.bufferedMs,
        onInteract = {},
        onSeek = onSeek,
        modifier = Modifier.fillMaxWidth(),
        externalPressed = playback.scrubbing,
        externalLook = playback.seekLook,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatPlayTime(playback.positionMs),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = formatPlayTime(playback.durationMs),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun FullscreenAudioTransport(playback: FullscreenPlaybackState, actions: FullscreenAudioActions) {
    Spacer(Modifier.height(24.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.13f))
                .clickable(interactionSource = null, indication = null) {
                    actions.onSeekRelative(-10_000L)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("-10s", color = Color.White, fontSize = 10.sp)
        }
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(interactionSource = null, indication = null, onClick = actions.onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            if (playback.playing) PauseGlyph(Color.Black) else PlayGlyph(Color.Black, 26.dp)
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.13f))
                .clickable(interactionSource = null, indication = null) {
                    actions.onSeekRelative(10_000L)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("+10s", color = Color.White, fontSize = 10.sp)
        }
        Spacer(Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .height(34.dp)
                .clip(Capsule())
                .border(1.dp, Color.White.copy(alpha = 0.35f), Capsule())
                .clickable(interactionSource = null, indication = null, onClick = actions.onBack)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("退出听视频", color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
private fun BoxScope.FullscreenAudioTopBar(title: String, onBack: () -> Unit) {
    // 顶部返回和状态仍保留，方便直接退出全屏
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .padding(start = 42.dp, top = 26.dp, end = 42.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(interactionSource = null, indication = null) {
                    onBack()
                },
            contentAlignment = Alignment.Center,
        ) {
            BackChevron()
        }
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp).weight(1f),
        )
    }
}

@Composable
private fun BoxScope.FullscreenAudioStatus() {
    val settings = LocalVideoPageState.current.settings
    if (settings.showFullscreenTime) {
        FullscreenStatusLine(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 42.dp, vertical = 3.dp),
        )
    }
}
