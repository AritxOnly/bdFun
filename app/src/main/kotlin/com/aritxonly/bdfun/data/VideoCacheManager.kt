package com.aritxonly.bdfun.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class CacheStatus { Queued, Downloading, Completed, Failed }

/** 一条离线缓存记录（只缓存当前视频当前 P）。 */
data class CachedVideo(
    val id: String,
    val bvid: String,
    val aid: Long,
    val cid: Long,
    val title: String,
    val cover: String,
    val coverPath: String,
    val ownerName: String,
    val ownerMid: Long,
    val durationSec: Int,
    val qualityId: Int,
    val qualityLabel: String,
    val videoPath: String,
    val audioPath: String,
    val progress: Float,
    val bytes: Long,
    val totalBytes: Long,
    val status: CacheStatus,
    val createdAt: Long,
    val error: String? = null,
) {
    val localCover: String
        get() = if (coverPath.isNotBlank() && File(coverPath).exists()) {
            Uri.fromFile(File(coverPath)).toString()
        } else {
            cover
        }

    fun toVideoItem(): VideoItem = VideoItem(
        bvid = bvid,
        aid = aid,
        title = title,
        cover = localCover,
        ownerName = ownerName,
        ownerFace = "",
        ownerMid = ownerMid,
        play = 0L,
        danmaku = 0L,
        duration = durationSec,
        pubdate = 0L,
        cid = cid,
    )

    fun toDetail(): VideoDetail = VideoDetail(
        bvid = bvid,
        aid = aid,
        title = title,
        cover = localCover,
        desc = "",
        ownerName = ownerName,
        ownerFace = "",
        ownerMid = ownerMid,
        ownerSign = "",
        play = 0L,
        danmaku = 0L,
        like = 0L,
        coin = 0L,
        favorite = 0L,
        share = 0L,
        reply = 0L,
        pubdate = 0L,
        duration = durationSec,
        cid = cid,
        tname = "",
        pages = listOf(VideoPage(cid = cid, page = 1, part = title, duration = durationSec)),
        episodes = emptyList(),
    )

    fun toPlayInfo(): PlayInfo {
        val videoUrl = Uri.fromFile(File(videoPath)).toString()
        val audioUrl = if (audioPath.isNotBlank() && File(audioPath).exists()) {
            Uri.fromFile(File(audioPath)).toString()
        } else {
            null
        }
        return PlayInfo(
            qualities = listOf(
                Quality(
                    id = qualityId,
                    label = qualityLabel,
                    width = 0,
                    height = 0,
                    frameRate = "",
                    codec = "",
                    videoUrl = videoUrl,
                ),
            ),
            audioUrl = audioUrl,
            fallbackUrl = null,
            durationMs = durationSec * 1000L,
            acceptQuality = emptyList(),
        )
    }
}

/**
 * 视频缓存：DASH 的视频轨 / 音轨各下一份到应用私有目录，播放时用本地文件拼回一路。
 * 下载在进程内协程里跑，前台服务只负责保活和通知进度。
 */
object VideoCacheManager {
    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val cancelledIds = ConcurrentHashMap.newKeySet<String>()
    private val client = com.aritxonly.bdfun.data.HttpClients.base.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val entries = mutableStateListOf<CachedVideo>()
    @Volatile
    private var loaded = false
    @Volatile
    private var loadStarted = false

    val items: List<CachedVideo> get() = entries

    fun init(context: Context) {
        if (loadStarted || loaded) return
        synchronized(this) {
            if (loadStarted || loaded) return
            appContext = context.applicationContext
            loadStarted = true
        }
        // 缓存 JSON 的读取和解析放到后台，避免拖慢首屏；解析完成后再回主线程填充列表。
        scope.launch {
            val cached = readEntries(appContext)
            withContext(Dispatchers.Main.immediate) {
                val existingIds = entries.mapTo(HashSet()) { it.id }
                entries.addAll(cached.filter { it.id !in existingIds })
                loaded = true
            }
        }
    }

    fun hasActive(): Boolean = entries.any {
        it.status == CacheStatus.Queued || it.status == CacheStatus.Downloading
    }

    /** 找一个能离线播放的缓存（cid 为 0 时只按 bvid 找）。 */
    fun findPlayable(bvid: String, cid: Long): CachedVideo? = entries.firstOrNull { entry ->
        entry.status == CacheStatus.Completed &&
            entry.bvid == bvid &&
            (cid <= 0L || entry.cid == cid) &&
            File(entry.videoPath).exists() &&
            (entry.audioPath.isBlank() || File(entry.audioPath).exists())
    }

