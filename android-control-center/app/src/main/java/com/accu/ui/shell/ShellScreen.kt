package com.accu.ui.shell

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accu.ui.components.ACCTopBar
import com.accu.utils.ExecMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellScreen(viewModel: ShellViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var saveName by remember { mutableStateOf("") }
    var saveDesc by remember { mutableStateOf("") }

    // Auto-scroll to bottom when output changes
    LaunchedEffect(state.outputLines.size) {
        if (state.outputLines.isNotEmpty()) {
            listState.animateScrollToItem(state.outputLines.size - 1)
        }
    }

    Scaffold(
        topBar = {
            ACCTopBar(
                title = "Shell Terminal",
                actions = {
                    // Exec method selector
                    var showMethodMenu by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { showMethodMenu = true }) {
                            Text(state.execMethod.name, style = MaterialTheme.typography.labelMedium)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = showMethodMenu, onDismissRequest = { showMethodMenu = false }) {
                            ExecMethod.entries.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.name) },
                                    onClick = { viewModel.setExecMethod(method); showMethodMenu = false },
                                    leadingIcon = { if (state.execMethod == method) Icon(Icons.Default.Check, null) },
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.toggleHistory() }) { Icon(Icons.Default.History, "History") }
                    IconButton(onClick = { viewModel.toggleScripts() }) { Icon(Icons.Default.Code, "Scripts") }
                    IconButton(onClick = { viewModel.clearOutput() }) { Icon(Icons.Default.ClearAll, "Clear") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // ── Output area ──
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                items(state.outputLines) { line ->
                    val (color, prefix) = when (line.type) {
                        OutputType.COMMAND -> Color(0xFF58A6FF) to ""
                        OutputType.ERROR   -> Color(0xFFFF7B72) to ""
                        OutputType.SUCCESS -> Color(0xFF3FB950) to ""
                        OutputType.SYSTEM  -> Color(0xFF8B949E) to ""
                        OutputType.OUTPUT  -> Color(0xFFE6EDF3) to ""
                    }
                    Text(
                        text = "$prefix${line.text}",
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
                if (state.isExecuting) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF58A6FF),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Executing…", color = Color(0xFF8B949E), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── Quick command chips ──
            val quickCmds = listOf("ls -la", "pwd", "pm list packages -3", "dumpsys battery", "uname -a", "id", "whoami", "df -h")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
            ) {
                items(quickCmds) { cmd ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.execute(cmd) },
                        label = { Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    )
                }
            }

            // ── Input row ──
            Surface(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                tonalElevation = 4.dp,
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("$ ", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextField(
                        value = state.currentInput,
                        onValueChange = viewModel::onInputChanged,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onKeyEvent { event ->
                                when {
                                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> { viewModel.navigateHistoryUp(); true }
                                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> { viewModel.navigateHistoryDown(); true }
                                    event.type == KeyEventType.KeyDown && event.key == Key.Tab -> { true }
                                    else -> false
                                }
                            },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.execute() }),
                        placeholder = { Text("Enter command…", fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                        trailingIcon = {
                            if (state.currentInput.isNotBlank()) {
                                IconButton(onClick = { viewModel.toggleSaveDialog() }) { Icon(Icons.Default.BookmarkAdd, "Save", modifier = Modifier.size(18.dp)) }
                            }
                        },
                    )
                    IconButton(
                        onClick = { viewModel.execute() },
                        enabled = state.currentInput.isNotBlank() && !state.isExecuting,
                    ) {
                        Icon(
                            if (state.isExecuting) Icons.Default.HourglassTop else Icons.Default.Send,
                            "Run",
                            tint = if (state.currentInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    // ── History drawer ──
    if (state.showHistory) {
        ModalBottomSheet(onDismissRequest = viewModel::toggleHistory) {
            Text("Command History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
            if (state.commandHistory.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No history yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(state.commandHistory) { cmd ->
                        ListItem(
                            headlineContent = { Text(cmd.command, fontFamily = FontFamily.Monospace) },
                            supportingContent = { if (cmd.description.isNotBlank()) Text(cmd.description) },
                            trailingContent = { Text("×${cmd.executionCount}", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.clickable {
                                viewModel.onInputChanged(cmd.command)
                                viewModel.toggleHistory()
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Scripts drawer ──
    if (state.showScripts) {
        ModalBottomSheet(onDismissRequest = viewModel::toggleScripts) {
            Text("Saved Scripts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                items(state.savedScripts) { script ->
                    ListItem(
                        headlineContent = { Text(script.name) },
                        supportingContent = { if (script.description.isNotBlank()) Text(script.description) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.runScript(script) }) { Icon(Icons.Default.PlayArrow, "Run") }
                        },
                        modifier = Modifier.clickable { viewModel.onInputChanged(script.content.lines().first()); viewModel.toggleScripts() },
                    )
                    HorizontalDivider()
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Save dialog ──
    if (state.showSaveDialog) {
        AlertDialog(
            onDismissRequest = viewModel::toggleSaveDialog,
            title = { Text("Save Command") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = saveName, onValueChange = { saveName = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = saveDesc, onValueChange = { saveDesc = it }, label = { Text("Description (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.saveCurrentCommand(saveName, saveDesc); saveName = ""; saveDesc = "" }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = viewModel::toggleSaveDialog) { Text("Cancel") } },
        )
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
