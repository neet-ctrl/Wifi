package com.accu.ui.shell

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accu.data.db.entities.SavedScriptEntity
import com.accu.data.db.entities.ShellCommandEntity
import com.accu.data.repositories.ShellRepository
import com.accu.utils.ExecMethod
import com.accu.utils.ShizukuUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

data class ShellUiState(
    val currentInput: String = "",
    val outputLines: List<OutputLine> = listOf(
        OutputLine("Android Control Center — Shell Terminal", OutputType.SYSTEM),
        OutputLine("Type a command and press Enter to execute", OutputType.SYSTEM),
        OutputLine("──────────────────────────────────────────", OutputType.SYSTEM),
    ),
    val commandHistory: List<ShellCommandEntity> = emptyList(),
    val savedScripts: List<SavedScriptEntity> = emptyList(),
    val historyIndex: Int = -1,
    val isExecuting: Boolean = false,
    val execMethod: ExecMethod = ExecMethod.ADB,
    val showHistory: Boolean = false,
    val showScripts: Boolean = false,
    val showSaveDialog: Boolean = false,
    val searchQuery: String = "",
    val aiSuggestions: List<String> = emptyList(),
    val showAiPanel: Boolean = false,
    val currentWorkingDir: String = "/",
    val templateQuery: String = "",
)