    fun enqueue(
        detail: VideoDetail,
        quality: Quality,
        cid: Long,
        audioUrl: String?,
        durationSec: Int,
    ): Boolean {
        if (!::appContext.isInitialized) return false
        if (audioUrl.isNullOrBlank()) return false
        val id = "${detail.bvid}_${cid}_${quality.id}"
        val existing = entries.firstOrNull { it.id == id }
        if (existing != null &&
            (existing.status == CacheStatus.Completed ||
                existing.status == CacheStatus.Queued ||
                existing.status == CacheStatus.Downloading)
        ) {
            return false
        }
        val dir = File(cacheRoot(), id)
        dir.mkdirs()
        val entry = CachedVideo(
            id = id,
            bvid = detail.bvid,
            aid = detail.aid,
            cid = cid,
            title = detail.title,
            cover = detail.cover,
            coverPath = "",
            ownerName = detail.ownerName,
            ownerMid = detail.ownerMid,
            durationSec = durationSec,
            qualityId = quality.id,
            qualityLabel = quality.label,
            videoPath = "",
            audioPath = "",
            progress = 0f,
            bytes = 0L,
            totalBytes = 0L,
            status = CacheStatus.Queued,
            createdAt = System.currentTimeMillis(),
        )
        entries.removeAll { it.id == id }
        entries.add(entry)
        cancelledIds.remove(id)
        persistNow()
        CacheService.start(appContext)
        jobs[id] = scope.launch {
            runDownload(entry, detail, quality, audioUrl, dir)
        }
        return true
    }

    fun delete(id: String) {
        jobs.remove(id)?.let {
            cancelledIds.add(id)
            it.cancel()
        }
        val entry = entries.firstOrNull { it.id == id }
        entry?.let { File(cacheRoot(), it.id).deleteRecursively() }
        File(cacheRoot(), id).deleteRecursively()
        entries.removeAll { it.id == id }
        persistNow()
    }

    private suspend fun runDownload(
        initial: CachedVideo,
        detail: VideoDetail,
        quality: Quality,
        audioUrl: String,
        dir: File,
    ) {
        if (cancelledIds.remove(initial.id)) return
        var current = initial.copy(status = CacheStatus.Downloading)
        update(current)
        var lastPublished = 0L
        var lastPersisted = 0L
        try {
            val videoPart = File(dir, "video.part")
            val audioPart = File(dir, "audio.part")
            val videoFile = File(dir, "video.m4s")
            val audioFile = File(dir, "audio.m4s")

            val videoLength = contentLength(quality.videoUrl)
            val audioLength = contentLength(audioUrl)
            val knownTotal = (videoLength.coerceAtLeast(0L) + audioLength.coerceAtLeast(0L))
                .takeIf { it > 0L } ?: 0L
            var videoDone = 0L
            var audioDone = 0L

            download(quality.videoUrl, videoPart) { done ->
                if (cancelledIds.contains(initial.id)) throw CancellationException("已删除")
                videoDone = done
                val now = System.currentTimeMillis()
                if (now - lastPublished >= 220L) {
                    lastPublished = now
                    val total = if (knownTotal > 0L) knownTotal else done + audioDone
                    current = current.copy(
                        bytes = done + audioDone,
                        totalBytes = total,
                        progress = if (total > 0L) {
                            ((done + audioDone).toFloat() / total).coerceIn(0f, 0.98f)
                        } else {
                            0f
                        },
                    )
                    update(current)
                    if (now - lastPersisted >= 5000L) {
                        lastPersisted = now
                        withContext(Dispatchers.Main.immediate) { persistNow() }
                    }
                }
            }
            if (!videoPart.renameTo(videoFile)) {
                videoPart.copyTo(videoFile, overwrite = true)
                videoPart.delete()
            }

            download(audioUrl, audioPart) { done ->
                if (cancelledIds.contains(initial.id)) throw CancellationException("已删除")
                audioDone = done
                val now = System.currentTimeMillis()
                if (now - lastPublished >= 220L) {
                    lastPublished = now
                    val total = if (knownTotal > 0L) knownTotal else videoDone + done
                    current = current.copy(
                        bytes = videoDone + done,
                        totalBytes = total,
                        progress = if (total > 0L) {
                            ((videoDone + done).toFloat() / total).coerceIn(0f, 0.99f)
                        } else {
                            0f
                        },
                    )
                    update(current)
                    if (now - lastPersisted >= 5000L) {
                        lastPersisted = now
                        withContext(Dispatchers.Main.immediate) { persistNow() }
                    }
                }
            }
            if (!audioPart.renameTo(audioFile)) {
                audioPart.copyTo(audioFile, overwrite = true)
                audioPart.delete()
            }

            val coverPath = downloadCover(detail.cover, dir)
            val bytes = videoFile.length() + audioFile.length()
            if (cancelledIds.contains(initial.id)) {
                dir.deleteRecursively()
                return
            }
            current = current.copy(
                status = CacheStatus.Completed,
                progress = 1f,
                bytes = bytes,
                totalBytes = bytes,
                videoPath = videoFile.absolutePath,
                audioPath = audioFile.absolutePath,
                coverPath = coverPath,
            )
            update(current)
            withContext(Dispatchers.Main.immediate) { persistNow() }
        } catch (e: CancellationException) {
            File(dir, "video.part").delete()
            File(dir, "audio.part").delete()
            throw e
        } catch (e: Exception) {
            File(dir, "video.part").delete()
            File(dir, "audio.part").delete()
            if (cancelledIds.contains(initial.id)) return
            current = current.copy(
                status = CacheStatus.Failed,
                error = e.message ?: "下载失败",
            )
            update(current)
            withContext(Dispatchers.Main.immediate) { persistNow() }
        } finally {
            jobs.remove(initial.id)
            cancelledIds.remove(initial.id)
        }
    }

