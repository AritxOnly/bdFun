package com.aritxonly.bdfun


import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import com.aritxonly.bdfun.ui.rememberStricterSwipeConfiguration
import com.aritxonly.bdfun.core.model.user.UserProfile
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.DynamicScreen
import com.aritxonly.bdfun.ui.MineScreen

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun MainTabPager(
    pager: androidx.compose.foundation.pager.PagerState,
    homeFeed: com.aritxonly.bdfun.feature.home.presentation.HomeFeedState,
    dynamicFeed: com.aritxonly.bdfun.feature.dynamic.presentation.DynamicFeedState,
    session: UserProfile?, settings: SettingsBindings, dynamicRefreshTick: Int,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    CompositionLocalProvider(
        LocalViewConfiguration provides rememberStricterSwipeConfiguration(),
    ) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0,
            // 到头/到尾再滑不出现任何回弹动画
            overscrollEffect = null,
        ) { page ->
            when (page) {
                0 -> HomeScreenRoute(
                    state = homeFeed,
                    onOpenVideo = { TeakNavigator.openVideo(context, it) },
                    cardStyle = settings.homeCardStyle,
                )

                1 -> DynamicScreen(
                    state = dynamicFeed,
                    loggedIn = session != null,
                    onNeedLogin = { TeakNavigator.openLogin(context) },
                    onOpenVideo = { TeakNavigator.openVideo(context, it) },
                    onOpenDynamic = { TeakNavigator.openDynamic(context, it) },
                    refreshTick = dynamicRefreshTick,
                )

                else -> MainMineTab(session, settings)
            }
        }
    }
}

@Composable
private fun MainMineTab(session: UserProfile?, settings: SettingsBindings) {
    val context = androidx.compose.ui.platform.LocalContext.current
    MineScreen(
        com.aritxonly.bdfun.ui.MineScreenRequest(session, settings.sentHistoryEntry, AppGraph.session.identity.revision),
        com.aritxonly.bdfun.ui.MineNavigation(
            { TeakNavigator.openLogin(context) }, { TeakNavigator.openSettings(context) },
            { TeakNavigator.openLibrary(context) }, { TeakNavigator.openHistory(context) },
            { TeakNavigator.openWatchLater(context) }, { TeakNavigator.openCache(context) },
            { TeakNavigator.openMySent(context) }, { TeakNavigator.openSpace(context, it) },
            { mid, fans -> RelationActivity.open(context, mid, fans) }, { TeakNavigator.openVideo(context, it) },
        ),
        AppGraph.mineDependencies,
    )
}
