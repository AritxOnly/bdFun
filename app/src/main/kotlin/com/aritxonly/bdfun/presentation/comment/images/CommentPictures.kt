package com.aritxonly.bdfun.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CommentPictures(urls: List<String>, onOpen: (Int) -> Unit) {

    val colors = MiuixTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        urls.take(3).forEachIndexed { index, url ->
            AsyncImage(
                model = rememberImageRequest(url),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceContainerHigh)
                    .clickable(interactionSource = null, indication = null) { onOpen(index) },
            )
        }
        if (urls.size > 3) {
            Text(
                text = "+${urls.size - 3}",
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
            )
        }
    }
}
