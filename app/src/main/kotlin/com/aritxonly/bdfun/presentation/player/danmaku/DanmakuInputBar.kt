package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.aritxonly.bdfun.feature.video.application.*
import androidx.compose.runtime.collectAsState

@Composable
fun DanmakuInputBar(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val page = LocalVideoPageState.current
    val advancedMaterial = LocalTeakMaterialSpec.current
    val recipe = TeakGlassRecipes.floatingNavigation(advancedMaterial.fineTuning)
    val backdrop = LocalTeakBackdrop.current
    val scope = rememberCoroutineScope()
    val controller = remember(page) { DanmakuInputStateHolder() }
    val form by controller.state.collectAsState()
    androidx.compose.runtime.DisposableEffect(controller) { onDispose { controller.close() } }
    val ui = remember { DanmakuInputPresentationStateHolder() }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val bottomDp = danmakuInputBottom(ui)
    LaunchedEffect(Unit) { androidx.compose.runtime.withFrameNanos { }; ui.shown = true }
    val reveal = rememberSmoothFloat(if (ui.shown) 1f else 0f, 280f, EaseOutQuint)
    LaunchedEffect(Unit) { delay(80); runCatching { focusRequester.requestFocus() } }
    fun dismiss() {
        if (!ui.shown) return
        ui.shown = false
        ui.frozenBottom = bottomDp
        focusManager.clearFocus()
        keyboard?.hide()
        scope.launch { delay(200); onDismiss() }
    }
    val closeTokenAtOpen = remember { page.danmakuInputClose }
    LaunchedEffect(page.danmakuInputClose) {
        if (page.danmakuInputClose == closeTokenAtOpen) return@LaunchedEffect
        dismiss()
    }
    fun send() {
        val request = controller.beginSend(page.danmakuSender) ?: return
        scope.launch { if (controller.completeSend(request) == DanmakuInputResult.Success) dismiss() }
    }
    val runtime = DanmakuInputRuntime(controller, form, ui, focusRequester, recipe, backdrop, ::send)
    DanmakuInputLayout(runtime, reveal, bottomDp, modifier)
}
internal class DanmakuInputPresentationStateHolder {
    var shown by mutableStateOf(false)
    var showEmoji by mutableStateOf(false)
    var frozenBottom by mutableStateOf<Float?>(null)
}
internal class DanmakuInputRuntime(val controller: DanmakuInputStateHolder, val form: DanmakuInputState,
    val ui: DanmakuInputPresentationStateHolder, val focusRequester: FocusRequester,
    val recipe: com.aritxonly.teak.common.material.GlassMaterialRecipe,
    val backdrop: top.yukonga.miuix.kmp.blur.Backdrop?, val send: () -> Unit)
@Composable
private fun danmakuInputBottom(ui: DanmakuInputPresentationStateHolder): Float {
    val density = LocalDensity.current
    val imeDp = with(density) { WindowInsets.ime.getBottom(density).toDp().value }
    val navDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value
    val animatedBottom = if (imeDp > navDp) imeDp else navDp
    return ui.frozenBottom ?: animatedBottom
}
