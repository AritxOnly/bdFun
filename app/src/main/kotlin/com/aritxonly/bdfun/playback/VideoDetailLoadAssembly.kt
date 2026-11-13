package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable

@Composable
internal fun rememberVideoDetailStatePorts(): VideoDetailStatePorts {
    val page = LocalVideoPageState.current
    return VideoDetailStatePorts(page.dependencies.controllers, page.readCache,
        page.dependencies.progress, page.settings)
}

@Composable
internal fun rememberVideoDetailLoadPorts(): VideoDetailLoadPorts {
    val page = LocalVideoPageState.current
    return VideoDetailLoadPorts({ page.dependencies.downloads }, page.settings, page.dependencies.log)
}
