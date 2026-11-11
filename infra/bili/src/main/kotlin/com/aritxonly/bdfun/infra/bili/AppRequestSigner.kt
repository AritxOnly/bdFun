package com.aritxonly.bdfun.infra.bili

/** App/TV 签名与 WBI 分开，避免两套 appkey 和 access_key 混用。 */
class AppRequestSigner(
    private val encode: (String) -> String,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun sign(
        params: MutableMap<String, String>,
        appKey: String,
        appSec: String,
        accessKey: String?,
    ) {
        params["appkey"] = appKey
        params["ts"] = (clock() / 1000L).toString()
        accessKey?.takeIf { it.isNotBlank() }?.let { params["access_key"] = it }
        val query = params.toSortedMap().entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        params["sign"] = WbiSigner.md5(query + appSec)
    }
}
