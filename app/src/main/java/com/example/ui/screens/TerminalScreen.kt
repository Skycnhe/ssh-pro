package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ServerViewModel
import com.example.ui.getStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: ServerViewModel,
    modifier: Modifier = Modifier
) {
    val server by viewModel.selectedServer.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val isConnecting by viewModel.isTerminalConnecting.collectAsState()
    val error by viewModel.terminalError.collectAsState()
    
    val appLanguage by viewModel.appLanguage.collectAsState()
    val strings = getStrings(appLanguage)

    var inputCommand by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Trigger SSH Shell connection
    DisposableEffect(Unit) {
        viewModel.connectTerminal()
        onDispose {
            viewModel.disconnectTerminal()
        }
    }

    // Auto-scroll to bottom of terminal output
    LaunchedEffect(terminalOutput) {
        if (terminalOutput.isNotEmpty()) {
            listState.animateScrollToItem(index = 0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(strings.terminal, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(server?.name ?: "Remote Shell", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.connectTerminal() }) {
                        Icon(Icons.Default.Refresh, contentDescription = strings.reconnect)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF121212)) // Dark retro background for terminal
        ) {
            // Main Terminal Console Output Panel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                if (isConnecting) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(strings.connectingShell, color = Color.White)
                        }
                    }
                } else if (error != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(strings.connectionFailed, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(error ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.connectTerminal() }) {
                                Text(strings.retry)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("terminal_logs"),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            Text(
                                text = terminalOutput,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = Color(0xFF00FF00) // Terminal Green
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }

            // 1. Dynamic Scrollable Custom Command Shortcuts Row
            val customShortcuts by viewModel.allShortcuts.collectAsState()
            if (customShortcuts.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1A))
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(customShortcuts) { shortcut ->
                        Button(
                            onClick = { viewModel.sendTerminalInput(shortcut.command) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(shortcut.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Quick Standard Controls Helper Row (Ctrl+C, Tab, Esc etc.)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val shortcutKeys = listOf(
                    "Tab" to "\t",
                    "Ctrl+C" to "\u0003",
                    "Esc" to "\u001b",
                    "Clear" to "clear\n",
                    "|" to "|",
                    "/" to "/"
                )
                shortcutKeys.forEach { (label, command) ->
                    Button(
                        onClick = { 
                            viewModel.sendTerminalInput(command)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF333333),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Interactive Command Prompt Input Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$ ",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00FF00),
                        fontSize = 16.sp
                    ),
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                )

                OutlinedTextField(
                    value = inputCommand,
                    onValueChange = { inputCommand = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = Color.White
                    ),
                    placeholder = { Text(strings.terminalPlaceholder, color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 14.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color(0xFF2B2B2B),
                        unfocusedContainerColor = Color(0xFF2B2B2B),
                        cursorColor = Color(0xFF00FF00)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 56.dp)
                        .testTag("terminal_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputCommand.isNotEmpty()) {
                                viewModel.sendTerminalInput(inputCommand + "\n")
                                inputCommand = ""
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputCommand.isNotEmpty()) {
                            viewModel.sendTerminalInput(inputCommand + "\n")
                            inputCommand = ""
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = strings.send, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
