package com.aritxonly.bdfun.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.user.UserProfile
import kotlinx.coroutines.launch

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import com.aritxonly.bdfun.feature.login.application.QrLoginStateHolder

@Composable
internal fun LoginScreen(controller: QrLoginStateHolder, onBack: () -> Unit, onSuccess: (UserProfile?) -> Unit) {
    val context = LocalContext.current
    val state by controller.state.collectAsState()
    val bitmap = remember(state.displayQr) {
        state.displayQr?.let { runCatching { qrBitmap(it.url, 480) }.getOrNull() }
    }
    val scope = rememberCoroutineScope()
    DisposableEffect(controller) { onDispose { controller.close() } }
    BackHandler(onBack = onBack)
    LaunchedEffect(controller) { controller.generate() }
    LaunchedEffect(controller, state.qr?.key) {
        state.qr?.key?.let { key -> controller.poll(key)?.let { onSuccess(it.profile) } }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LoginHeader()
        LoginQrCard(bitmap, state.loading) { saveLoginQr(context, bitmap) }
        OfficialAppLoginButton(enabled = controller.officialAppLoginLink() != null) {
            controller.officialAppLoginLink()?.let { openOfficialAppLogin(context, it) }
                ?: context.showToast("请先刷新并获取有效二维码")
        }
        LoginStatus(state) { scope.launch { controller.generate() } }
    }
}
private fun saveLoginQr(context: android.content.Context, bitmap: Bitmap?) {
    if (bitmap == null) {
        context.showToast("二维码还没准备好")
        return
    }
    if (saveQrToGallery(context, bitmap)) context.showToast("成功保存二维码")
    else context.showToast("保存失败")
}
