package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable

@Composable
internal fun videoDetailEffectPorts(): VideoDetailEffectPorts {
    val page = LocalVideoPageState.current
    val background = page.dependencies.background
    return VideoDetailEffectPorts(page, { page.dependencies.sentHistory },
        publishFullscreen = { page.playerUi.fullscreen = it },
        updateBackground = { item, detail, cid, quality ->
            background.updateInfo(video = item, title = item.title, author = item.ownerName, cover = item.cover)
            background.updateSession(video = item, detail = detail, currentCid = cid, qualityId = quality)
        })
}
