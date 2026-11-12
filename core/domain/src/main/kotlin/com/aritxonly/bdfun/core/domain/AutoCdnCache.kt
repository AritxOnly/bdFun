package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.settings.AutoCdnSnapshot

interface AutoCdnCache {
    fun read(): AutoCdnSnapshot
    fun write(snapshot: AutoCdnSnapshot)
}
