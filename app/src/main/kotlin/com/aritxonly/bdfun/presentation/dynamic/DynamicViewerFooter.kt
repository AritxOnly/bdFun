package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.feed.DynamicItem

@Composable
internal fun DynamicViewerFooter(item: DynamicItem, liked: Boolean, dismissAnimated: () -> Unit, onLike: () -> Unit, onComment: () -> Unit, onRepost: () -> Unit) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 26.dp),
    ) {
        DynamicViewerAuthor(item, dismissAnimated)
        if (item.text.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = item.text,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(12.dp))
        DynamicViewerActions(item, liked, onLike, onComment, onRepost)
    }
}
