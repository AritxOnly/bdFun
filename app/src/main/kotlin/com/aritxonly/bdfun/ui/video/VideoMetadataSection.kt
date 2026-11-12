package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.data.VideoDetail
import com.aritxonly.bdfun.data.VideoItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal data class VideoMetadataContent(
    val detail: VideoDetail?, val video: VideoItem, val metaReady: Boolean,
    val upFollowers: Long, val upVideos: Long, val onlineCount: Long, val onlineText: String,
)
internal class VideoMetadataActions(
    val toggleFollow: () -> Unit, val toggleLike: () -> Unit, val toggleDislike: () -> Unit,
    val giveCoin: () -> Unit, val toggleFavorite: () -> Unit, val shareVideo: () -> Unit,
    val tripleAction: () -> Unit, val onOpenSpace: () -> Unit,
)
internal class VideoDescriptionStateHolder {
    var expanded by mutableStateOf(false)
    var expandable by mutableStateOf(false)
    fun reset() { expanded = false; expandable = false }
}

@Composable
internal fun VideoMetadataSection(
    content: VideoMetadataContent,
    interaction: com.aritxonly.bdfun.feature.video.application.VideoInteractionState,
    description: VideoDescriptionStateHolder, actions: VideoMetadataActions,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        StaggerLine(content.metaReady, 0) { VideoMetadataAuthor(content, interaction, actions) }
        Spacer(Modifier.height(10.dp))
        StaggerLine(content.metaReady, 1) { VideoMetadataTitle(content) }
        Spacer(Modifier.height(4.dp))
        StaggerLine(content.metaReady, 2) { VideoMetadataStatistics(content) }
        val descText = content.detail?.desc?.takeIf { it.isNotBlank() }
        val descProgress = rememberManualProgress(
            if (description.expanded) 1f else 0f,
            durationMs = 420f,
        )
        Spacer(Modifier.height(4.dp))
        StaggerLine(content.metaReady, 3) {
            VideoMetadataIdentifier(content, description, descText, descProgress)
        }
        StaggerLine(content.metaReady, 4) {
            VideoMetadataDescription(descText, descProgress, description)
        }
        StaggerLine(content.metaReady, 5) { VideoMetadataCategory(content) }
        Spacer(Modifier.height(14.dp))
        StaggerLine(content.metaReady, 6) { VideoMetadataInteractions(interaction, actions) }
    }
}
