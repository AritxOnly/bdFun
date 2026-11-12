package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun VideoMetadataInteractions(interaction: com.aritxonly.bdfun.feature.video.application.VideoInteractionState, actions: VideoMetadataActions) {
    val liked = interaction.liked
    val disliked = interaction.disliked
    val likedCount = interaction.likeCount
    val coined = interaction.coined
    val coinCount = interaction.coinCount
    val favorited = interaction.favorited
    val favoriteCount = interaction.favoriteCount
    val shareCount = interaction.shareCount
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ActionStat(
            modifier = Modifier.weight(1f),
            label = "点赞",
            value = likedCount,
            kind = ActionKind.ThumbUp,
            active = liked,
            onClick = { actions.toggleLike() },
            onLongPress = { actions.tripleAction() },
        )
        ActionStat(
            modifier = Modifier.weight(1f),
            label = "不喜欢",
            value = 0,
            kind = ActionKind.ThumbDown,
            active = disliked,
            onClick = { actions.toggleDislike() },
        )
        ActionStat(
            modifier = Modifier.weight(1f),
            label = "投币",
            value = coinCount,
            kind = ActionKind.Coin,
            active = coined,
            onClick = { actions.giveCoin() },
        )
        ActionStat(
            modifier = Modifier.weight(1f),
            label = "收藏",
            value = favoriteCount,
            kind = ActionKind.Star,
            active = favorited,
            onClick = { actions.toggleFavorite() },
        )
        ActionStat(
            modifier = Modifier.weight(1f),
            label = "分享",
            value = shareCount,
            kind = ActionKind.Share,
            active = false,
            onClick = { actions.shareVideo() },
        )
    }
}
