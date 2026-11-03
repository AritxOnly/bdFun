package com.aritxonly.teak.common.chrome

/** 全局动效开关：关闭后所有手写补间/弹簧直接落到目标值。 */
object TeakMotion {
    @Volatile
    var enabled: Boolean = true
}
