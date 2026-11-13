package com.aritxonly.bdfun.ui

import com.kyant.shapes.Capsule
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.modal.MiuixBottomSheet
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SheetActionRow(
    label: String,
    glyph: MenuGlyphKind?,
    color: Color,
    danger: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val fill = if (danger) {
        Brush.verticalGradient(listOf(Color(0xFFE5484D), Color(0xFFC93B40)))
    } else {
        Brush.verticalGradient(
            listOf(
                com.aritxonly.bdfun.BrandPink,
                com.aritxonly.bdfun.BrandPinkDeep,
            ),
        )
    }
    val rim = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.62f), Color.White.copy(alpha = 0.10f)),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .height(46.dp)
            .clip(Capsule())
            .background(fill)
            .border(1.dp, rim, Capsule())
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (glyph != null) {
                MenuGlyph(kind = glyph, tint = Color.White, size = 18.dp)
                Spacer(Modifier.width(9.dp))
            }
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
            )
            trailing?.invoke()
        }
    }
}