data class OutputLine(
    val text: String,
    val type: OutputType = OutputType.OUTPUT,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class OutputType { SYSTEM, COMMAND, OUTPUT, ERROR, SUCCESS }

@HiltViewModel
class ShellViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shellRepository: ShellRepository,
    private val shizukuUtils: ShizukuUtils,
) : ViewModel() {

    private val _state = MutableStateFlow(ShellUiState())
    val state: StateFlow<ShellUiState> = _state.asStateFlow()

    private val sessionHistory = mutableListOf<String>()

    init {
        detectExecMethod()
        observeHistory()
        observeScripts()
    }

    private fun detectExecMethod() {
        viewModelScope.launch(Dispatchers.IO) {
            val method = shizukuUtils.getBestExecMethod()
            _state.update { it.copy(execMethod = method) }
            appendOutput("Exec mode: $method", OutputType.SYSTEM)
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            shellRepository.observeCommands().collect { cmds ->
                _state.update { it.copy(commandHistory = cmds) }
            }
        }
    }

    private fun observeScripts() {
        viewModelScope.launch {
            shellRepository.observeScripts().collect { scripts ->
                _state.update { it.copy(savedScripts = scripts) }
            }
        }
    }

    fun onInputChanged(input: String) { _state.update { it.copy(currentInput = input, historyIndex = -1) } }

    fun execute(command: String = _state.value.currentInput) {
        val cmd = command.trim()
        if (cmd.isEmpty()) return
        sessionHistory.add(0, cmd)
        appendOutput("$ $cmd", OutputType.COMMAND)
        _state.update { it.copy(currentInput = "", isExecuting = true, historyIndex = -1) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Handle built-in commands
                when {
                    cmd == "clear" || cmd == "cls" -> { _state.update { it.copy(outputLines = emptyList()) } }
                    cmd == "help" -> showHelp()
                    cmd.startsWith("cd ") -> changeDirectory(cmd.removePrefix("cd ").trim())
                    cmd == "pwd" -> appendOutput(_state.value.currentWorkingDir, OutputType.OUTPUT)
                    cmd == "history" -> showHistory()
                    cmd == "exit" || cmd == "quit" -> appendOutput("Use back button to exit terminal", OutputType.SYSTEM)
                    else -> executeReal(cmd)
                }
                // Save to history
                shellRepository.saveCommand(
                    ShellCommandEntity(
                        command = cmd,
                        executionCount = 1,
                        lastUsed = System.currentTimeMillis(),
                        category = inferCategory(cmd),
                    )
                )
            } catch (e: Exception) {
                appendOutput("Error: ${e.message}", OutputType.ERROR)
                Timber.e(e)
            } finally {
                _state.update { it.copy(isExecuting = false) }
            }
        }
    }

    private suspend fun executeReal(cmd: String) {
        val fullCmd = if (_state.value.currentWorkingDir != "/") "cd ${_state.value.currentWorkingDir} && $cmd" else cmd
        val result = when (_state.value.execMethod) {
            ExecMethod.SHIZUKU -> shizukuUtils.execShizuku(fullCmd)
            ExecMethod.ROOT    -> shizukuUtils.execRoot(fullCmd)
            ExecMethod.ADB     -> shizukuUtils.execAdb(fullCmd)
        }
        if (result.output.isNotBlank()) appendOutput(result.output.trimEnd(), OutputType.OUTPUT)
        if (result.error.isNotBlank()) appendOutput(result.error.trimEnd(), OutputType.ERROR)
        if (result.output.isBlank() && result.error.isBlank()) {
            appendOutput(if (result.isSuccess) "Command completed (exit 0)" else "Exit code: ${result.exitCode}", if (result.isSuccess) OutputType.SUCCESS else OutputType.ERROR)
        }
    }

    private fun changeDirectory(path: String) {
        val newDir = when {
            path.startsWith("/") -> path
            path == ".." -> _state.value.currentWorkingDir.substringBeforeLast("/").ifEmpty { "/" }
            else -> "${_state.value.currentWorkingDir.trimEnd('/')}/$path"
        }
        _state.update { it.copy(currentWorkingDir = newDir) }
        appendOutput("Changed to $newDir", OutputType.SYSTEM)
    }

    private fun showHelp() {
        val help = """
            ┌─ ACC Shell Terminal ─────────────────────┐
            │  clear / cls    — Clear terminal           │
            │  help           — Show this help           │
            │  history        — Show command history     │
            │  cd <path>      — Change directory         │
            │  pwd            — Print working directory  │
            │  exit           — Exit terminal            │
            │                                            │
            │  All other commands execute via:           │
            │  ${_state.value.execMethod.name.padEnd(43)}│
            └────────────────────────────────────────────┘
            Common ADB commands:
              pm list packages -3        — User apps
              pm list packages -s        — System apps
              pm disable-user <pkg>      — Disable app
              pm enable <pkg>            — Enable app
              pm uninstall --user 0 <pkg>— Remove app
              settings get global <key>  — Get setting
              settings put global <key> <val>
              dumpsys battery            — Battery info
              wm size                    — Screen size
              wm density                 — Screen density
        """.trimIndent()
        appendOutput(help, OutputType.SYSTEM)
    }

    private fun showHistory() {
        sessionHistory.take(50).forEachIndexed { i, cmd ->
            appendOutput("  ${"${i+1}".padStart(3)}  $cmd", OutputType.OUTPUT)
        }
    }

    fun navigateHistoryUp() {
        val history = _state.value.commandHistory
        if (history.isEmpty()) return
        val newIdx = (_state.value.historyIndex + 1).coerceAtMost(history.size - 1)
        _state.update { it.copy(historyIndex = newIdx, currentInput = history[newIdx].command) }
    }

    fun navigateHistoryDown() {
        val newIdx = _state.value.historyIndex - 1
        if (newIdx < 0) {
            _state.update { it.copy(historyIndex = -1, currentInput = "") }
        } else {
            _state.update { it.copy(historyIndex = newIdx, currentInput = _state.value.commandHistory[newIdx].command) }
        }
    }

    fun runScript(script: SavedScriptEntity) {
        viewModelScope.launch {
            appendOutput("▶ Running script: ${script.name}", OutputType.SYSTEM)
            script.content.lines().filter { it.isNotBlank() && !it.startsWith("#") }.forEach { line ->
                execute(line)
                delay(100)
            }
            shellRepository.incrementScriptRunCount(script.id)
        }
    }

    fun saveCurrentCommand(name: String, description: String) {
        val cmd = _state.value.currentInput.ifBlank {
            _state.value.commandHistory.firstOrNull()?.command ?: return
        }
        viewModelScope.launch {
            shellRepository.saveScript(SavedScriptEntity(name = name, content = cmd, description = description))
            _state.update { it.copy(showSaveDialog = false) }
            appendOutput("Saved: $name", OutputType.SUCCESS)
        }
    }

    fun clearOutput() { _state.update { it.copy(outputLines = emptyList()) } }
    fun toggleHistory() { _state.update { it.copy(showHistory = !it.showHistory, showScripts = false) } }
    fun toggleScripts() { _state.update { it.copy(showScripts = !it.showScripts, showHistory = false) } }
    fun toggleSaveDialog() { _state.update { it.copy(showSaveDialog = !it.showSaveDialog) } }
    fun setExecMethod(method: ExecMethod) {
        _state.update { it.copy(execMethod = method) }
        appendOutput("Switched to exec mode: $method", OutputType.SYSTEM)
    }

    fun exportLogs(): String = _state.value.outputLines.joinToString("\n") {
        "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(it.timestamp))}] ${it.text}"
    }

    private fun appendOutput(text: String, type: OutputType) {
        val lines = text.lines().map { OutputLine(it, type) }
        _state.update { it.copy(outputLines = it.outputLines + lines) }
    }

    private fun inferCategory(cmd: String): String = when {
        cmd.startsWith("pm ") -> "Package Manager"
        cmd.startsWith("am ") -> "Activity Manager"
        cmd.startsWith("dumpsys") -> "Diagnostics"
        cmd.startsWith("settings") -> "Settings"
        cmd.startsWith("adb ") -> "ADB"
        cmd.startsWith("wm ") -> "Window Manager"
        cmd.startsWith("ls") || cmd.startsWith("cat") || cmd.startsWith("cp") || cmd.startsWith("mv") -> "File System"
        else -> "General"
    }
}
