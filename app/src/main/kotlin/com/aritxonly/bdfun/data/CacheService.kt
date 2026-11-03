package com.aritxonly.bdfun.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.aritxonly.bdfun.CacheActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 缓存下载的前台服务：只负责保活 + 通知进度，真正的下载在 VideoCacheManager 里。 */
class CacheService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notification = buildNotification()
        runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        }
        ticker = scope.launch {
            while (isActive) {
                delay(1000)
                if (!VideoCacheManager.hasActive()) {
                    stopSelf()
                    break
                }
                runCatching {
                    getSystemService(NotificationManager::class.java)
                        ?.notify(NOTIFICATION_ID, buildNotification())
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!VideoCacheManager.hasActive()) stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        ticker?.cancel()
        scope.cancel()
        runCatching {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        super.onDestroy()
    }

    private fun buildNotification(): android.app.Notification {
        val active = VideoCacheManager.items.filter {
            it.status == CacheStatus.Queued || it.status == CacheStatus.Downloading
        }
        val progress = when {
            active.isEmpty() -> 0
            active.size == 1 -> (active[0].progress * 100).toInt().coerceIn(0, 100)
            else -> (active.sumOf { it.progress.toDouble() } / active.size * 100)
                .toInt()
                .coerceIn(0, 100)
        }
        val text = when {
            active.isEmpty() -> "准备中"
            active.size > 1 -> "${active.size} 个视频 · $progress%"
            else -> "${active[0].title.take(18)} · $progress%"
        }
        val intent = Intent(this, CacheActivity::class.java)
        val pending = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("正在缓存视频")
            .setContentText(text)
            .setContentIntent(pending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "视频缓存",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "显示视频缓存下载进度"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "video_cache"
        private const val NOTIFICATION_ID = 4301

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, CacheService::class.java),
                )
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, CacheService::class.java)) }
        }
    }
}
