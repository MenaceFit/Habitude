package com.menacefit.habitude.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/**
 * Installed once from [com.menacefit.habitude.HabitudeApplication]. The app
 * ships no crash-reporting SDK (it is 100% offline, spec section 2), so an
 * unhandled exception has nowhere remote to go and no UI surface left to
 * show a friendly message on — the process is about to die either way.
 *
 * What this *can* still do honestly: write the stack trace to a local file
 * instead of letting it vanish with logcat, so a genuine bug is diagnosable
 * from a device, then hand off to the platform's default handler so the
 * normal crash/restart cycle still happens. Swallowing the exception instead
 * would leave Compose/ViewModel state silently corrupted — worse than the
 * system's own recovery.
 */
class CrashHandler(
    private val appContext: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            writeCrashLog(throwable)
        } catch (loggingFailure: Exception) {
            Log.e(TAG, "Failed to persist crash log", loggingFailure)
        }
        Log.e(TAG, "Uncaught exception on thread ${thread.name}", throwable)
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun writeCrashLog(throwable: Throwable) {
        val dir = File(appContext.filesDir, "crash_logs").apply { mkdirs() }
        val existing = dir.listFiles()?.sortedBy { it.lastModified() }.orEmpty()
        if (existing.size >= MAX_KEPT_LOGS) {
            existing.take(existing.size - MAX_KEPT_LOGS + 1).forEach { it.delete() }
        }
        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        File(dir, "crash_${Instant.now().epochSecond}.txt").writeText(stackTrace)
    }

    companion object {
        private const val TAG = "Habitude"
        private const val MAX_KEPT_LOGS = 5

        fun install(appContext: Context) {
            val existingDefault = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(appContext, existingDefault))
        }
    }
}
