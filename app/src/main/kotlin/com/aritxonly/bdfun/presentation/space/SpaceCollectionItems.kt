package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal fun androidx.compose.foundation.lazy.LazyListScope.spaceCollectionItems(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val seasons = content.snapshot.seasons
    if (seasons.isEmpty()) {
        item(key = "all-collections-empty") {
            SpaceEmpty("还没有合集")
        }
    } else {
        items(
            items = seasons.chunked(2),
            key = { row ->
                row.joinToString("-") {
                    "${if (it.isSeries) "series" else "season"}-${it.id}"
                }
            },
        ) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { season ->
                    SpaceCollectionGridCard(
                        season = season,
                        onClick = { actions.onLoadCollection(season) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
