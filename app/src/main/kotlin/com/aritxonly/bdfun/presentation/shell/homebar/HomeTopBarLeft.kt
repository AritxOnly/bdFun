package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.TeakGlassBackButton

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp


@Composable
internal fun HomeTopBarLeft(content: HomeTopBarContent, search: HomeTopBarSearch, actions: HomeTopBarActions) {
    val searchActive = search.active
    val profile = content.profile
    val onCancelSearch = actions.onCancelSearch
    val onOpenProfile = actions.onOpenProfile
    Crossfade(
        targetState = searchActive,
        animationSpec = tween(durationMillis = 160),
        label = "left-slot",
    ) { searching ->
        if (searching) {
            // 搜索态返回键：和顶栏/底栏同一套玻璃（真模糊）
            TeakGlassBackButton(
                onClick = onCancelSearch,
                revealed = true,
                size = 38.dp,
            )
        } else {
            TeakAvatar(faceUrl = profile?.face, onClick = onOpenProfile, size = 38.dp)
        }
    }
}
