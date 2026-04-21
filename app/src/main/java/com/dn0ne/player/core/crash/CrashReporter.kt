package com.dn0ne.player.core.crash

import android.content.Context
import android.os.Build
import com.dn0ne.player.core.util.getAppVersionName
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * On-device, zero-telemetry crash reporter.
 *
 * Writes uncaught exceptions to a local file in the app's private storage, then
 * delegates to the previous [Thread.UncaughtExceptionHandler] so Android's
 * normal crash flow still runs. Nothing ever leaves the device — users attach
 * the file manually when filing a bug.
 *
 * Log location: `filesDir/crash-logs/crash-<timestamp>.txt`. Only the most
 * recent [MAX_LOGS] files are retained.
 */
class CrashReporter(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val previousHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            writeReport(thread, throwable)
        } catch (_: Throwable) {
            // Never let the reporter itself crash the crash path.
        }
        previousHandler?.uncaughtException(thread, throwable)
    }

    private fun writeReport(thread: Thread, throwable: Throwable) {
        val dir = File(context.filesDir, LOG_DIR_NAME).apply { mkdirs() }
        rotate(dir)

        val timestamp = FILENAME_FORMAT.format(Date())
        val file = File(dir, "crash-$timestamp.txt")

        file.writeText(buildReport(thread, throwable))
    }

    private fun buildReport(thread: Thread, throwable: Throwable): String {
        val stack = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }
        return buildString {
            appendLine("Lotus crash report")
            appendLine("Time (UTC): ${ISO_FORMAT.format(Date())}")
            appendLine("App version: ${context.getAppVersionName()}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("Thread: ${thread.name}")
            appendLine()
            appendLine("--- Stack trace ---")
            appendLine(stack.toString())
        }
    }

    private fun rotate(dir: File) {
        val logs = dir.listFiles { f -> f.isFile && f.name.startsWith("crash-") }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        logs.drop(MAX_LOGS - 1).forEach { it.delete() }
    }

    companion object {
        const val LOG_DIR_NAME = "crash-logs"
        private const val MAX_LOGS = 5

        private val FILENAME_FORMAT = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }

        /** Returns the most recent crash log on disk, or null if none exist. */
        fun latestLog(context: Context): File? {
            val dir = File(context.filesDir, LOG_DIR_NAME)
            if (!dir.isDirectory) return null
            return dir.listFiles { f -> f.isFile && f.name.startsWith("crash-") }
                ?.maxByOrNull { it.lastModified() }
        }
    }
}
