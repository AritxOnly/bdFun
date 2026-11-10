package com.aritxonly.bdfun.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.bdfun.data.SettingsStore
import com.kyant.shapes.Capsule
import com.aritxonly.teak.common.material.TeakGlassPanel
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val tag: String,
    val name: String,
    val body: String,
    val url: String,
)

/** 检查 GitHub 最新 release；preview 版本也能识别。 */
object UpdateChecker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun check(): UpdateInfo? = kotlinx.coroutines.withContext(
        kotlinx.coroutines.Dispatchers.IO,
    ) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/QxTeak/bdFun/releases?per_page=5")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "bdFun-Android")
                .get()
                .build()
            val json = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                JSONArray(response.body?.string() ?: return@use null)
            } ?: return@runCatching null
            val latest = (0 until json.length())
                .mapNotNull { json.optJSONObject(it) }
                .firstOrNull { !it.optBoolean("draft", false) }
                ?: return@runCatching null
            val tag = latest.optString("tag_name")
            if (tag.isBlank() || !isNewer(tag, BuildConfig.VERSION_NAME)) {
                return@runCatching null
            }
            UpdateInfo(
                tag = tag,
                name = latest.optString("name").ifBlank { tag },
                body = latest.optString("body"),
                url = latest.optString("html_url"),
            )
        }.getOrNull()
    }

    private fun isNewer(candidate: String, current: String): Boolean {
        val a = versionParts(candidate)
        val b = versionParts(current)
        if (a.isEmpty() || b.isEmpty()) return candidate != current
        val size = maxOf(a.size, b.size)
        for (i in 0 until size) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun versionParts(value: String): List<Int> =
        Regex("\\d+").findAll(value).mapNotNull { it.value.toIntOrNull() }.toList()
}

object UpdatePromptState {
    var pending by mutableStateOf<UpdateInfo?>(null)
        private set

    fun show(info: UpdateInfo) {
        pending = info
    }

    fun dismiss() {
        pending = null
    }
}

@Composable
fun UpdatePromptHost(settings: SettingsStore) {
    LaunchedEffect(Unit) {
        if (settings.autoCheckUpdate) {
            UpdateChecker.check()?.let { UpdatePromptState.show(it) }
        }
    }
    UpdatePromptState.pending?.let { info ->
        UpdateDialog(info = info, onDismiss = { UpdatePromptState.dismiss() })
    }
}

@Composable
fun VersionUpdatedHost(settings: SettingsStore) {
    val context = LocalContext.current
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        show = settings.lastSeenVersion != BuildConfig.VERSION_NAME
    }
    if (show) {
        VersionUpdatedDialog(
            onDismiss = {
                settings.lastSeenVersion = BuildConfig.VERSION_NAME
                show = false
            },
            onViewChangelog = {
                settings.lastSeenVersion = BuildConfig.VERSION_NAME
                show = false
                com.aritxonly.bdfun.TeakNavigator.openChangelog(context = context)
            },
        )
    }
}

@Composable
private fun VersionUpdatedDialog(onDismiss: () -> Unit, onViewChangelog: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            TeakGlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp)
                    .clickable(interactionSource = null, indication = null) {},
                shape = RoundedCornerShape(22.dp),
                tintAlpha = 0.3f,
            ) {
                Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                    Text(
                        text = "版本更新",
                        color = colors.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "检测到您已更新版本，是否前往查看更新日志？",
                        color = colors.onBackgroundVariant,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        UpdateButton(
                            text = "取消",
                            primary = false,
                            modifier = Modifier.weight(1f),
                            onClick = onDismiss,
                        )
                        UpdateButton(
                            text = "前往查看",
                            primary = true,
                            modifier = Modifier.weight(1f),
                            onClick = onViewChangelog,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateDialog(info: UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            TeakGlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp)
                    .clickable(interactionSource = null, indication = null) {},
                shape = RoundedCornerShape(22.dp),
                tintAlpha = 0.3f,
            ) {
                Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                    Text(
                        text = "发现新版本",
                        color = colors.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = info.name,
                        color = colors.onBackgroundVariant,
                        fontSize = 13.sp,
                    )
                    if (info.body.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Text(
                                text = info.body,
                                color = colors.onBackgroundVariant,
                                fontSize = 12.5.sp,
                                lineHeight = 19.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        UpdateButton(
                            text = "我知道了",
                            primary = false,
                            modifier = Modifier.weight(1f),
                            onClick = onDismiss,
                        )
                        UpdateButton(
                            text = "前往更新",
                            primary = true,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(info.url)),
                                    )
                                }
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateButton(
    text: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(Capsule())
            .background(if (primary) colors.primary else colors.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (primary) 0.55f else 0.75f),
                shape = Capsule(),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (primary) Color.White else colors.onBackground,
            fontSize = 14.sp,
            fontWeight = if (primary) FontWeight.Normal else FontWeight.Normal,
        )
    }
}
