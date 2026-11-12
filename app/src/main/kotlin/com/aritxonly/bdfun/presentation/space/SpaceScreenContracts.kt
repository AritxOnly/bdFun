package com.aritxonly.bdfun

import androidx.compose.ui.unit.Dp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.core.model.user.SpaceSeason

internal class SpaceBannerStateHolder {
    var ready by mutableStateOf(false)
    var bitmap by mutableStateOf<android.graphics.Bitmap?>(null)
}
internal class SpaceSelectionStateHolder {
    var selectedTab by mutableIntStateOf(0)
    var submissionMode by mutableIntStateOf(0)
    var selectedCollection by mutableStateOf<SpaceSeason?>(null)
}
internal class SpaceHeaderStateHolder {
    var heightPx by mutableIntStateOf(0)
    var collapsePx by mutableFloatStateOf(0f)
}
internal data class SpaceScreenContent(
    val mid: Long, val isSelf: Boolean, val loggedIn: Boolean,
    val snapshot: com.aritxonly.bdfun.feature.space.application.SpaceUiState,
    val ui: SpaceSelectionStateHolder, val banner: SpaceBannerStateHolder,
    val pagerState: androidx.compose.foundation.pager.PagerState,
    val pagePosition: androidx.compose.runtime.State<Float>,
)
internal data class SpaceScreenLayout(
    val wideSpace: Boolean, val profileWidth: Dp, val statusBarTop: Dp,
    val profileListState: androidx.compose.foundation.lazy.LazyListState,
    val tabListStates: List<androidx.compose.foundation.lazy.LazyListState>,
    val headerScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    val header: SpaceHeaderStateHolder, val headerMaxCollapsePx: Float,
)
internal class SpaceScreenActions(
    val context: android.content.Context, val onSelectTab: (Int) -> Unit,
    val onLoadCollection: (SpaceSeason) -> Unit, val onLoadArticles: (Int) -> Unit,
    val onFollow: () -> Unit, val onPageDrag: (Float) -> Unit,
)
