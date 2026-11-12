package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.kyant.shapes.Capsule

@Composable
internal fun FavoritePanel(
    info: FullscreenPlayerInfo,
    actions: FullscreenActions,
    onClose: () -> Unit,
) {

    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader("选择收藏夹", onClose)
        Spacer(Modifier.height(14.dp))
        if (info.folders.isEmpty()) {
            Text(
                text = if (info.loggedIn) "正在读取收藏夹…" else "登录后可选择收藏夹",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            info.folders.forEach { folder ->
                val checked = info.folderIds.contains(folder.id)
                FavoriteFolderOption(folder, checked, actions.onToggleFolder)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(Capsule())
                .background(Color.White.copy(alpha = 0.15f))
                .clickable(interactionSource = null, indication = null, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Text("完成", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Normal)
        }
    }
}


@Composable
private fun FavoriteFolderOption(folder: com.aritxonly.bdfun.data.FavoriteFolder, checked: Boolean, onToggleFolder: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = null, indication = null) {
                onToggleFolder(folder.id)
            }
            .padding(vertical = 13.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.title,
                color = Color.White,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${folder.count} 个内容 · ${if (checked) "已收藏" else "公开"}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(
                    width = 1.5.dp,
                    color = if (checked) BrandPink else Color.White.copy(alpha = 0.5f),
                    shape = CircleShape,
                )
                .background(if (checked) BrandPink else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                CheckGlyph()
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.6.dp)
            .background(Color.White.copy(alpha = 0.08f)),
    )
}
