package com.aritxonly.bdfun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.core.application.BackgroundQueueLoader
import com.aritxonly.bdfun.core.application.PlaybackSessionStateHolder
import com.aritxonly.bdfun.infra.playback.AndroidBackgroundPlayback
import com.aritxonly.bdfun.infra.playback.BackgroundPlaybackPlatformPorts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

internal val BackgroundPlayback get() = AppGraph.backgroundPlayback

/** 只保留原Compose投影；原生持有、交接、服务与释放实现在infra:playback。 */
internal class BackgroundPlaybackStateHolder(
    session: PlaybackSessionStateHolder, processScope: CoroutineScope,
    queue: BackgroundQueueLoader, ports: BackgroundPlaybackPlatformPorts,
) : AndroidBackgroundPlayback(session, queue, ports) {
    private var observed by mutableStateOf(session.state.value)
    override val snapshot get() = observed
    override fun sync() { observed = session.state.value }
    init {
        processScope.launch(start = CoroutineStart.UNDISPATCHED) {
            session.state.collect { observed = session.state.value }
        }
    }
}
