package com.aritxonly.bdfun

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.CdnAutoSelector
import com.aritxonly.bdfun.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var api: BiliApi
    private lateinit var settings: SettingsStore

    private var lastHandledLink = ""
    private var lastClipText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.aritxonly.bdfun.data.LogCollector.init(applicationContext)
        enableEdgeToEdge()
        // 采样上限：底栏玻璃每帧要把整屏录一次，绘制频率跟屏幕刷新率一致。
        // 120Hz 屏上把整窗钉在 60Hz，录制次数直接减半、每帧预算翻倍；60Hz 设备上是 no-op。
        // 不想要这个整窗限帧，删掉下面这 3 行即可。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes = window.attributes.apply { preferredRefreshRate = 60f }
        }
        api = BiliApi(applicationContext)
        settings = SettingsStore(applicationContext)
        AppGraph.init(api = api, settings = settings, context = applicationContext)
        // 自动 CDN 在冷启动先沿用上次结果，同时在后台重新测速；不阻塞首屏和播放。
        if (settings.cdnService == CdnAutoSelector.ID) {
            lifecycleScope.launch(Dispatchers.IO) {
                CdnAutoSelector.refresh(api)
            }
        }
        handleIntent(intent)
        setContent {
            androidx.compose.foundation.layout.Box {
                BdFunApp(api = api, settings = settings)
                com.aritxonly.bdfun.ui.SplashWallpaper(settings.splashWallpaper)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        handleClipboard()
    }

    private fun handleIntent(intent: Intent?) {
        val raw = intent?.data?.toString()
            ?: intent?.getStringExtra(Intent.EXTRA_TEXT)
            ?: return
        openLink(raw)
    }

    private fun handleClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (text.isBlank() || text == lastClipText) return
        lastClipText = text
        if (BiliLink.isVideoLink(text)) openLink(text)
    }

    private fun openLink(text: String) {
        if (text.isBlank() || text == lastHandledLink) return
        if (!BiliLink.isVideoLink(text)) return
        lastHandledLink = text
        Thread {
            val bvid = BiliLink.resolve(text)
            if (bvid != null) {
                runOnUiThread { TeakNavigator.openVideoByBvid(this, bvid) }
            }
        }.start()
    }
}
