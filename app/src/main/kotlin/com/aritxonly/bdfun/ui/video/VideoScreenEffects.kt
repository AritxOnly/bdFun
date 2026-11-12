package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
internal fun BindVideoScreenEffects(
    request: VideoScreenRequest, content: VideoScreenContent,
    holders: VideoScreenStateHolders, actions: VideoDetailViewActions, pager: PagerState,
) {
    val detail = content.read.detail
    val currentCid = content.selection.cid
    val controllers = holders.controllers
    val ui = holders.ui
    var fullscreen by ui.fullscreen
    BindVideoAuthor(detail, content.loggedIn, holders.metadata, controllers.interaction, ui, holders.description)
    LaunchedEffect(content.interaction) {
        val current = content.interaction
        val d = current.detail ?: return@LaunchedEffect
        controllers.readCache.updateVideo(d.bvid) { cached ->
            cached.copy(liked = current.liked, like = current.likeCount, coined = current.coined,
                coin = current.coinCount, favorited = current.favorited, favorite = current.favoriteCount)
        }
    }
    BackHandler {
        when {
            fullscreen -> fullscreen = false
            else -> actions.leaveVideo()
        }
    }
    LoadInitialVideo(request.video, request.defaultQuality, controllers, ui)
    BindVideoPageMetadata(detail, currentCid, content.loggedIn, holders.metadata, controllers.interaction, ui)
    RegisterVideoDanmakuSender(detail, currentCid, holders.metadata, controllers.detail)
    BindVideoFullscreenState(ui)
    BindBackgroundVideoMetadata(request.video, detail, currentCid, content.quality)
    ReconcileVideoPager(pager)
}
