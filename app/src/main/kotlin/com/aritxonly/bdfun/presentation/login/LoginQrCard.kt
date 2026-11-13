package com.aritxonly.bdfun.ui

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme


@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LoginQrCard(bitmap: Bitmap?, loading: Boolean, saveQr: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .size(240.dp)
            .clip(ContinuousCornerShape(24.dp))
            .background(colors.surfaceContainerHigh)
            // 长按二维码也能保存
            .combinedClickable(
                interactionSource = null,
                indication = null,
                onClick = {},
                onLongClick = { saveQr() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = "登录二维码",
                modifier = Modifier
                    .size(208.dp)
                    .clip(ContinuousCornerShape(12.dp)),
            )
        } else if (loading) {
            TeakLoading(
                modifier = Modifier.size(28.dp),
                color = colors.primary,
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.primary)
            // 高光描边，和 App 里其它胶囊按钮一致
            .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = { saveQr() },
            )
            .padding(horizontal = 22.dp, vertical = 8.dp),
    ) {
        Text(
            text = "保存二维码",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
        )
    }

}
