package com.aritxonly.bdfun.core.domain

/** 直播页只控制传输，不感知Media3实例或原生View。 */
interface LivePlayback {
    fun prepare(url: String)
    fun play()
    fun pause()
    fun release()
}
