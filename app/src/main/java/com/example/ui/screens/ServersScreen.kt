package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Server
import com.example.ui.ServerViewModel
import com.example.ui.getStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ServersScreen(
    viewModel: ServerViewModel,
    onServerSelected: (Server) -> Unit,
    modifier: Modifier = Modifier
) {
    val servers by viewModel.allServers.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val strings = getStrings(appLanguage)

    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var serverToEdit by remember { mutableStateOf<Server?>(null) }
    var serverToDelete by remember { mutableStateOf<Server?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "ServerBox",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            strings.appName,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = strings.settings
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_server_fab"),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.addServer)
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (servers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Empty list",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        strings.noServersTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        strings.noServersDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("server_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(servers, key = { it.id }) { server ->
                    ServerCard(
                        server = server,
                        strings = strings,
                        onClick = {
                            viewModel.selectServer(server)
                            onServerSelected(server)
                        },
                        onLongClick = { serverToDelete = server },
                        onEditClick = { serverToEdit = server }
                    )
                }
            }
        }

        // Add/Edit Dialogs
        if (showAddDialog) {
            ServerFormDialog(
                strings = strings,
                onDismiss = { showAddDialog = false },
                onSave = { name, host, port, user, authType, password, pkey, pass ->
                    viewModel.insertServer(
                        Server(
                            name = name,
                            host = host,
                            port = port,
                            username = user,
                            authType = authType,
                            password = password,
                            privateKey = pkey,
                            passphrase = pass
                        )
                    )
                    showAddDialog = false
                }
            )
        }

        if (serverToEdit != null) {
            val s = serverToEdit!!
            ServerFormDialog(
                server = s,
                strings = strings,
                onDismiss = { serverToEdit = null },
                onSave = { name, host, port, user, authType, password, pkey, pass ->
                    viewModel.updateServer(
                        s.copy(
                            name = name,
                            host = host,
                            port = port,
                            username = user,
                            authType = authType,
                            password = password,
                            privateKey = pkey,
                            passphrase = pass
                        )
                    )
                    serverToEdit = null
                }
            )
        }

        if (serverToDelete != null) {
            AlertDialog(
                onDismissRequest = { serverToDelete = null },
                title = { Text(strings.deleteServer) },
                text = { Text("${strings.deleteConfirm}\n\n${serverToDelete?.name} (${serverToDelete?.host})") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            serverToDelete?.let { viewModel.deleteServer(it) }
                            serverToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(strings.deleteServer)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { serverToDelete = null }) {
                        Text(strings.cancel)
                    }
                }
            )
        }

        // Settings Sheet
        if (showSettingsSheet) {
            SettingsBottomSheet(
                viewModel = viewModel,
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerCard(
    server: Server,
    strings: com.example.ui.AppStrings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val dateStr = if (server.lastConnected > 0) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        strings.lastConnected + sdf.format(Date(server.lastConnected))
    } else {
        "Never connected"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("server_card_${server.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual indicator of Server
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (server.authType == "PASSWORD") Icons.Default.VpnKey else Icons.Default.Lock,
                        contentDescription = "Auth type",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    server.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${server.username}@${server.host}:${server.port}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Server")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerFormDialog(
    server: Server? = null,
    strings: com.example.ui.AppStrings,
    onDismiss: () -> Unit,
    onSave: (name: String, host: String, port: Int, user: String, authType: String, pwd: String?, pkey: String?, pass: String?) -> Unit
) {
    var name by remember { mutableStateOf(server?.name ?: "") }
    var host by remember { mutableStateOf(server?.host ?: "") }
    var portStr by remember { mutableStateOf(server?.port?.toString() ?: "22") }
    var username by remember { mutableStateOf(server?.username ?: "root") }
    var authType by remember { mutableStateOf(server?.authType ?: "PASSWORD") }
    var password by remember { mutableStateOf(server?.password ?: "") }
    var privateKey by remember { mutableStateOf(server?.privateKey ?: "") }
    var passphrase by remember { mutableStateOf(server?.passphrase ?: "") }

    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (server == null) strings.addServer else strings.editServer) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(strings.serverName) },
                        modifier = Modifier.fillMaxWidth().testTag("form_name"),
                        singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text(strings.host) },
                            modifier = Modifier.weight(2f).testTag("form_host"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = portStr,
                            onValueChange = { portStr = it },
                            label = { Text(strings.port) },
                            modifier = Modifier.weight(1f).testTag("form_port"),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(strings.username) },
                        modifier = Modifier.fillMaxWidth().testTag("form_user"),
                        singleLine = true
                    )
                }
                item {
                    Text("Authentication Mode", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = authType == "PASSWORD",
                                onClick = { authType = "PASSWORD" }
                            )
                            Text("Password")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = authType == "PRIVATE_KEY",
                                onClick = { authType = "PRIVATE_KEY" }
                            )
                            Text("Private Key")
                        }
                    }
                }
                if (authType == "PASSWORD") {
                    item {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(strings.password) },
                            modifier = Modifier.fillMaxWidth().testTag("form_password"),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(image, contentDescription = "Toggle password visibility")
                                }
                            }
                        )
                    }
                } else {
                    item {
                        OutlinedTextField(
                            value = privateKey,
                            onValueChange = { privateKey = it },
                            label = { Text(strings.privateKey) },
                            placeholder = { Text(strings.privateKeyPlaceholder) },
                            modifier = Modifier.fillMaxWidth().testTag("form_pkey"),
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = passphrase,
                            onValueChange = { passphrase = it },
                            label = { Text("Key Passphrase (Optional)") },
                            modifier = Modifier.fillMaxWidth().testTag("form_passphrase"),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portStr.toIntOrNull() ?: 22
                    if (name.isNotEmpty() && host.isNotEmpty() && username.isNotEmpty()) {
                        onSave(
                            name,
                            host,
                            port,
                            username,
                            authType,
                            if (authType == "PASSWORD") password else null,
                            if (authType == "PRIVATE_KEY") privateKey else null,
                            if (authType == "PRIVATE_KEY" && passphrase.isNotEmpty()) passphrase else null
                        )
                    }
                },
                enabled = name.isNotEmpty() && host.isNotEmpty() && username.isNotEmpty()
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
