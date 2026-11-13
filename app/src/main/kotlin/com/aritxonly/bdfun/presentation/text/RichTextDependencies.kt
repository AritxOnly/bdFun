package com.aritxonly.bdfun.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.aritxonly.bdfun.core.application.EmojiPanelStateHolder
import com.aritxonly.bdfun.core.application.VideoLinkTitleStore
import com.aritxonly.bdfun.presentation.EmotePresentationState

internal class RichTextDependencies(
    val emotes: EmotePresentationState, val titles: VideoLinkTitleStore,
    val createEmojiPanel: () -> EmojiPanelStateHolder,
    val openVideo: (android.content.Context, String) -> Unit,
)
internal val LocalRichTextDependencies = staticCompositionLocalOf<RichTextDependencies> { error("富文本依赖尚未装配") }
