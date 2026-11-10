package com.aritxonly.bdfun.data

import android.content.Context
import android.os.Build
import com.aritxonly.bdfun.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * 轻量应用日志收集：
 * - 关键事件写入内存环形缓冲 + filesDir，进程被杀也能留下最后一段；
 * - 未捕获异常自动记录；
 * - 导出时把应用日志、设备信息、logcat 快照合并成一个 txt。
 *
 * 只记录调试所需信息，不主动读取 cookie/token。
 */
object LogCollector {
    private const val MAX_MEMORY_LINES = 3000
    private const val MAX_FILE_BYTES = 2 * 1024 * 1024

    private val lines = ArrayDeque<String>()
    private val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

    @Volatile
    private var initialized = false

    private var logFile: File? = null
    private var appContext: Context? = null

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        val dir = File(context.filesDir, "bdFun-logs").apply { mkdirs() }
        logFile = File(dir, "current.log")
        record(
            "APP",
            "start version=${BuildConfig.VERSION_NAME} code=${BuildConfig.VERSION_CODE} " +
                "sdk=${Build.VERSION.SDK_INT} device=${Build.MANUFACTURER} ${Build.MODEL}",
        )
        installCrashHandler()
    }

    @Synchronized
    fun record(tag: String, message: String) {
        val line = "${timeFormat.format(Date())} [${Thread.currentThread().name}] [$tag] $message"
        lines.addLast(line)
        while (lines.size > MAX_MEMORY_LINES) lines.removeFirst()
        runCatching {
            val file = logFile ?: return@runCatching
            if (file.length() > MAX_FILE_BYTES) {
                file.writeText("")
            }
            file.appendText(line + "\n")
        }
    }

    @Synchronized
    fun clear() {
        lines.clear()
        runCatching { logFile?.writeText("") }
        record("APP", "log cleared")
    }

    @Synchronized
    fun beginSession() {
        clear()
        record("APP", "capture session started")
    }

    @Synchronized
    fun snapshot(): String = lines.joinToString("\n")

    @Synchronized
    fun tail(count: Int): String =
        lines.toList().takeLast(count).joinToString("\n")

    @Synchronized
    fun size(): Int = lines.size

    fun export(context: Context): File {
        val sb = StringBuilder()
        sb.appendLine("bdFun debug log")
        sb.appendLine("version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        sb.appendLine("package: ${context.packageName}")
        sb.appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("android: ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
        sb.appendLine("exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        sb.appendLine()
        sb.appendLine("===== app log =====")
        sb.appendLine(snapshot())
        sb.appendLine()
        sb.appendLine("===== logcat snapshot =====")
        sb.appendLine(captureLogcat())
        val dir = File(context.cacheDir, "bdFun-logs").apply { mkdirs() }
        val out = File(dir, "bdFun-log-${fileFormat.format(Date())}.txt")
        out.writeText(sb.toString())
        return out
    }

    private fun captureLogcat(): String {
        val pid = android.os.Process.myPid()
        val commands = listOf(
            arrayOf("logcat", "-d", "-t", "4000", "--pid=$pid"),
            arrayOf("logcat", "-d", "-t", "2000"),
        )
        for (cmd in commands) {
            val result = runCatching {
                val process = ProcessBuilder(*cmd)
                    .redirectErrorStream(true)
                    .start()
                val text = process.inputStream.bufferedReader().use { it.readText() }
                process.waitFor()
                text
            }.getOrNull()
            if (!result.isNullOrBlank() && !result.contains("Permission denied")) {
                return result.take(300_000)
            }
        }
        return "(logcat unavailable; app log only)"
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                record(
                    "CRASH",
                    "thread=${thread.name} ${throwable.javaClass.name}: ${throwable.message}\n" +
                        throwable.stackTraceToString(),
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
