package com.aritxonly.bdfun.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private class AndroidVideoDetailActionHost(
    private val context: Context, private val page: VideoPagePresentationState,
) : VideoDetailActionHost {
    override val settings get() = page.settings
    override val downloads get() = page.dependencies.downloads
    override var progressMs: Long
        get() = page.danmakuProgressMs
        set(value) { page.danmakuProgressMs = value }
    override fun showToast(text: String) = context.showToast(text)
    override fun share(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "分享视频"))
    }
    override fun requestHandOff() = page.dependencies.background.requestHandOff()
}

@Composable
internal fun videoDetailActionHost(): VideoDetailActionHost =
    AndroidVideoDetailActionHost(LocalContext.current, LocalVideoPageState.current)
