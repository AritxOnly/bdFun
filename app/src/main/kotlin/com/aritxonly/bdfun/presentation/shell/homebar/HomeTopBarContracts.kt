package com.aritxonly.bdfun.ui


import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.unit.Dp
import com.aritxonly.bdfun.core.model.user.UserProfile


@androidx.compose.runtime.Immutable
internal data class HomeTopBarContent(val profile: UserProfile?, val unreadCount: Int = 0)
@androidx.compose.runtime.Immutable
internal data class HomeTopBarSearch(val active: Boolean = false, val query: String = "",
    val reveal: Float = 0f, val resultsVisible: Boolean = false)
@androidx.compose.runtime.Immutable
internal data class HomeTopBarActions(val onSearch: () -> Unit, val onOpenMessages: () -> Unit,
    val onOpenProfile: () -> Unit, val onQueryChange: (String) -> Unit = {}, val onSubmitSearch: () -> Unit = {},
    val onCancelSearch: () -> Unit = {}, val onClearQuery: () -> Unit = {}, val onOpenSearchPage: () -> Unit = {})
internal class HomeTopBarMaterial(val wide: Boolean, val searchWidth: Dp,
    val recipe: com.aritxonly.teak.common.material.GlassMaterialRecipe,
    val backdrop: top.yukonga.miuix.kmp.blur.Backdrop?, val searchInteraction: MutableInteractionSource, val classic: Boolean)
