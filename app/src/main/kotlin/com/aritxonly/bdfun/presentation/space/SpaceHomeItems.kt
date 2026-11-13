package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToVideo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.ui.VideoGridCard

internal fun androidx.compose.foundation.lazy.LazyListScope.spaceHomeItems(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val space = content.snapshot.profile.space
    val context = actions.context
    val seasons = content.snapshot.seasons
    val allVideos = content.snapshot.videos.items
    val ui = content.ui
    val tabEnterModifier = Modifier
    item(key = "home-videos-title") {
        SpaceSectionHeader(
            title = "视频（${(space?.videoCount ?: 0).takeIf { it > 0 } ?: allVideos.size}）",
            onMore = {
                ui.submissionMode = 0
                ui.selectedCollection = null
                actions.onSelectTab(2)
            },
            modifier = Modifier
                .then(tabEnterModifier)
                .padding(start = 16.dp, end = 16.dp, top = 10.dp),
        )
    }
    item(key = "home-videos") {
        Column(
            modifier = Modifier
                .then(tabEnterModifier)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            val shown = allVideos.take(4)
            for (row in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (col in 0..1) {
                        val index = row * 2 + col
                        val video = shown.getOrNull(index)
                        if (video == null) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            VideoGridCard(
                                video = video,
                                onClick = {
                                    navigateToVideo(context, video)
                                },
                                modifier = Modifier.weight(1f),
                                showOwner = false,
                            )
                        }
                    }
                }
                if (row == 0) Spacer(Modifier.height(10.dp))
            }
        }
    }
    spaceHomeCollectionItems(content, actions)

}
