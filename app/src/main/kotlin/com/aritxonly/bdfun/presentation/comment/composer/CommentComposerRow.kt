package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.material.TeakGlassIconButton

@Composable
internal fun CommentComposerRow(content: CommentComposerContent, actions: CommentComposerActions, material: CommentComposerMaterial) {
    val recipe = material.recipe
    val backdrop = material.backdrop
    Row(verticalAlignment = Alignment.CenterVertically) {
        CommentComposerInput(content, actions, material)
        Spacer(Modifier.width(8.dp))
        TeakGlassIconButton(
            onClick = actions::toggleEmoji,
            size = 46.dp,
            recipe = recipe,
            backdrop = backdrop,
        ) {
            Text(text = "\uD83D\uDE00", fontSize = 19.sp)
        }
    }

}
