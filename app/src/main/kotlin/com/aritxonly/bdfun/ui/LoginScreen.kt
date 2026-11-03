package com.aritxonly.bdfun.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.QrLogin
import com.aritxonly.bdfun.data.QrState
import com.aritxonly.bdfun.data.UserProfile
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 用 zxing 把登录 url 画成二维码位图。 */
private fun qrBitmap(text: String, size: Int): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (matrix[x, y]) {
                0xFF000000.toInt()
            } else {
                0xFFFFFFFF.toInt()
            }
        }
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}

/** 把二维码写进系统相册（Pictures/bdFun），Android 10+ 不需要存储权限。 */
private fun saveQrToGallery(context: android.content.Context, bitmap: Bitmap): Boolean {
    val name = "bdfun_qr_${System.currentTimeMillis()}.png"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/bdFun",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return false
    return runCatching {
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: error("no output stream")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    }.getOrElse {
        runCatching { resolver.delete(uri, null, null) }
        false
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoginScreen(
    api: BiliApi,
    onBack: () -> Unit,
    onSuccess: (UserProfile?) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    var qr by remember { mutableStateOf<QrLogin?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("正在获取二维码…") }
    var expired by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun saveQr() {
        val image = bitmap
        if (image == null) {
            context.showToast("二维码还没准备好")
            return
        }
        if (saveQrToGallery(context, image)) {
            context.showToast("成功保存二维码")
        } else {
            context.showToast("保存失败")
        }
    }

    BackHandler(onBack = onBack)

    suspend fun loadQr() {
        loading = true
        expired = false
        bitmap = null
        status = "正在获取二维码…"
        runCatching { api.qrGenerate() }
            .onSuccess { code ->
                qr = code
                bitmap = runCatching { qrBitmap(code.url, 480) }.getOrNull()
                status = "请用哔哩哔哩手机客户端扫码"
            }
            .onFailure {
                status = "二维码获取失败：${it.message ?: "网络异常"}"
                expired = true
            }
        loading = false
    }

    LaunchedEffect(Unit) { loadQr() }

    LaunchedEffect(qr?.key) {
        val key = qr?.key ?: return@LaunchedEffect
        while (true) {
            delay(1500)
            val poll = runCatching { api.qrPoll(key) }.getOrNull() ?: continue
            when (poll.state) {
                QrState.Success -> {
                    onSuccess(poll.profile)
                    return@LaunchedEffect
                }

                QrState.Scanned -> status = "已扫码，请在手机上确认登录"
                QrState.Expired -> {
                    expired = true
                    status = "二维码已过期，请刷新"
                    return@LaunchedEffect
                }

                QrState.Error -> {
                    expired = true
                    status = poll.message.ifBlank { "登录失败" }
                    return@LaunchedEffect
                }

                QrState.Waiting -> Unit
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(44.dp))
        Text(
            text = "扫码登录",
            color = colors.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "登录后可查看关注动态、个人主页与更高清晰度",
            color = colors.onBackgroundVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(ContinuousCornerShape(24.dp))
                .background(colors.surfaceContainerHigh)
                // 长按二维码也能保存
                .combinedClickable(
                    interactionSource = null,
                    indication = null,
                    onClick = {},
                    onLongClick = { saveQr() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            val image = bitmap
            if (image != null) {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = "登录二维码",
                    modifier = Modifier
                        .size(208.dp)
                        .clip(ContinuousCornerShape(12.dp)),
                )
            } else if (loading) {
                TeakLoading(
                    modifier = Modifier.size(28.dp),
                    color = colors.primary,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.primary)
                // 高光描边，和 App 里其它胶囊按钮一致
                .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = { saveQr() },
                )
                .padding(horizontal = 22.dp, vertical = 8.dp),
        ) {
            Text(
                text = "保存二维码",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = status,
            color = colors.onBackgroundVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )

        if (expired) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.primary)
                    .clickable { scope.launch { loadQr() } }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text(text = "刷新二维码", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
