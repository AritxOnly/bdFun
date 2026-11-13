package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.application.videoTextTokens
import com.aritxonly.bdfun.core.application.displayVideoTextToken
import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.application.LinkTitlesStateHolder
import com.aritxonly.bdfun.core.model.video.ResolvedVideoLink

@Composable
internal fun rememberBiliLinkTitles(text: String): Map<String, ResolvedVideoLink> {
    val tokens = remember(text) { videoTextTokens(text) }
    if (tokens.isEmpty()) return emptyMap()
    val store = LocalRichTextDependencies.current.titles
    val generation by store.generation.collectAsState()
    val controller = remember(text, store, generation) { LinkTitlesStateHolder(text, store) }
    val state by key(controller) { controller.state.collectAsState() }
    DisposableEffect(controller) { onDispose { controller.close() } }
    LaunchedEffect(controller) { controller.load() }
    return state
}

internal fun biliDisplayText(token: String, resolved: Map<String, ResolvedVideoLink>) = displayVideoTextToken(token, resolved)
