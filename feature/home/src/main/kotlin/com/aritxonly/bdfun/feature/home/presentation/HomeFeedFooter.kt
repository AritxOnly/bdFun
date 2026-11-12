package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun HomeFeedFooter(state: HomeFeedState, scope: kotlinx.coroutines.CoroutineScope) {
    val colors = MiuixTheme.colorScheme
                    val footerError = state.loadMoreError
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .then(
                                if (footerError != null) {
                                    Modifier.clickable { scope.launch { state.loadMore() } }
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = when {
                                state.loadingMore -> "加载中…"
                                footerError != null -> footerError
                                state.endReached -> "没有更多了"
                                else -> "上滑加载更多"
                            },
                            color = colors.onBackgroundVariant,
                            fontSize = 12.sp,
                        )
                    }
}
