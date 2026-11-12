package com.aritxonly.bdfun

import android.content.Context
import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.ui.LibraryScreen
import com.aritxonly.bdfun.ui.LibraryScreenRequest
import com.aritxonly.bdfun.ui.LibraryNavigation

@Composable
internal fun LibraryScreenRoute(context: Context, fixedMode: Int = 0, title: String = "收藏") {
    val owner = AppGraph.session.value?.mid
    LibraryScreen(
        LibraryScreenRequest(owner != null, owner, fixedMode, title, AppGraph.session.identity.revision),
        LibraryNavigation({ TeakNavigator.openLogin(context) }, { (context as? android.app.Activity)?.finish() },
            { TeakNavigator.openVideo(context, it) }),
        AppGraph.libraryDependencies,
    )
}
