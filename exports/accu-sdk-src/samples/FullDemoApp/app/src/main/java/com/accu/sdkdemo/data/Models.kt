package com.accu.sdkdemo.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class LogLevel { DEBUG, INFO, SUCCESS, WARNING, ERROR }

enum class TestStatus { PENDING, RUNNING, PASS, FAIL, WARNING }

data class LogEntry(
    val id: Long = idGen.getAndIncrement(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
    val formattedDate: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    companion object { private val idGen = AtomicLong(0) }
}

data class CrashEntry(
    val id: Long = idGen.getAndIncrement(),
    val timestamp: Long = System.currentTimeMillis(),
    val apiInvolved: String,
    val exceptionType: String,
    val message: String,
    val stackTrace: String,
) {
    val formattedTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    companion object { private val idGen = AtomicLong(0) }
}

data class AppInfo(
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
)

data class TestResult(
    val id: Int,
    val name: String,
    val description: String,
    val status: TestStatus = TestStatus.PENDING,
    val detail: String = "",
)

data class DiagnosticItem(
    val id: Int,
    val name: String,
    val description: String,
    val status: DiagnosticStatus = DiagnosticStatus.UNKNOWN,
    val detail: String = "",
)

enum class DiagnosticStatus { UNKNOWN, CHECKING, PASS, FAIL, WARNING }

data class SettingEntry(
    val category: String,
    val key: String,
    val description: String,
    val safeWriteValue: String? = null,
    val readOnly: Boolean = false,
)

data class ApiMethod(
    val name: String,
    val signature: String,
    val description: String,
    val scope: String,
    val transactionId: Int,
    val category: String,
)
