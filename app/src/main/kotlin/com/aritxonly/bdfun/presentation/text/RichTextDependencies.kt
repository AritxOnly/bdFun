package com.aritxonly.bdfun.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.aritxonly.bdfun.core.application.EmojiPanelStateHolder
import com.aritxonly.bdfun.core.application.VideoLinkTitleStore
import com.aritxonly.bdfun.presentation.EmotePresentationState

internal class RichTextDependencies(
    val emotes: EmotePresentationState, val titles: VideoLinkTitleStore,
    val createEmojiPanel: (kotlinx.coroutines.CoroutineScope) -> EmojiPanelStateHolder,
    val openVideo: (android.content.Context, String) -> Unit,
    val emoteImages: coil.ImageLoader? = null,
)
internal val LocalRichTextDependencies = staticCompositionLocalOf<RichTextDependencies> { error("富文本依赖尚未装配") }

@androidx.compose.runtime.Composable
internal fun emoteImageLoader(): coil.ImageLoader = LocalRichTextDependencies.current.emoteImages
    ?: coil.Coil.imageLoader(androidx.compose.ui.platform.LocalContext.current)
