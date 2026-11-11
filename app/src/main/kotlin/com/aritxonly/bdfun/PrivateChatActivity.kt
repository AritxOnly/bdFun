package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.ChatMessage
import com.aritxonly.bdfun.data.MessageSession
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.bdfun.ui.ComposerIcon
import com.aritxonly.bdfun.ui.ComposerIconButton
import com.aritxonly.bdfun.ui.EmojiPanel
import com.aritxonly.bdfun.ui.EmoteCache
import com.aritxonly.bdfun.ui.EmoteText
import com.aritxonly.bdfun.ui.TeakActivityScaffold
import com.aritxonly.bdfun.ui.formatCount
import com.aritxonly.bdfun.ui.rememberImageRequest
import com.aritxonly.bdfun.ui.showToast
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.teak.common.material.TeakGlassIconButton
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.kyant.shapes.Capsule
import kotlinx.coroutines.launch
import org.json.JSONObject
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PrivateChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val talkerId = intent?.getLongExtra("talker_id", 0L) ?: 0L
        val sessionType = intent?.getIntExtra("session_type", 1) ?: 1
        val name = intent?.getStringExtra("name").orEmpty().ifBlank { "私信" }
        val avatar = intent?.getStringExtra("avatar").orEmpty()
        com.aritxonly.bdfun.data.LogCollector.record(
            "CHAT",
            "open talker=$talkerId type=$sessionType name=$name",
        )
        val session = MessageSession(
            id = "1_$talkerId",
            talkerId = talkerId,
            sessionType = sessionType,
            name = name,
            avatar = avatar,
            preview = "",
            time = 0L,
            unread = intent?.getIntExtra("unread", 0) ?: 0,
            isSystem = false,
            maxSeqno = intent?.getLongExtra("max_seqno", 0L) ?: 0L,
        )
        setContent {
            PrivateChatRoot(api = AppGraph.api, session = session)
        }
    }
}

