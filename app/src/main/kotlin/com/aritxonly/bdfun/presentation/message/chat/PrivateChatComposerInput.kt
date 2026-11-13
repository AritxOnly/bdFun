package com.aritxonly.bdfun

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.ui.ComposerIcon
import com.aritxonly.bdfun.ui.ComposerIconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PrivateChatComposerInput(
    content: PrivateChatComposerContent, actions: PrivateChatComposerActions,
) {
    val draft = content.draft
    val pendingImage = content.pendingImage
    val sending = content.sending
    val uploading = content.uploading
    val onPickImage = actions.onPickImage
    val onClearImage = actions.onClearImage
    val onSend = actions.onSend
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PrivateChatTextField(content, actions)
        if (pendingImage != null) {
            Box(
                modifier = Modifier.padding(start = 6.dp),
            ) {
                AsyncImage(
                    model = pendingImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(colors.surfaceContainerHigh)
                        .clickable(onClick = onClearImage),
                )
            }
        }
        Spacer(Modifier.width(2.dp))
        ComposerIconButton(
            kind = ComposerIcon.Image,
            tint = colors.onBackgroundVariant,
            onClick = onPickImage,
        )
        ComposerIconButton(
            kind = ComposerIcon.Send,
            tint = if ((draft.isNotBlank() || pendingImage != null) &&
                !sending && !uploading
            ) {
                colors.primary
            } else {
                colors.outline
            },
            onClick = onSend,
        )
    }
}
