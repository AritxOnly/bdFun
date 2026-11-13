package com.aritxonly.bdfun.infra.storage

import android.content.Context
import android.net.Uri
import com.aritxonly.bdfun.core.domain.VideoCacheStorage
import com.aritxonly.bdfun.core.model.cache.*
import java.io.File

class AndroidVideoCacheStorage(context: Context) : VideoCacheStorage {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("video_cache", Context.MODE_PRIVATE)
    override fun read(): List<CachedVideo> {
        val raw = prefs.getString("items", null) ?: return emptyList()
        return decodeVideoCache(raw, ::exists)
    }
    override fun write(entries: List<CachedVideo>) { prefs.edit().putString("items", encodeVideoCache(entries)).apply() }
    override fun prepare(id: String): CacheDestination {
        val dir = File(cacheRoot(), id).apply { mkdirs() }
        return CacheDestination(dir.absolutePath, File(dir, "video.part").absolutePath, File(dir, "audio.part").absolutePath,
            File(dir, "video.m4s").absolutePath, File(dir, "audio.m4s").absolutePath, File(dir, "cover.jpg").absolutePath)
    }
    override fun delete(id: String) { File(cacheRoot(), id).deleteRecursively() }
    override fun exists(path: String) = File(path).exists()
    override fun uri(path: String) = Uri.fromFile(File(path)).toString()
    override fun size(path: String) = File(path).length()
    override fun commit(part: String, target: String) {
        val source = File(part)
        val destination = File(target)
        if (!source.renameTo(destination)) {
            source.copyTo(destination, overwrite = true)
            source.delete()
        }
    }
    override fun removePartial(destination: CacheDestination) {
        File(destination.videoPart).delete()
        File(destination.audioPart).delete()
    }
    private fun cacheRoot(): File {
        val base = app.getExternalFilesDir(null) ?: app.filesDir
        return File(base, "video_cache").apply { mkdirs() }
    }
}