@Composable
private fun PrivateChatRoot(api: BiliApi, session: MessageSession) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var reloadTick by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf(false) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var showEmoji by remember { mutableStateOf(false) }
    var pendingImage by remember { mutableStateOf<android.net.Uri?>(null) }
    var myAvatar by remember { mutableStateOf("") }
    var historyCursor by remember { mutableStateOf<Long?>(null) }
    var historyLoading by remember { mutableStateOf(false) }
    var historyError by remember { mutableStateOf<String?>(null) }
    suspend fun acknowledgeLatest() {
        val seq = messages.maxOfOrNull { it.sequence } ?: 0L
        if (seq > 0) api.markSessionsRead(listOf(session.copy(unread = 1, maxSeqno = seq)))
    }
    suspend fun refreshLatest() {
        val page = api.privateMessagePage(session.talkerId, session.sessionType)
        val updates = page.items.associateBy { it.id }
        messages = (messages.filterNot { it.id in updates } + page.items).sortedBy { it.sequence }
        if (listState.firstVisibleItemIndex == 0) acknowledgeLatest()
    }
    fun loadHistory() {
        val cursor = historyCursor ?: return
        if (historyLoading) return
        historyLoading = true
        historyError = null
        scope.launch {
            try {
                val page = api.privateMessagePage(session.talkerId, session.sessionType, before = cursor)
                messages = (page.items + messages).distinctBy { it.id }.sortedBy { it.sequence }
                historyCursor = page.next
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { historyError = e.message ?: "历史消息加载失败" }
            finally { historyLoading = false }
        }
    }

    LaunchedEffect(Unit) { EmoteCache.ensure(api) }

    LaunchedEffect(session.talkerId, reloadTick) {
        loading = true
        loadError = false
        var loaded: List<ChatMessage>? = null
        // 私信接口偶发超时；连续重试 3 次，避免一进来就永远空着。
        for (attempt in 0 until 3) {
            val result = runCatching {
                api.privateMessagePage(session.talkerId, session.sessionType)
            }
            if (result.isSuccess) {
                val page = result.getOrThrow()
                loaded = page.items
                historyCursor = page.next
                break
            }
            if (attempt < 2) kotlinx.coroutines.delay(400)
        }
        if (loaded != null) {
            messages = loaded
        } else {
            loadError = true
        }
        com.aritxonly.bdfun.data.LogCollector.record(
            "CHAT",
            "loaded talker=${session.talkerId} count=${messages.size} error=$loadError",
        )
        loading = false
        if (loaded != null) runCatching { acknowledgeLatest() }
        myAvatar = runCatching { api.nav()?.face.orEmpty() }.getOrDefault("")
    }

    // 列表用 reverseLayout，本身就从底部开始；新消息进来直接贴底，不做位移动画。
    LaunchedEffect(messages.lastOrNull()?.id) {
        if (!loading && messages.isNotEmpty() && listState.firstVisibleItemIndex < 2) listState.scrollToItem(0)
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, session.talkerId) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            while (true) {
                kotlinx.coroutines.delay(8000)
                if (!loading && !sending && !loadError) {
                    try { refreshLatest() }
                    catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (_: Exception) { /* Keep readable history during a transient failure. */ }
                }
            }
        }
    }

    // 键盘弹出时，除了输入框上移，聊天记录也贴到最新一条。
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottom) {
        if (imeBottom > 0 && messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) pendingImage = uri
    }

    fun send() {
        if (sending || uploading) return
        val value = draft.trim()
        val image = pendingImage
        if (value.isBlank() && image == null) return
        sending = true
        scope.launch {
            try {
                if (image != null) {
                    uploading = true
                    val uploaded = JSONObject(api.uploadCommentImage(image))
                    val imageUrl = uploaded.optString("img_src")
                    check(imageUrl.isNotBlank()) { "图片上传失败" }
                    val content = JSONObject().put("url", imageUrl)
                        .put("width", uploaded.optInt("img_width")).put("height", uploaded.optInt("img_height"))
                        .put("size", uploaded.optDouble("img_size", 0.0)).put("original", 1).toString()
                    check(api.sendPrivateMessage(session.talkerId, "", session.sessionType, content)) { "图片发送失败" }
                    pendingImage = null // If text fails afterwards, don't send the picture a second time on retry.
                    uploading = false
                }
                if (value.isNotBlank()) {
                    check(api.sendPrivateMessage(session.talkerId, value, session.sessionType)) { "文字发送失败，请重试" }
                    if (draft.trim() == value) draft = ""
                }
                showEmoji = false
                try { refreshLatest(); listState.scrollToItem(0) }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { context.showToast("已发送，消息列表稍后更新") }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { context.showToast(e.message ?: "发送失败，请稍后重试") }
            finally { sending = false; uploading = false }
        }
    }

    TeakActivityScaffold(
        settings = AppGraph.settings,
        onBack = { (context as? android.app.Activity)?.finish() },
        title = session.name.ifBlank { session.talkerId.toString() },
        backRevealed = true,
        titleAlwaysVisible = true,
        overlay = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                PrivateChatComposer(
                    api = api,
                    draft = draft,
                    onDraftChange = { draft = it },
                    showEmoji = showEmoji,
                    onToggleEmoji = { showEmoji = !showEmoji },
                    onPickEmoji = { draft = draft + it },
                    pendingImage = pendingImage,
                    onPickImage = { imagePicker.launch("image/*") },
                    onClearImage = { pendingImage = null },
                    onSend = { send() },
                    sending = sending,
                    uploading = uploading,
                )
            }
        },
    ) {
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "加载中…",
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = 13.sp,
                )
            }
        } else if (loadError) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "消息加载失败",
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "点击重试",
                        color = MiuixTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { reloadTick++ }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 66.dp,
                    bottom = 118.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(messages.asReversed(), key = { _, message -> message.id }) { reversedIndex, message ->
                    val originalIndex = messages.lastIndex - reversedIndex
                    val previous = messages.getOrNull(originalIndex - 1)
                    val showTime = previous == null ||
                        message.timestamp - previous.timestamp > 600L ||
                        !sameChatDay(previous.timestamp, message.timestamp)
                    if (showTime) {
                        Text(
                            text = formatChatTime(message.timestamp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 8.dp),
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                    ChatBubble(
                        message = message,
                        otherAvatar = session.avatar,
                        myAvatar = myAvatar,
                    )
                }
                item(key = "history") {
                    Text(if (historyLoading) "加载中…" else historyError?.let { "$it · 点击重试" }
                        ?: if (historyCursor != null) "加载更早的消息" else "没有更早的消息了",
                        color = MiuixTheme.colorScheme.onBackgroundVariant, fontSize = 12.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !historyLoading && historyCursor != null) { loadHistory() }
                            .padding(vertical = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun PrivateChatComposer(
    api: BiliApi,
    draft: String,
    onDraftChange: (String) -> Unit,
    showEmoji: Boolean,
    onToggleEmoji: () -> Unit,
    onPickEmoji: (String) -> Unit,
    pendingImage: android.net.Uri?,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onSend: () -> Unit,
    sending: Boolean,
    uploading: Boolean,
) {
    val colors = MiuixTheme.colorScheme
    val recipe = TeakGlassRecipes.floatingNavigation(LocalTeakMaterialSpec.current.fineTuning)
    val backdrop = LocalTeakBackdrop.current
    val density = LocalDensity.current
    val imeDp = with(density) {
        WindowInsets.ime.getBottom(density).toDp().value
    }
    val navDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value
    val raiseDp = if (AppGraph.settings.bottomBarRaise) {
        TeakChromeDefaults.RaisedBottomBarExtra.value
    } else {
        0f
    }
    val animatedBottom = if (imeDp > navDp) imeDp else navDp + raiseDp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = animatedBottom.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (showEmoji) {
            EmojiPanel(
                api = api,
                onPick = onPickEmoji,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeakGlassSurface(
                modifier = Modifier.weight(1f).height(46.dp),
                shape = Capsule(),
                recipe = recipe,
                backdrop = backdrop,
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = onDraftChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(color = colors.onBackground, fontSize = 13.sp),
                        cursorBrush = SolidColor(colors.primary),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (draft.isEmpty()) {
                                    Text(
                                        text = "发个消息聊聊呗",
                                        color = colors.outline,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                    )
                                }
                                inner()
                            }
                        },
                    )
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
            Spacer(Modifier.width(8.dp))
            TeakGlassIconButton(
                onClick = onToggleEmoji,
                size = 46.dp,
                recipe = recipe,
                backdrop = backdrop,
            ) {
                Text(text = "\uD83D\uDE00", fontSize = 19.sp)
            }
        }
        if (uploading) {
            Text(
                text = "图片上传中…",
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 16.dp, top = 5.dp),
            )
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    otherAvatar: String,
    myAvatar: String,
) {
    val colors = MiuixTheme.colorScheme
    val mine = message.isMine
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!mine) {
            ChatAvatar(url = otherAvatar)
            Spacer(Modifier.width(7.dp))
        }
        Column(
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 272.dp),
        ) {
            if (message.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = rememberImageRequest(message.imageUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceContainerHigh),
                )
            }
            val sharedVideo = remember(message.text) { parseSharedVideo(message.text) }
            if (sharedVideo != null) {
                ChatSharedVideoCard(info = sharedVideo)
            } else if (message.text.isNotBlank()) {
                EmoteText(
                    text = message.text,
                    color = if (mine) Color.White else Color.Black,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (mine) colors.primary else Color.White)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                )
            }
        }
        if (mine) {
            Spacer(Modifier.width(7.dp))
            ChatAvatar(url = myAvatar, placeholder = "我")
        }
    }
}