    private suspend fun download(
        url: String,
        target: File,
        onBytes: suspend (Long) -> Unit,
    ): Long = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", BiliApi.UA)
            .header("Referer", "https://www.bilibili.com/")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("空响应")
            var total = 0L
            target.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        total += read
                        onBytes(total)
                    }
                }
            }
            total
        }
    }

    private suspend fun contentLength(url: String): Long = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", BiliApi.UA)
                .header("Referer", "https://www.bilibili.com/")
                .head()
                .build()
            client.newCall(request).execute().use { response ->
                response.header("Content-Length")?.toLongOrNull() ?: -1L
            }
        }.getOrDefault(-1L)
    }

    private suspend fun downloadCover(url: String, dir: File): String {
        if (url.isBlank()) return ""
        val target = File(dir, "cover.jpg")
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", BiliApi.UA)
                .header("Referer", "https://www.bilibili.com/")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return ""
                val body = response.body ?: return ""
                target.outputStream().use { output ->
                    body.byteStream().use { input -> input.copyTo(output) }
                }
            }
            if (target.length() > 0) target.absolutePath else ""
        }.getOrDefault("")
    }

    private fun cacheRoot(): File {
        val base = appContext.getExternalFilesDir(null) ?: appContext.filesDir
        return File(base, "video_cache").apply { mkdirs() }
    }

    private suspend fun update(entry: CachedVideo) = withContext(Dispatchers.Main.immediate) {
        val index = entries.indexOfFirst { it.id == entry.id }
        if (index >= 0) entries[index] = entry else entries.add(entry)
    }

    private fun persistNow() {
        if (!loaded) return
        val array = JSONArray()
        entries.forEach { array.put(it.toJson()) }
        appContext.getSharedPreferences("video_cache", Context.MODE_PRIVATE)
            .edit()
            .putString("items", array.toString())
            .apply()
    }

    private fun readEntries(context: Context): List<CachedVideo> {
        val raw = context.getSharedPreferences("video_cache", Context.MODE_PRIVATE)
            .getString("items", null) ?: return emptyList()
        return runCatching {
            val result = ArrayList<CachedVideo>()
            val array = JSONArray(raw)
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                var entry = CachedVideo(
                    id = item.optString("id"),
                    bvid = item.optString("bvid"),
                    aid = item.optLong("aid"),
                    cid = item.optLong("cid"),
                    title = item.optString("title"),
                    cover = item.optString("cover"),
                    coverPath = item.optString("coverPath"),
                    ownerName = item.optString("ownerName"),
                    ownerMid = item.optLong("ownerMid"),
                    durationSec = item.optInt("durationSec"),
                    qualityId = item.optInt("qualityId"),
                    qualityLabel = item.optString("qualityLabel"),
                    videoPath = item.optString("videoPath"),
                    audioPath = item.optString("audioPath"),
                    progress = item.optDouble("progress", 0.0).toFloat(),
                    bytes = item.optLong("bytes"),
                    totalBytes = item.optLong("totalBytes"),
                    status = runCatching {
                        CacheStatus.valueOf(item.optString("status"))
                    }.getOrDefault(CacheStatus.Failed),
                    createdAt = item.optLong("createdAt"),
                    error = item.optString("error").ifBlank { null },
                )
                // 上次进程被杀时还在下载的条目，文件不完整，标记失败让用户重下。
                if (entry.status == CacheStatus.Queued || entry.status == CacheStatus.Downloading) {
                    entry = entry.copy(status = CacheStatus.Failed, error = "下载已中断")
                }
                if (entry.status == CacheStatus.Completed &&
                    (!File(entry.videoPath).exists() ||
                        (entry.audioPath.isNotBlank() && !File(entry.audioPath).exists()))
                ) {
                    entry = entry.copy(status = CacheStatus.Failed, error = "缓存文件已丢失")
                }
                if (entry.id.isNotBlank()) result.add(entry)
            }
            result
        }.getOrDefault(emptyList())
    }

    private fun CachedVideo.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("bvid", bvid)
        put("aid", aid)
        put("cid", cid)
        put("title", title)
        put("cover", cover)
        put("coverPath", coverPath)
        put("ownerName", ownerName)
        put("ownerMid", ownerMid)
        put("durationSec", durationSec)
        put("qualityId", qualityId)
        put("qualityLabel", qualityLabel)
        put("videoPath", videoPath)
        put("audioPath", audioPath)
        put("progress", progress.toDouble())
        put("bytes", bytes)
        put("totalBytes", totalBytes)
        put("status", status.name)
        put("createdAt", createdAt)
        put("error", error.orEmpty())
    }
}
