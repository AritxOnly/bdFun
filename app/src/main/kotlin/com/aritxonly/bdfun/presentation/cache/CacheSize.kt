package com.aritxonly.bdfun.ui



internal fun formatCacheSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / 1024.0 / 1024.0
    return if (mb >= 1024) {
        "%.2f GB".format(mb / 1024)
    } else {
        "%.1f MB".format(mb)
    }
}
