package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.live.LiveRoomInfo
import com.aritxonly.teak.common.navigation.TeakMorphMenuAnchor
import com.aritxonly.teak.common.navigation.TeakMorphMenuState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.More

@Composable
internal fun LiveTopBar(
    room: LiveRoomInfo,
    following: Boolean,
    menuState: TeakMorphMenuState,
    onBack: () -> Unit,
    onFollow: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(50.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 返回键：纯图标，不加玻璃底（压在画面上，白箭头 + 点击热区）
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.ChevronBackward,
                contentDescription = "返回",
                tint = Color.White,
                // 和收藏页等页面一致：38dp 热区 + 22dp 图标
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiveAnchorChip(room)
        }

        Spacer(Modifier.width(8.dp))
        FollowButton(following = following, onClick = onFollow, compact = true)
        Spacer(Modifier.width(4.dp))

        // 省略号：同样不加玻璃底，只保留 bdFun 的形变菜单动效
        TeakMorphMenuAnchor(menuState) { onClick ->
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MiuixIcons.More,
                    contentDescription = "更多",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

/** 主播头像 + 昵称 + 副标题，完全照 BiliPai 的裸排版：没有底板、没有玻璃。 */

@Composable
internal fun LiveAnchorChip(room: LiveRoomInfo) {
    val subtitle = buildString {
        if (room.areaText.isNotBlank()) append(room.areaText)
        if (room.onlineText.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(room.onlineText)
            append("人气")
        }
    }
    Row(
        modifier = Modifier.padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveAvatar(room, size = 30)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = room.uname.ifBlank { room.title },
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* 真实直播画面：Media3 PlayerView（控制器关闭，界面全部由我们自己画）    */
/* ------------------------------------------------------------------ */


@Composable
internal fun LiveAvatar(room: LiveRoomInfo, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(0xFF2A2D33)),
        contentAlignment = Alignment.Center,
    ) {
        if (room.face.isNotBlank()) {
            AsyncImage(
                model = room.face,
                contentDescription = room.uname,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Text(
                text = room.uname.take(1),
                color = Color.White,
                fontSize = (size / 2.4f).sp,
            )
        }
    }
}
