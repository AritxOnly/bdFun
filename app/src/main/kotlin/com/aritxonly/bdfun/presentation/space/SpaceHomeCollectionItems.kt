package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal fun androidx.compose.foundation.lazy.LazyListScope.spaceHomeCollectionItems(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val seasons = content.snapshot.seasons
    val ui = content.ui
    val tabEnterModifier = Modifier
    if (seasons.isNotEmpty()) {
        item(key = "home-collections-title") {
            SpaceSectionHeader(
                title = "合集和系列",
                onMore = {
                    ui.submissionMode = 3
                    ui.selectedCollection = null
                    actions.onSelectTab(2)
                },
                modifier = Modifier
                    .then(tabEnterModifier)
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp),
            )
        }
        item(key = "home-collections") {
            Column(
                modifier = Modifier
                    .then(tabEnterModifier)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                val shown = seasons.take(4)
                for (row in 0..1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        for (col in 0..1) {
                            val index = row * 2 + col
                            val season = shown.getOrNull(index)
                            if (season == null) {
                                Spacer(Modifier.weight(1f))
                            } else {
                                SpaceCollectionGridCard(
                                    season = season,
                                    onClick = { actions.onLoadCollection(season) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    if (row == 0) Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}
