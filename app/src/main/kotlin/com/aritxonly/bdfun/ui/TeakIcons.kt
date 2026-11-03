package com.aritxonly.bdfun.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.R

/**
 * 用户提供的图标资源统一在这里取用。
 * 素材都是「白色图形 + 透明底」，所以颜色一律用 tint 覆盖，同一张图能同时适配深浅色。
 */
@Composable
internal fun ResourceGlyph(
    @DrawableRes res: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}

internal object TeakIcon {
    val Like = R.drawable.act_like
    val Dislike = R.drawable.act_dislike
    val LikeOutline = R.drawable.act_like_outline
    val DislikeOutline = R.drawable.act_dislike_outline
    val Coin = R.drawable.act_coin
    val Star = R.drawable.act_star
    val Share = R.drawable.act_share

    val CommentBubble = R.drawable.comment_bubble
    val CommentLike = R.drawable.comment_like
    val CommentLikeFilled = R.drawable.comment_like_filled
    val CommentDislike = R.drawable.comment_dislike
    val CommentDislikeFilled = R.drawable.comment_dislike_filled

    val MetricPlay = R.drawable.ic_metric_play
    val MetricDanmaku = R.drawable.ic_metric_danmaku

    val Fullscreen = R.drawable.ic_fullscreen
    val Play = R.drawable.fs_play
    val SeekThumb = R.drawable.ic_seek_thumb

    // 用户为本轮全屏播放器提供的图标（黑底白图已转透明 PNG）
    val PlayerPlay = R.drawable.fs_play
    val PlayerDanmakuOn = R.drawable.fs_danmaku_on
    val PlayerDanmakuOff = R.drawable.fs_danmaku_off
    val PlayerDanmakuSettings = R.drawable.fs_danmaku_settings
    val PlayerNext = R.drawable.fs_next
    val PlayerLike = R.drawable.fs_like
    val PlayerComment = R.drawable.fs_comment
    val PlayerStar = R.drawable.fs_star
    val PlayerShare = R.drawable.fs_share
    val SeekNormal = R.drawable.fs_seek_normal
    val SeekLeft = R.drawable.fs_seek_left
    val SeekRight = R.drawable.fs_seek_right

    val Send = R.drawable.ic_send
    val Image = R.drawable.ic_image
}
