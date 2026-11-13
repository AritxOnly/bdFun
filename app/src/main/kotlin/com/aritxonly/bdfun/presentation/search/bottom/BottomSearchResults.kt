package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.user.SearchUser
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.chrome.TeakScrollContainer
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BottomSearchResults(
    tab: Int,
    onTabChange: (Int) -> Unit,
    results: List<VideoItem>,
    users: List<SearchUser>,
    loading: Boolean,
    loadedOnce: Boolean,
    error: String?,
    onOpenVideo: (VideoItem) -> Unit,
    onOpenUser: (SearchUser) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val listState = rememberLazyListState()
    TrackTeakPageScroll(listState)
    Column(modifier = Modifier.fillMaxSize()) {
        // 分段栏参照原实现的分类标题：搜索框下面一行的类别切换。
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            BottomSearchSegmented(
                options = listOf("视频", "UP主"),
                selected = tab,
                onSelect = onTabChange,
            )
        }
        TeakScrollContainer(listState, Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (loading && results.isEmpty() && users.isEmpty()) {
                    item(key = "loading") {
                        BottomSearchLoading()
                    }
                }
                if (error != null && results.isEmpty() && users.isEmpty()) {
                    item(key = "error") {
                        BottomSearchError(error)
                    }
                }

                if (tab == 0) {
                    items(items = results, key = { "r" + it.bvid }) { video ->
                        BottomSearchVideoRow(video = video, onClick = { onOpenVideo(video) })
                    }
                    if (loadedOnce && !loading && results.isEmpty() && error == null) {
                        item(key = "empty-videos") {
                            Text(
                                text = "没有找到相关视频",
                                color = colors.onBackgroundVariant,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 24.dp),
                            )
                        }
                    }
                } else {
                    items(items = users, key = { "u" + it.mid }) { user ->
                        BottomSearchUserRow(user = user, onClick = { onOpenUser(user) })
                    }
                    if (loadedOnce && !loading && users.isEmpty() && error == null) {
                        item(key = "empty-users") {
                            Text(
                                text = "没有找到相关用户",
                                color = colors.onBackgroundVariant,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 结果页的分段栏：实色胶囊 + 选中项白底，不走玻璃。 */
