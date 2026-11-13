package com.aritxonly.bdfun

/** 入口只提供会话输入；搜索栏和正文始终由相同组件绘制。 */
internal data class MainSearchInput(
    val active: Boolean, val query: String, val submitted: String?, val reveal: Float,
    val changeQuery: (String) -> Unit, val changeSubmitted: (String?) -> Unit, val close: () -> Unit,
)
internal fun MainShellState.searchInput(bottomSearch: Boolean): MainSearchInput = if (bottomSearch) {
    MainSearchInput(bottom.showBottomSearch, bottom.bottomQuery, bottom.bottomSubmitted, 1f,
        { bottom.bottomQuery = it }, { bottom.bottomSubmitted = it }, { bottom.close() })
} else {
    MainSearchInput(ui.showSearch, ui.searchQuery, ui.searchSubmitted, searchReveal,
        { ui.searchQuery = it }, { ui.searchSubmitted = it }, { ui.closeSearch() })
}
