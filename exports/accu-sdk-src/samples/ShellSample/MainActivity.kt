package com.example.accu.shell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accu.sdk.AccuClient
import com.accu.sdk.AccuConnectionState
import com.accu.sdk.AccuConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ACCU SDK — Shell Sample
 *
 * Demonstrates:
 *  - AccuClient connection + permission flow
 *  - exec() for synchronous commands
 *  - execAsync() for streaming long-running commands
 *  - A simple terminal-like UI
 */
class ShellSampleViewModel(application: android.app.Application) :
    androidx.lifecycle.AndroidViewModel(application) {

    private val accu = AccuClient(application)
    val accuState: StateFlow<AccuConnectionState> = accu.state

    private val _outputLines = MutableStateFlow<List<String>>(emptyList())
    val outputLines: StateFlow<List<String>> = _outputLines.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    init { accu.connect() }

    override fun onCleared() { accu.disconnect() }

    fun requestPermission(onResult: (Int) -> Unit) {
        viewModelScope.launch { onResult(accu.requestPermission()) }
    }

    fun runCommand(command: String) {
        _outputLines.update { it + "$ $command" }
        _isRunning.value = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { accu.exec(command) }
            _isRunning.value = false
            if (result.stdout.isNotBlank())
                _outputLines.update { it + result.stdout.lines().filter { l -> l.isNotBlank() } }
            if (result.stderr.isNotBlank())
                _outputLines.update { it + result.stderr.lines().map { l -> "ERR: $l" } }
            _outputLines.update { it + "→ exit ${result.exitCode}" + listOf("") }
        }
    }

    fun runStreaming(command: String) {
        _outputLines.update { it + "$ $command (streaming)" }
        _isRunning.value = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                accu.execAsync(
                    command  = command,
                    onStdout = { line -> _outputLines.update { it + line } },
                    onStderr = { line -> _outputLines.update { it + "ERR: $line" } },
                    onExit   = { code ->
                        _isRunning.value = false
                        _outputLines.update { it + "→ exit $code" + listOf("") }
                    },
                )
            }
        }
    }

    fun clearOutput() { _outputLines.value = emptyList() }
}

class MainActivity : ComponentActivity() {
    private val vm: ShellSampleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { ShellScreen(vm) } }
    }
}

@Composable
fun ShellScreen(vm: ShellSampleViewModel) {
    val state by vm.accuState.collectAsState()
    val lines by vm.outputLines.collectAsState()
    val isRunning by vm.isRunning.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (val s = state) {
            is AccuConnectionState.Connected -> {
                if (!s.isPermissionGranted) {
                    Button(onClick = { vm.requestPermission {} }) { Text("Grant ACCU Permission") }
                }
            }
            is AccuConnectionState.Error -> Text("Error: ${s.reason}", color = MaterialTheme.colorScheme.error)
            else -> LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            items(lines) { line ->
                Text(line, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Enter command...") },
                enabled = !isRunning,
            )
            Button(onClick = { vm.runCommand(input); input = "" }, enabled = input.isNotBlank() && !isRunning) {
                Text("Run")
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.runStreaming("logcat -d -t 20") }, enabled = !isRunning) { Text("Stream logcat") }
            OutlinedButton(onClick = { vm.clearOutput() }) { Text("Clear") }
        }
    }
}
