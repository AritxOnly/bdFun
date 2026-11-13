package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.CachedEmotes
import com.aritxonly.bdfun.core.domain.EmoteCache
import com.aritxonly.bdfun.core.model.comment.EmoteItem
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/** 元数据按账号保存；图片由按 URL 共享的图片缓存管理，不在这里批量下载。 */
class FileEmoteCache(private val directory: File) : EmoteCache {
    private val lock = Mutex()
    override suspend fun read(accountId: Long): Outcome<CachedEmotes?> = access {
        val file = file(accountId)
        if (!file.isFile) return@access null
        val root = JSONObject(file.readText())
        require(root.getInt("version") == 1)
        val rows = root.getJSONArray("items")
        CachedEmotes(List(rows.length()) { index ->
            val row = rows.getJSONObject(index)
            EmoteItem(row.getString("text"), row.getString("url"), row.getInt("packageId"),
                row.getString("packageIcon"), row.getInt("type"), row.getInt("size"))
        }, root.getLong("savedAt"))
    }

    override suspend fun write(accountId: Long, snapshot: CachedEmotes): Outcome<Unit> = access {
        check(directory.isDirectory || directory.mkdirs())
        val rows = JSONArray()
        snapshot.items.forEach { item ->
            rows.put(JSONObject().put("text", item.text).put("url", item.url)
                .put("packageId", item.packageId).put("packageIcon", item.packageIcon)
                .put("type", item.type).put("size", item.size))
        }
        val root = JSONObject().put("version", 1).put("savedAt", snapshot.savedAt).put("items", rows)
        val target = file(accountId)
        val temporary = File(directory, "${target.name}.tmp")
        try {
            temporary.writeText(root.toString())
            // 同目录原子替换，写入中断时仍保留上一份完整列表。
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING)
        } finally { temporary.delete() }
        Unit
    }

    private fun file(accountId: Long) = File(directory, "${accountId.coerceAtLeast(0)}.json")

    private suspend fun <T> access(action: () -> T): Outcome<T> = withContext(Dispatchers.IO) {
        lock.withLock {
            try { Outcome.Success(action()) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { Outcome.Failure(AppError.Unknown(error.message ?: "表情缓存读写失败")) }
        }
    }
}
