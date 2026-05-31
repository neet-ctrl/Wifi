package com.accu.sdkdemo.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object CrashManager {
    private const val MAX_CRASHES = 100

    private val _crashes = MutableStateFlow<List<CrashEntry>>(emptyList())
    val crashes: StateFlow<List<CrashEntry>> = _crashes.asStateFlow()

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            record("UncaughtException[${thread.name}]", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun record(apiInvolved: String, throwable: Throwable) {
        val entry = CrashEntry(
            apiInvolved   = apiInvolved,
            exceptionType = throwable.javaClass.simpleName,
            message       = throwable.message ?: "(no message)",
            stackTrace    = throwable.stackTraceToString(),
        )
        _crashes.update { current ->
            (current + entry).takeLast(MAX_CRASHES)
        }
        LogManager.error("CrashManager", "${entry.exceptionType} in $apiInvolved: ${entry.message}")
    }

    fun record(apiInvolved: String, exType: String, message: String, stack: String) {
        val entry = CrashEntry(
            apiInvolved   = apiInvolved,
            exceptionType = exType,
            message       = message,
            stackTrace    = stack,
        )
        _crashes.update { current -> (current + entry).takeLast(MAX_CRASHES) }
        LogManager.error("CrashManager", "$exType in $apiInvolved: $message")
    }

    fun clear() { _crashes.value = emptyList() }

    fun exportText(): String = buildString {
        appendLine("=== ACCU SDK Test App — Crash Export ===")
        appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
        appendLine("Total crashes: ${_crashes.value.size}")
        appendLine("=".repeat(50))
        appendLine()
        _crashes.value.forEachIndexed { i, crash ->
            appendLine("--- Crash #${i + 1} ---")
            appendLine("Time:      ${crash.formattedTime}")
            appendLine("API:       ${crash.apiInvolved}")
            appendLine("Exception: ${crash.exceptionType}")
            appendLine("Message:   ${crash.message}")
            appendLine("Stack Trace:")
            appendLine(crash.stackTrace)
            appendLine()
        }
    }
}
