package com.aritxonly.bdfun.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File

/** 只由可见表情的 AsyncImage 发起请求；独立缓存避免被首页退后台的清理影响。 */
internal fun createEmoteImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
    .memoryCache { MemoryCache.Builder(context).maxSizeBytes(8 * 1024 * 1024).build() }
    .diskCache {
        DiskCache.Builder().directory(File(context.filesDir, "emote_images"))
            .maxSizeBytes(128L * 1024 * 1024).build()
    }
    // B 站表情使用内容 URL；命中即复用，避免重进页面时反复校验网络。
    .respectCacheHeaders(false)
    .build()
