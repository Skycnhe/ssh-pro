package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.ServerViewModel

import com.example.ui.getStrings

enum class ServerTab {
    DASHBOARD, TERMINAL, SFTP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshApp(
    viewModel: ServerViewModel,
    modifier: Modifier = Modifier
) {
    val selectedServer by viewModel.selectedServer.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val strings = getStrings(appLanguage)
    var activeTab by remember { mutableStateOf(ServerTab.DASHBOARD) }

    if (selectedServer == null) {
        // Show main list of SSH Servers
        ServersScreen(
            viewModel = viewModel,
            onServerSelected = {
                activeTab = ServerTab.DASHBOARD
            },
            modifier = modifier
        )
    } else {
        // Hand-off back button to deselect server and return to main list
        BackHandler {
            viewModel.selectServer(null) // This deselects and returns
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = activeTab == ServerTab.DASHBOARD,
                        onClick = { activeTab = ServerTab.DASHBOARD },
                        icon = { Icon(Icons.Default.PieChart, contentDescription = strings.dashboard) },
                        label = { Text(strings.dashboard) }
                    )
                    NavigationBarItem(
                        selected = activeTab == ServerTab.TERMINAL,
                        onClick = { activeTab = ServerTab.TERMINAL },
                        icon = { Icon(Icons.Default.Terminal, contentDescription = strings.terminal) },
                        label = { Text(strings.terminal) }
                    )
                    NavigationBarItem(
                        selected = activeTab == ServerTab.SFTP,
                        onClick = { activeTab = ServerTab.SFTP },
                        icon = { Icon(Icons.Default.Folder, contentDescription = strings.sftp) },
                        label = { Text(strings.sftp) }
                    )
                }
            },
            modifier = modifier
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Display the appropriate screen inside the workspace
                when (activeTab) {
                    ServerTab.DASHBOARD -> {
                        MonitorScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ServerTab.TERMINAL -> {
                        TerminalScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ServerTab.SFTP -> {
                        SftpScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
