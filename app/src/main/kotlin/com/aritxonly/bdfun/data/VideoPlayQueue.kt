package com.aritxonly.bdfun.data

/** 迁移期旧入口；实际状态由 AppContainer 的 PlaybackQueueStateHolder 持有。 */
internal val VideoPlayQueue get() = com.aritxonly.bdfun.AppGraph.playbackQueue
