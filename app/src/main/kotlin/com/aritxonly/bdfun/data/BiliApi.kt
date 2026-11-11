package com.aritxonly.bdfun.data

import android.content.Context
import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.bdfun.infra.bili.BiliBackend

/** 旧包名兼容入口；网络实现不再依赖 app。 */
class BiliApi(context: Context) : BiliBackend(context, BuildConfig.DEBUG, LogCollector::record, ::cdnServiceById) {
    companion object {
        const val API = BiliBackend.API
        const val APP_API = BiliBackend.APP_API
        const val APP_UA = BiliBackend.APP_UA
        const val APP = BiliBackend.APP
        const val PASSPORT = BiliBackend.PASSPORT
        const val VC = BiliBackend.VC
        const val UA = BiliBackend.UA
        fun image(url: String, width: Int = 0) = BiliBackend.image(url, width)
        fun avatar(url: String, size: Int) = BiliBackend.avatar(url, size)
        fun cleanHtml(text: String) = BiliBackend.cleanHtml(text)
        fun aidToBvid(aid: Long) = BiliBackend.aidToBvid(aid)
        fun parseCountText(text: String) = BiliBackend.parseCountText(text)
        fun parseDurationText(text: String) = BiliBackend.parseDurationText(text)
        fun qualityLabel(id: Int) = BiliBackend.qualityLabel(id)
    }
}
