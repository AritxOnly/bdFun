package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import com.aritxonly.bdfun.data.LiveDanmakuMapper
import com.aritxonly.bdfun.live.net.DanmakuProtocol
import com.aritxonly.bdfun.live.net.LiveDanmakuClient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.launch
import com.aritxonly.bdfun.data.LiveChatKind
import com.aritxonly.bdfun.data.LiveChatMessage
import com.aritxonly.bdfun.data.LiveMock
import com.aritxonly.bdfun.data.LiveRoomInfo
import com.aritxonly.bdfun.ui.LiveRoomScreen
import com.aritxonly.teak.common.theme.TeakTheme


/**
 * 直播详情页。
 *
 * 数据分两步：
 * 1. 房间信息 + 播放地址走真实接口（[com.aritxonly.bdfun.data.BiliApi.liveRoomInfo] /
 *    [com.aritxonly.bdfun.data.BiliApi.livePlayUrl]），失败时退回 mock，不会白屏。
 * 2. 弹幕暂时还是本地假数据，等原生弹幕长连接做好再替换。
 */
class LiveRoomActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 直播间是深色背景：状态栏/导航栏图标固定用白色
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        AppGraph.ensure(applicationContext)
        val settings = AppGraph.settings
        val api = AppGraph.api
        val requestedRoomId = intent.getLongExtra(EXTRA_ROOM_ID, 0L)

        setContent {
            // 直播间始终使用深色主题：播放器区域的文字和控件都要有深色底。
            TeakTheme(
                dark = true,
                lightColors = hyperLightColors(),
                darkColors = hyperDarkColors(),
                materialColors = darkColorScheme(
                    primary = BrandPink,
                    background = Color(0xFF0D0E11),
                    surface = Color(0xFF121317),
                    surfaceVariant = Color(0xFF1A1C21),
                    onSurface = Color(0xFFF2F3F5),
                    outline = Color(0xFF2B2E35),
                ),
                blurEnabled = settings.blurEnabled,
                refractionEnabled = settings.refractionEnabled,
                fontScale = settings.fontScale,
            ) {
                val context = LocalContext.current
                var room by remember { mutableStateOf<LiveRoomInfo?>(null) }
                var playUrl by remember { mutableStateOf<String?>(null) }
                var error by remember { mutableStateOf<String?>(null) }
                var messages by remember { mutableStateOf<List<LiveChatMessage>>(emptyList()) }
                var danmakuConnected by remember { mutableStateOf(false) }
                var currentRoomId by remember { mutableLongStateOf(0L) }
                var qualities by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
                var liveEmotes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
                var currentQn by remember { androidx.compose.runtime.mutableIntStateOf(10000) }
                var danmakuEnabled by remember { mutableStateOf(true) }
                var playing by remember { mutableStateOf(true) }
                var danmakuReloadTick by remember { mutableStateOf(0) }

                // 弹幕长连接：真实 WebSocket（鉴权 + 心跳 + Brotli 解压）
                val scope = rememberCoroutineScope()
                val danmakuClient = remember {
                    LiveDanmakuClient(scope, webSocketFactory = api.webSocketClient)
                }
                DisposableEffect(Unit) {
                    onDispose {
                        danmakuClient.disconnect()
                    }
                }

                // 播放器：进入就建好，离开页面释放
                val player = remember {
                    ExoPlayer.Builder(context.applicationContext).build().apply {
                        playWhenReady = true
                    }
                }
                DisposableEffect(Unit) {
                    onDispose { player.release() }
                }

                // 拿房间信息 + 播放地址
                LaunchedEffect(requestedRoomId) {
                    val roomId = if (requestedRoomId > 0L) {
                        requestedRoomId
                    } else {
                        runCatching { api.liveHotRoomId() }.getOrNull() ?: LiveMock.room.roomId
                    }
                    val info = runCatching { api.liveRoomInfo(roomId) }.getOrNull()
                    val portraitBg = runCatching { api.livePortraitBackground(roomId) }.getOrNull()
                    room = (info ?: LiveMock.room.copy(roomId = roomId)).let {
                        if (portraitBg.isNullOrBlank()) it else it.copy(cover = portraitBg)
                    }
                    currentRoomId = roomId
                    qualities = runCatching { api.liveQualities(roomId) }.getOrDefault(emptyList())
                    val url = runCatching { api.livePlayUrl(roomId, currentQn) }.getOrNull()
                    playUrl = url
                    if (url == null) error = "直播流拿不到，可能未开播或接口变动"
                }

                // 房间表情包：滚动弹幕里的 [表情] 需要靠它换成图片
                LaunchedEffect(currentRoomId) {
                    val roomId = currentRoomId
                    if (roomId <= 0L) return@LaunchedEffect
                    liveEmotes = runCatching { api.liveEmoticons(roomId) }
                        .getOrDefault(emptyMap())
                }

                // 接真实弹幕
                LaunchedEffect(currentRoomId, danmakuReloadTick) {
                    val roomId = currentRoomId
                    if (roomId <= 0L) return@LaunchedEffect
                    danmakuClient.disconnect()
                    val info = runCatching { api.liveDanmuInfo(roomId) }.getOrNull()
                    if (info == null) {
                        com.aritxonly.bdfun.data.LogCollector.record("LIVE", "danmuInfo=null room=$roomId")
                        return@LaunchedEffect
                    }
                    com.aritxonly.bdfun.data.LogCollector.record("LIVE", "danmuInfo ok room=$roomId hosts=${info.second.size}")
                    val (token, urls) = info
                    // 和 BiliPai 一致：只有带 SESSDATA 时才带账号 uid，否则退回游客 uid=0
                    val uid = if (api.isLoggedIn) (AppGraph.session.value?.mid ?: 0L) else 0L
                    danmakuClient.connect(urls, token, roomId, uid)
                    com.aritxonly.bdfun.data.LogCollector.record(
                        "LIVE",
                        "ws connect urls=${urls.size} first=${urls.firstOrNull()} uid=$uid",
                    )
                    var logged = 0
                    danmakuClient.messageFlow.collect { packet ->
                        if (logged < 6) {
                            com.aritxonly.bdfun.data.LogCollector.record(
                                "LIVE",
                                "packet op=${packet.operation} len=${packet.body.size}",
                            )
                            logged++
                        }
                        if (packet.operation != DanmakuProtocol.OP_MESSAGE) return@collect
                        if (!danmakuConnected) {
                            // 第一条真实弹幕到达时清掉占位假消息，之后只显示真实内容
                            messages = emptyList()
                            danmakuConnected = true
                        }
                        val body = String(packet.body, Charsets.UTF_8)
                        LiveDanmakuMapper.toMessage(body, System.nanoTime())?.let { msg ->
                            messages = (messages + msg).takeLast(MAX_MESSAGES)
                        }
                    }
                }

                LaunchedEffect(playUrl, player) {
                    val url = playUrl ?: return@LaunchedEffect
                    runCatching {
                        player.setMediaItem(MediaItem.fromUri(url))
                        player.prepare()
                        player.play()
                        playing = true
                    }.onFailure { error = it.message ?: "播放失败" }
                }

                // 播放中保持屏幕常亮；暂停后恢复系统息屏，避免挂着直播一直亮屏。
                DisposableEffect(playing) {
                    val flag = android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    if (playing) window.addFlags(flag) else window.clearFlags(flag)
                    onDispose { window.clearFlags(flag) }
                }

                BackHandler {
                    if (resources.configuration.orientation ==
                        android.content.res.Configuration.ORIENTATION_LANDSCAPE
                    ) {
                        requestedOrientation =
                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        finish()
                    }
                }

                // 直播重载 / 切换清晰度：重新取流并重启弹幕连接。
                // 不能只改 playUrl：同一个地址赋回去 Compose 不会重新触发播放。
                val playLiveQuality: (Int) -> Unit = { qn ->
                    error = null
                    scope.launch {
                        val url = runCatching { api.livePlayUrl(currentRoomId, qn) }.getOrNull()
                        if (url != null) {
                            currentQn = qn
                            playUrl = url
                            runCatching {
                                player.setMediaItem(MediaItem.fromUri(url))
                                player.prepare()
                                player.play()
                            }.onFailure { error = it.message ?: "播放失败" }
                            playing = true
                            // 重载后旧弹幕属于上一条流，清掉并重新拉长连接
                            messages = emptyList()
                            danmakuConnected = false
                            danmakuReloadTick++
                        } else {
                            error = "直播重载失败，请稍后再试"
                        }
                    }
                }

                LiveRoomScreen(
                    room = room ?: LiveMock.room.copy(roomId = requestedRoomId),
                    messages = messages,
                    onSend = { text ->
                        messages = (
                            messages + LiveChatMessage(
                                id = System.currentTimeMillis(),
                                kind = LiveChatKind.DANMAKU,
                                uname = "我",
                                text = text,
                                nameColor = 0xFF6699L,
                            )
                            ).takeLast(MAX_MESSAGES)
                    },
                    onBack = { finish() },
                    player = if (playUrl != null) player else null,
                    statusText = error,
                    qualities = qualities,
                    currentQuality = currentQn,
                    roomEmotes = liveEmotes,
                    onSelectQuality = { qn ->
                        if (qn != currentQn) playLiveQuality(qn)
                    },
                    isPlaying = playing,
                    onTogglePlay = {
                        playing = !playing
                        if (playing) player.play() else player.pause()
                    },
                    danmakuEnabled = danmakuEnabled,
                    onToggleDanmaku = { danmakuEnabled = !danmakuEnabled },
                    onReload = { playLiveQuality(currentQn) },
                    onToggleFullscreen = {
                        requestedOrientation =
                            if (resources.configuration.orientation ==
                                android.content.res.Configuration.ORIENTATION_LANDSCAPE
                            ) {
                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            }
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_ROOM_ID = "room_id"
        private const val MAX_MESSAGES = 300
    }
}
