package com.aritxonly.bdfun.core.domain

/** 旋转只为紧凑设备自动进入全屏；展开的大屏保留分栏和手动全屏选择。 */
fun fullscreenAfterVideoRotation(landscape: Boolean, smallestWidthDp: Int, autoRotate: Boolean): Boolean? = when {
    !landscape -> false
    autoRotate && smallestWidthDp in 1 until 600 -> true
    else -> null
}
