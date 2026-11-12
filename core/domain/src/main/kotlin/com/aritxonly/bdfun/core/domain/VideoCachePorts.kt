package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.cache.*

interface VideoCacheStorage {
    fun read(): List<CachedVideo>
    fun write(entries: List<CachedVideo>)
    fun prepare(id: String): CacheDestination
    fun delete(id: String)
    fun exists(path: String): Boolean
    fun uri(path: String): String
    fun commit(part: String, target: String)
    fun size(path: String): Long
    fun removePartial(destination: CacheDestination)
}
interface VideoCacheTransfer {
    suspend fun contentLength(url: String): Long
    suspend fun download(url: String, target: String, onBytes: suspend (Long) -> Unit): Outcome<Long>
    suspend fun cover(url: String, target: String): String
}
