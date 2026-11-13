package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.user.UserProfile
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MineHero(profile: UserProfile, navigation: MineNavigation) {
    val shape = ContinuousCornerShape(24.dp)
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(220.dp).clip(shape)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)) {
            AsyncImage(
                model = rememberImageRequest(profile.banner.ifBlank { DEFAULT_MINE_BANNER }),
                contentDescription = null, contentScale = ContentScale.Crop,
                alignment = androidx.compose.ui.BiasAlignment(0f, profile.bannerAlignmentY),
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f)))))
        }
        Box(Modifier.fillMaxWidth().padding(top = 190.dp)) {
            MineProfileCard(profile, shape, navigation)
        }
    }
}

private const val DEFAULT_MINE_BANNER =
    "https://i0.hdslb.com/bfs/space/cb1c3ef50e22b6096fde67febe863494caefebad.png"
