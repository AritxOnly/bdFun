package com.aritxonly.bdfun

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun SpaceContentEffects(
    runtime: SpaceScreenRuntime, actions: SpaceContentActions,
    onTitleChange: (String) -> Unit, onBannerDarkChange: (Boolean) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val content = runtime.content
    val controller = runtime.controller
    val isSelf = content.isSelf
    val loggedIn = content.loggedIn
    val snapshot = content.snapshot
    val ui = content.ui
    val banner = content.banner
    val profile = content.snapshot.profile.space?.profile
    LaunchedEffect(controller, isSelf) {
        controller.load(isSelf, loggedIn)
        if (BuildConfig.DEBUG) {
            val dynamics = controller.state.value.dynamics
            com.aritxonly.bdfun.data.LogCollector.record("SPACE",
                "dynamics mid=${content.mid} count=${dynamics.items.size} end=${dynamics.end} error=${dynamics.error}")
        }
    }
    LaunchedEffect(controller, snapshot.profile.settled, profile) {
        if (!snapshot.profile.settled && profile == null) return@LaunchedEffect
        val urls = (listOf(profile?.banner.orEmpty()) + profile?.bannerFallbacks.orEmpty() + DEFAULT_SPACE_BANNER)
            .filter { it.isNotBlank() }.distinct()
        var bitmap: android.graphics.Bitmap? = null
        for (url in urls) {
            bitmap = loadSpaceBannerBitmap(context, url)
            if (bitmap != null) break
        }
        banner.bitmap = bitmap ?: banner.bitmap
        if (bitmap != null && !bitmap.isRecycled) onBannerDarkChange(bitmapLuminance(bitmap) < 0.5f)
        banner.ready = true
    }

    LaunchedEffect(profile?.name) {
        onTitleChange(profile?.name.orEmpty().ifBlank { "个人主页" })
    }

    // 切到对应 tab 时补拉数据。
    LaunchedEffect(controller, ui.selectedTab, ui.submissionMode) {
        val current = controller.state.value
        when {
            ui.selectedTab == 2 && ui.submissionMode == 0 && current.videos.items.isEmpty() -> actions.loadVideos(1)
            ui.selectedTab == 2 && ui.submissionMode == 1 && current.articles.items.isEmpty() -> actions.loadArticles(1)
            ui.selectedTab == 3 && current.shop.items.isEmpty() -> actions.loadShop()
        }
    }

}
