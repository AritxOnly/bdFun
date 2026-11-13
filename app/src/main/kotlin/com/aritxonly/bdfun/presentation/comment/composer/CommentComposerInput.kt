package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.material.teakContentControlShadow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.kyant.shapes.Capsule

@Composable
internal fun androidx.compose.foundation.layout.RowScope.CommentComposerInput(
    content: CommentComposerContent, actions: CommentComposerActions, material: CommentComposerMaterial,
) {
    val recipe = material.recipe
    val backdrop = material.backdrop
    TeakGlassSurface(
        modifier = Modifier.weight(1f).height(46.dp).teakContentControlShadow(Capsule()),
        shape = Capsule(),
        recipe = recipe,
        backdrop = backdrop,
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CommentComposerTextField(content, actions)
            CommentComposerActionRow(content, actions)
        }
    }

}
