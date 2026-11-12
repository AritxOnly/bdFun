package com.aritxonly.bdfun.data

/** 旧入口无存储，按原 SharedPreferences 格式委托进程依赖。 */
internal val PlaybackProgressStore get() = com.aritxonly.bdfun.AppGraph.playbackProgress