/** 私信里的视频分享是一段 JSON 文本，解析出来按动态视频卡片展示。 */
private data class SharedVideoInfo(
    val bvid: String,
    val title: String,
    val cover: String,
    val play: Long,
    val danmaku: Long,
    val desc: String,
)

private fun parseSharedVideo(text: String): SharedVideoInfo? {
    val trimmed = text.trim()
    if (!trimmed.startsWith("{")) return null
    return runCatching {
        val json = JSONObject(trimmed)
        val bvid = json.optString("bvid")
        if (bvid.isBlank()) return@runCatching null
        SharedVideoInfo(
            bvid = bvid,
            title = json.optString("title").ifBlank { "视频" },
            cover = json.optString("cover"),
            play = json.optLong("view"),
            danmaku = json.optLong("danmaku"),
            desc = json.optString("desc"),
        )
    }.getOrNull()
}

@Composable
private fun ChatSharedVideoCard(info: SharedVideoInfo) {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .width(240.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .clickable(interactionSource = null, indication = null) {
                TeakNavigator.openVideo(
                    context,
                    VideoItem(
                        bvid = info.bvid,
                        aid = 0,
                        title = info.title,
                        cover = info.cover,
                        ownerName = "",
                        ownerFace = "",
                        ownerMid = 0,
                        play = info.play,
                        danmaku = info.danmaku,
                        duration = 0,
                        pubdate = 0,
                    ),
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(colors.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = rememberImageRequest(info.cover),
                contentDescription = info.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0x66000000))),
                    ),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${formatCount(info.play)} 播放",
                    color = Color.White,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${formatCount(info.danmaku)} 弹幕",
                    color = Color.White,
                    fontSize = 10.sp,
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = info.title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
            )
            if (info.desc.isNotBlank() && info.desc != "-") {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = info.desc,
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ChatAvatar(url: String, placeholder: String = "") {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colors.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = rememberImageRequest(url),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (placeholder.isNotBlank()) {
            Text(placeholder, color = colors.onBackgroundVariant, fontSize = 11.sp)
        }
    }
}

private fun sameChatDay(a: Long, b: Long): Boolean {
    if (a <= 0L || b <= 0L) return a == b
    val ca = Calendar.getInstance().apply { timeInMillis = a * 1000L }
    val cb = Calendar.getInstance().apply { timeInMillis = b * 1000L }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
        ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

// 按线程缓存，避免每条聊天记录都新建 formatter。
private val chatTimeThisYear = ThreadLocal.withInitial {
    SimpleDateFormat("M月d日 HH:mm", Locale.CHINA)
}
private val chatTimeOtherYear = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
}

private fun formatChatTime(seconds: Long): String {
    if (seconds <= 0L) return ""
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = seconds * 1000L }
    val formatter = if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)) {
        chatTimeThisYear.get()!!
    } else {
        chatTimeOtherYear.get()!!
    }
    return formatter.format(Date(seconds * 1000L))
}
