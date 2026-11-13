package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.aritxonly.bdfun.core.model.video.SponsorSegment
import com.aritxonly.bdfun.feature.video.application.PlaybackTrackingStateHolder

data class PlayerSponsorPresentation(val active: SponsorSegment?, val dismiss: (SponsorSegment) -> Unit)

private class PlayerSponsorUiStateHolder {
    val dismissed = mutableStateOf<Set<String>>(emptySet())
    fun dismiss(segment: SponsorSegment) { dismissed.value = dismissed.value + segment.uuid }
}

@Composable
fun rememberPlayerSponsor(
    bvid: String, tracking: PlaybackTrackingStateHolder, observed: PlayerObservedStateHolder, sponsorSkipEnabled: () -> Boolean,
): PlayerSponsorPresentation {
    val state by tracking.state.collectAsState()
    val ui = remember(bvid) { PlayerSponsorUiStateHolder() }
    val dismissed by ui.dismissed
    val enabled = sponsorSkipEnabled()
    LaunchedEffect(bvid, enabled) { tracking.loadSegments(bvid, enabled) }
    val positionMs by observed.positionMs
    val active = tracking.activeSegment(state, bvid, positionMs, dismissed)
    return PlayerSponsorPresentation(active.takeIf { enabled }, ui::dismiss)
}
