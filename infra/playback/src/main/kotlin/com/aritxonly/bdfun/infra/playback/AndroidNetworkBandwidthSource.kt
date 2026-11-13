package com.aritxonly.bdfun.infra.playback

import android.content.Context
import android.net.ConnectivityManager
import com.aritxonly.bdfun.core.domain.NetworkBandwidthSource

/** 只读取原系统提示值，不执行测速或修改网络；无网络/读取失败保持原0值回退。 */
class AndroidNetworkBandwidthSource(context: Context?) : NetworkBandwidthSource {
    private val context = context?.applicationContext
    override fun downstreamKbps(): Int = runCatching {
        val cm = context?.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        caps?.linkDownstreamBandwidthKbps ?: 0
    }.getOrDefault(0)
}
