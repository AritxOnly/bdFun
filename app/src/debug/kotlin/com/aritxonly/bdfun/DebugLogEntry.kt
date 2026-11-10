package com.aritxonly.bdfun

import android.content.Context
import android.content.Intent

/** 仅 debug 包提供日志抓取入口。 */
fun openLogs(context: Context) {
    context.startActivity(Intent(context, LogActivity::class.java))
}
