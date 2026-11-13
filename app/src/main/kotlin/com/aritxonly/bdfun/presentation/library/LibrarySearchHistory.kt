package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.feature.library.application.*
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LibrarySearchHistory(history: List<String>, events: LibrarySearchEvents) {
    val colors = MiuixTheme.colorScheme
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "history-title", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            LibrarySearchHistoryHeader(history, events)
        }
        if (history.isEmpty()) {
            item(key = "empty-history", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "还没有搜索历史",
                    color = colors.onBackgroundVariant,
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }
        }
        items(items = history, key = { "h" + it }) { word ->
            val rank = history.indexOf(word) + 1
            LibrarySearchHistoryWord(word, rank, events)
        }
    }
}

@Composable
internal fun LibrarySearchHistoryHeader(history: List<String>, events: LibrarySearchEvents) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "搜索历史",
            color = colors.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        if (history.isNotEmpty()) {
            Text(
                text = "清空",
                color = colors.onBackgroundVariant,
                fontSize = 12.sp,
                modifier = Modifier.clickable {
                    events.clearHistory()
                },
            )
        }
    }
}

@Composable
internal fun LibrarySearchHistoryWord(word: String, rank: Int, events: LibrarySearchEvents) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Capsule())
            .clickable {
                events.chooseHistory(word)
            }
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString(),
            color = if (rank <= 3) colors.primary else colors.onBackgroundVariant,
            fontSize = 12.sp,
            fontWeight = if (rank <= 3) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.width(20.dp),
        )
        Text(
            text = word,
            color = colors.onBackground,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
