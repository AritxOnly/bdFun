package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.aritxonly.bdfun.BrandPink

internal class FullscreenChromeAssembly(val appearance: FullscreenAppearance, val slots: FullscreenChromeSlots)

/** 原app资源、图片请求及尚在迁移的内容只在组装壳接线，不进入视频模块。 */
@Composable
internal fun rememberFullscreenChromeAssembly(): FullscreenChromeAssembly {
    val settings = LocalVideoPageState.current.settings
    val icons = remember {
        FullscreenIconAssets(TeakIcon.PlayerPlay, TeakIcon.PlayerNext, TeakIcon.PlayerDanmakuOn,
            TeakIcon.PlayerDanmakuOff, TeakIcon.PlayerDanmakuSettings,
            TeakIcon.PlayerLike, TeakIcon.PlayerComment, TeakIcon.PlayerStar, TeakIcon.PlayerShare)
    }
    val slots = remember {
        FullscreenChromeSlots(
            avatar = { face, modifier ->
                AsyncImage(model = rememberImageRequest(face), contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = modifier)
            },
            audio = { info, playback, actions, modifier ->
                FullscreenAudioMode(info = info, playback = playback,
                    onBack = { actions.onAudioOnlyChanged(false) }, onPlayPause = actions.onPlayPause,
                    onSeek = actions.onSeek, onSeekRelative = actions.onSeekRelative, modifier = modifier)
            },
            danmaku = { info, close -> DanmakuPanel(info, close) },
        )
    }
    return FullscreenChromeAssembly(FullscreenAppearance(BrandPink, icons,
        settings.showFullscreenActions, settings.showViewerCount, settings.showFullscreenTime), slots)
}
