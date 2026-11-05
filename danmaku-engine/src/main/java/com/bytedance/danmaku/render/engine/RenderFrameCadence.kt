package com.bytedance.danmaku.render.engine

/** Choreographer owns the cadence: never turn 90/120Hz into uneven 60fps updates. */
internal class RenderFrameCadence {
    private var lastFrameNs: Long? = null

    /** 最小渲染间隔；0 表示不限制（跟屏幕刷新率）。 */
    var minIntervalNs: Long = 0L

    fun shouldRender(frameTimeNs: Long): Boolean {
        val last = lastFrameNs ?: return true
        if (frameTimeNs <= last) return false
        return minIntervalNs <= 0L || frameTimeNs - last >= minIntervalNs
    }

    fun onFrame(frameTimeNs: Long) { lastFrameNs = frameTimeNs }

    fun reset() { lastFrameNs = null }
}
