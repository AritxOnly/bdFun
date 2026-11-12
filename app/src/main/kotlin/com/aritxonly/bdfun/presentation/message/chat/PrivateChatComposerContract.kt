package com.aritxonly.bdfun

internal data class PrivateChatComposerContent(
    val draft: String, val showEmoji: Boolean, val pendingImage: android.net.Uri?,
    val sending: Boolean, val uploading: Boolean,
)
internal class PrivateChatComposerActions(
    val onDraftChange: (String) -> Unit, val onToggleEmoji: () -> Unit, val onPickEmoji: (String) -> Unit,
    val onPickImage: () -> Unit, val onClearImage: () -> Unit, val onSend: () -> Unit,
)
internal data class PrivateChatComposerMaterial(
    val recipe: com.aritxonly.teak.common.material.GlassMaterialRecipe,
    val backdrop: top.yukonga.miuix.kmp.blur.Backdrop?,
)
