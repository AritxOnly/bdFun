package com.aritxonly.bdfun.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 专门用来抓“界面卡住”的日志。
 *
 * 关键设计：主线程只负责把日志塞进队列，真正的写文件在专用线程完成，
 * 所以日志本身不会因为写盘/抢锁把界面卡住。
 * 看门狗只看主线程心跳；卡住超过 800ms 时只抓主线程调用栈（抓全部线程会让整个进程暂停）。
 */
/** 只用于 debug 日志：当前还活着的关键界面实例数量。 */
object LiveCounters {
    @Volatile var discInstances = 0
    @Volatile var spaceInstances = 0
    @Volatile var homeInstances = 0
}

object FreezeWatchdog {
    private const val MAX_BYTES = 2 * 1024 * 1024L
    private const val STALL_MS = 800L
    private const val COOLDOWN_MS = 4000L

    private val formatter = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val queue = ConcurrentLinkedQueue<String>()

    @Volatile private var file: File? = null
    @Volatile private var started = false
    @Volatile private var heartbeat = 0L
    @Volatile private var lastStallAt = 0L

    @Volatile private var lastFrameNanos = 0L

    fun start(context: Context) {
        // 只给 debug 包用：生产包里每帧挂 Choreographer + 常驻看门狗会让设备
        // 一直保持高刷新/耗电，而且用户也拿不到这份日志。
        if (!com.aritxonly.bdfun.BuildConfig.DEBUG) return
        if (started) return
        started = true
        file = File(context.applicationContext.filesDir, "freeze.log")
        enqueue("=== bdFun freeze watchdog started pid=${android.os.Process.myPid()} ===")
        installUncaughtHandler()
        startWriter()
        mainHandler.post(tick)
        mainHandler.post { Choreographer.getInstance().postFrameCallback(frameTick) }
        Thread({ watchLoop() }, "bdFun-freeze-watchdog").apply {
            isDaemon = true
            start()
        }
    }

    /** 关键路径埋点：只是入队，主线程不做任何文件操作。 */
    inline fun log(tag: String, message: String) {
        if (!com.aritxonly.bdfun.BuildConfig.DEBUG) return
        logInternal(tag, message)
    }

    @PublishedApi
    internal fun logInternal(tag: String, message: String) {
        enqueue("[$tag] $message")
    }

    fun exportTarget(context: Context): File = File(context.filesDir, "freeze.log")

    fun readTail(context: Context, maxLines: Int = 500): String {
        val f = exportTarget(context)
        if (!f.exists()) return ""
        return runCatching { f.readLines().takeLast(maxLines).joinToString("\n") }
            .getOrDefault("")
    }

    fun size(context: Context): Long =
        runCatching { exportTarget(context).length() }.getOrDefault(0L)

    fun clear(context: Context) {
        runCatching { exportTarget(context).delete() }
        enqueue("=== freeze log cleared ===")
    }

    private val tick = object : Runnable {
        override fun run() {
            heartbeat = SystemClock.elapsedRealtime()
            mainHandler.postDelayed(this, 50L)
        }
    }

    private val frameTick = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val last = lastFrameNanos
            lastFrameNanos = frameTimeNanos
            if (last != 0L) {
                val gapMs = (frameTimeNanos - last) / 1_000_000f
                if (gapMs > 300f) {
                    enqueue("!!! FRAME GAP ${gapMs.toInt()}ms !!!")
                }
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun watchLoop() {
        var lastMemLogAt = 0L
        while (true) {
            try {
                Thread.sleep(100L)
            } catch (_: InterruptedException) {
                return
            }
            val now = SystemClock.elapsedRealtime()
            // 每 5 秒记一次 Java 堆和界面实例数量：用来区分“泄漏”还是“短时间分配冲高”。
            if (now - lastMemLogAt >= 5_000L) {
                lastMemLogAt = now
                runCatching {
                    val runtime = Runtime.getRuntime()
                    val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                    val maxMb = runtime.maxMemory() / (1024 * 1024)
                    enqueue(
                        "MEM used=${usedMb}MB max=${maxMb}MB " +
                            "discs=${LiveCounters.discInstances} " +
                            "spaces=${LiveCounters.spaceInstances} " +
                            "homes=${LiveCounters.homeInstances}",
                    )
                }
            }
            val beat = heartbeat
            if (beat == 0L) continue
            val lag = now - beat
            if (lag >= STALL_MS && now - lastStallAt >= COOLDOWN_MS) {
                lastStallAt = now
                enqueue("!!! MAIN THREAD STALL ${lag}ms !!!")
                // 连续采样：卡住期间每 120ms 抓一次主线程栈，只抓主线程。
                // 单次抓栈可能正好落在“已经恢复”的位置，连续采样才能看到真正耗时的那段。
                var sample = 0
                while (sample < 25) {
                    val lagNow = SystemClock.elapsedRealtime() - heartbeat
                    if (lagNow < 300L) {
                        enqueue("--- recovered (sample $sample, lag=${lagNow}ms) ---")
                        break
                    }
                    enqueue("--- sample $sample lag=${lagNow}ms ---")
                    runCatching {
                        val main = Looper.getMainLooper().thread
                        main.stackTrace.take(14).forEach { enqueue("    at $it") }
                    }
                    sample++
                    try {
                        Thread.sleep(120L)
                    } catch (_: InterruptedException) {
                        return
                    }
                }
                enqueue("--- end of stall samples ---")
            }
        }
    }

    private fun startWriter() {
        Thread({
            while (true) {
                val line = queue.poll()
                if (line == null) {
                    try {
                        Thread.sleep(60L)
                    } catch (_: InterruptedException) {
                        return@Thread
                    }
                    continue
                }
                writeLine(line)
            }
        }, "bdFun-freeze-writer").apply {
            isDaemon = true
            start()
        }
    }

    private fun installUncaughtHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                writeLine("!!! UNCAUGHT on ${thread.name}: ${error.javaClass.name}: ${error.message}")
                error.stackTrace.take(40).forEach { writeLine("    at $it") }
            }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun enqueue(line: String) {
        queue.add(line)
    }

    private fun writeLine(line: String) {
        val f = file ?: return
        val text = "${formatter.format(Date())} $line\n"
        runCatching {
            if (f.exists() && f.length() > MAX_BYTES) {
                val lines = f.readLines()
                f.writeText(lines.drop(lines.size / 2).joinToString("\n") + "\n")
            }
            FileOutputStream(f, true).use { out ->
                out.write(text.toByteArray())
                out.flush()
            }
        }
    }
}
