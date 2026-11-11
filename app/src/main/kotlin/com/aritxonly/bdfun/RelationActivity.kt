package com.aritxonly.bdfun

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.data.RelationUser
import com.aritxonly.bdfun.ui.*
import com.aritxonly.teak.common.chrome.*
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

class RelationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val mid = intent.getLongExtra("mid", 0)
        if (mid <= 0) { finish(); return }
        val fans = intent.getBooleanExtra("fans", false)
        setContent { RelationPage(mid, fans, onBack = ::finish) }
    }

    companion object {
        fun open(context: Context, mid: Long, fans: Boolean) {
            if (mid > 0) context.startActivity(Intent(context, RelationActivity::class.java)
                .putExtra("mid", mid).putExtra("fans", fans))
        }
    }
}

@Composable
private fun RelationPage(mid: Long, fans: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var frequent by rememberSaveable(mid, fans) { mutableStateOf(false) }
    val state = remember(mid, fans, frequent) {
        RelationFeedState { page -> AppGraph.api.relationUsers(mid, fans, page, frequent) }
    }
    val listState = rememberLazyListState()
    val busyUsers = remember { mutableStateMapOf<Long, Boolean>() }
    val title = if (fans) "粉丝" else "关注"
    val currentMid = AppGraph.session.value?.mid
    LaunchedEffect(state) { listState.scrollToItem(0); state.fetch(reset = true) }
    MessageResumeEffect { state.fetch(reset = true) }
    TeakActivityScaffold(settings = AppGraph.settings, title = title, onBack = onBack,
        topBarActions = {
            TeakTopGlassButton(onClick = { scope.launch { state.fetch(reset = true) } }) {
                Icon(MiuixIcons.Refresh, contentDescription = "刷新$title", tint = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp))
            }
        },
    ) {
        val colors = MiuixTheme.colorScheme
        val dark = colors.background.luminance() < 0.5f
        SideEffect { context.findHostActivity()?.syncSystemBarIcons(dark) }
        TrackTeakPageScroll(listState)
        RegisterScrollTop(listState, "relations")
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
        ) {
            item("title") { TeakLargeTitle(title = title); Spacer(Modifier.height(16.dp)) }
            if (!fans) item("order") {
                TeakPagerTabs(options = listOf("最近关注", "最常访问"), selected = if (frequent) 1 else 0,
                    onSelect = { frequent = it == 1 }, itemWidth = 94.dp,
                    plain = true, backdrop = null, modifier = Modifier.padding(bottom = 12.dp))
            }
            if (state.total > 0) item("total") {
                Text("共 ${state.total} 人", color = colors.onBackgroundVariant, fontSize = 13.sp,
                    modifier = Modifier.padding(start = 12.dp, bottom = 12.dp))
            }
            itemsIndexed(state.items, key = { _, user -> user.mid }) { index, user ->
                MessageRowSurface(first = index == 0, last = index == state.items.lastIndex) {
                    RelationUserRow(user, busyUsers[user.mid] == true, showAction = user.mid != currentMid,
                        onOpen = {
                            AppGraph.pendingSpaceMid.value = user.mid
                            context.startActivity(Intent(context, SpaceActivity::class.java))
                        },
                        onFollow = {
                            if (!AppGraph.api.isLoggedIn) context.showToast("请先登录")
                            else if (busyUsers[user.mid] != true) {
                                busyUsers[user.mid] = true
                                scope.launch {
                                    try {
                                        // If the batch failed, resolve explicitly before offering any mutation.
                                        if (user.attribute == null) {
                                            val attr = AppGraph.api.relation(user.mid) ?: error("关注状态获取失败，请重试")
                                            state.updateRelation(user.mid, attr)
                                        } else {
                                            val follow = !user.followed
                                            AppGraph.api.followUser(user.mid, follow)
                                            state.updateRelation(user.mid, if (follow) 2 else 0)
                                            context.showToast(if (follow) "关注成功" else "已取消关注")
                                        }
                                    } catch (e: CancellationException) { throw e }
                                    catch (e: Exception) { context.showToast(e.message ?: "操作失败，请重试") }
                                    finally { busyUsers.remove(user.mid) }
                                }
                            }
                        })
                }
            }
            item("footer") {
                LaunchedEffect(state.loading, state.error, state.nextPage) {
                    if (!state.loading && state.error == null && state.nextPage != null) scope.launch { state.fetch() }
                }
                Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (state.loading) TeakLoading(size = 28.dp)
                    Text(when {
                        state.loading -> "加载中…"
                        state.error != null -> "${state.error}\n点击重试"
                        state.items.isEmpty() -> if (fans) "还没有粉丝" else "还没有关注的人"
                        state.nextPage != null -> "加载更多"
                        else -> "已显示全部可见用户"
                    }, color = colors.onBackgroundVariant, fontSize = 13.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !state.loading &&
                            (state.error != null || state.nextPage != null)) {
                            scope.launch { if (state.error != null) state.retry() else state.fetch() }
                        }.padding(12.dp))
                }
            }
        }
    }
}

@Composable
private fun RelationUserRow(user: RelationUser, busy: Boolean, showAction: Boolean,
    onOpen: () -> Unit, onFollow: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.weight(1f).clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(model = rememberImageRequest(user.face), contentDescription = "${user.name}的头像",
                contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.surfaceContainer))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(user.name, color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(user.sign.ifBlank { "这个人很神秘，什么都没有写" }, color = colors.onBackgroundVariant,
                    fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (showAction) Button(onClick = onFollow, enabled = !busy, minWidth = 76.dp, minHeight = 38.dp,
            cornerRadius = 24.dp, insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            colors = if (user.followed || user.attribute == null) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColorsPrimary()) {
            Text(when { busy -> "处理中"; user.attribute == null -> "重试状态"; user.attribute == 6 -> "已互关";
                user.followed -> "已关注"; else -> "+ 关注" }, fontSize = 12.sp,
                color = if (user.followed || user.attribute == null) colors.onSecondaryVariant else colors.onPrimary)
        }
    }
}
