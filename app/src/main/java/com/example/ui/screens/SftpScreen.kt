package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ServerViewModel
import com.example.ui.getStrings
import com.example.ssh.SftpFile
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SftpScreen(
    viewModel: ServerViewModel,
    modifier: Modifier = Modifier
) {
    val server by viewModel.selectedServer.collectAsState()
    val currentPath by viewModel.currentSftpPath.collectAsState()
    val files by viewModel.sftpFiles.collectAsState()
    val isLoading by viewModel.isSftpLoading.collectAsState()
    val error by viewModel.sftpError.collectAsState()
    
    val appLanguage by viewModel.appLanguage.collectAsState()
    val strings = getStrings(appLanguage)

    var showCreateDirDialog by remember { mutableStateOf(false) }
    var newDirName by remember { mutableStateOf("") }

    var showCreateFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFileContent by remember { mutableStateOf("") }

    var selectedFileToView by remember { mutableStateOf<SftpFile?>(null) }
    var fileViewContent by remember { mutableStateOf<String?>(null) }
    var isFileLoading by remember { mutableStateOf(false) }
    var isEditingFile by remember { mutableStateOf(false) }
    var editingFileText by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    // Load initial directory listing
    LaunchedEffect(Unit) {
        viewModel.loadSftpDirectory(currentPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(strings.sftp, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(server?.name ?: "Remote Files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDirDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = strings.createFolder)
                    }
                    IconButton(onClick = { 
                        newFileName = ""
                        newFileContent = ""
                        showCreateFileDialog = true 
                    }) {
                        Icon(Icons.Default.NoteAdd, contentDescription = strings.createFile)
                    }
                    IconButton(onClick = { viewModel.loadSftpDirectory(currentPath) }) {
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
        ) {
            // Path Navigation bar / Breadcrumbs
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Directory",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentPath,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (currentPath != "/" && currentPath.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val parent = currentPath.substringBeforeLast("/")
                                val cleanParent = if (parent.isEmpty()) "/" else parent
                                viewModel.loadSftpDirectory(cleanParent)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Up a dir")
                        }
                    }
                }
            }

            // File Listing Panel
            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(strings.connectionFailed, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error ?: "Unknown error", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadSftpDirectory(currentPath) }) {
                            Text(strings.retry)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("sftp_file_list"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (files.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(strings.noFiles, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    items(files) { file ->
                        SftpFileItemRow(
                            file = file,
                            strings = strings,
                            onClick = {
                                if (file.isDirectory) {
                                    viewModel.loadSftpDirectory(file.fullPath)
                                } else {
                                    // Open file reader sheet for standard texts
                                    val ext = file.name.substringAfterLast(".", "").lowercase()
                                    val textExtensions = listOf("txt", "conf", "json", "yml", "yaml", "sh", "log", "py", "html", "css", "js", "xml", "env", "md")
                                    if (textExtensions.contains(ext) || ext.isEmpty()) {
                                        selectedFileToView = file
                                        isFileLoading = true
                                        fileViewContent = null
                                        isEditingFile = false
                                        editingFileText = ""
                                        scope.launch {
                                            try {
                                                val content = com.example.ssh.SshManager.readFileContent(server!!, file.fullPath)
                                                fileViewContent = content
                                                editingFileText = content
                                            } catch (e: Exception) {
                                                fileViewContent = "Error reading file content:\n${e.localizedMessage ?: e.toString()}"
                                                editingFileText = fileViewContent ?: ""
                                            } finally {
                                                isFileLoading = false
                                            }
                                        }
                                    }
                                }
                            },
                            onDelete = {
                                viewModel.deleteSftpFile(file.fullPath, file.isDirectory)
                            }
                        )
                    }
                }
            }
        }

        // Create Directory Dialog
        if (showCreateDirDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDirDialog = false },
                title = { Text(strings.createFolder) },
                text = {
                    OutlinedTextField(
                        value = newDirName,
                        onValueChange = { newDirName = it },
                        label = { Text(strings.folderName) },
                        modifier = Modifier.fillMaxWidth().testTag("form_folder_name"),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newDirName.isNotEmpty()) {
                                viewModel.createSftpDirectory(newDirName)
                                newDirName = ""
                                showCreateDirDialog = false
                            }
                        },
                        enabled = newDirName.isNotEmpty()
                    ) {
                        Text(strings.create)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDirDialog = false }) {
                        Text(strings.cancel)
                    }
                }
            )
        }

        // Create File Dialog
        if (showCreateFileDialog) {
            AlertDialog(
                onDismissRequest = { showCreateFileDialog = false },
                title = { Text(strings.createFile) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newFileName,
                            onValueChange = { newFileName = it },
                            label = { Text(strings.fileName) },
                            modifier = Modifier.fillMaxWidth().testTag("form_file_name"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newFileContent,
                            onValueChange = { newFileContent = it },
                            label = { Text(strings.fileContent) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFileName.isNotEmpty()) {
                                viewModel.uploadSftpFile(newFileName, newFileContent)
                                newFileName = ""
                                newFileContent = ""
                                showCreateFileDialog = false
                            }
                        },
                        enabled = newFileName.isNotEmpty()
                    ) {
                        Text(strings.create)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFileDialog = false }) {
                        Text(strings.cancel)
                    }
                }
            )
        }

        // File Viewer Bottom Sheet
        if (selectedFileToView != null) {
            val file = selectedFileToView!!
            ModalBottomSheet(
                onDismissRequest = { selectedFileToView = null },
                modifier = Modifier.fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Article, contentDescription = "File", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(formatBytes(file.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        
                        if (!isFileLoading && fileViewContent != null && !fileViewContent!!.startsWith("Error reading file content:")) {
                            if (isEditingFile) {
                                IconButton(onClick = {
                                    viewModel.uploadSftpFile(file.name, editingFileText)
                                    isEditingFile = false
                                    selectedFileToView = null // Close editor after save
                                }) {
                                    Icon(Icons.Default.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                IconButton(onClick = { isEditingFile = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                            }
                        }
                        
                        IconButton(onClick = { selectedFileToView = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    if (isFileLoading) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (isEditingFile) {
                                OutlinedTextField(
                                    value = editingFileText,
                                    onValueChange = { editingFileText = it },
                                    modifier = Modifier.fillMaxSize(),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                )
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item {
                                        Text(
                                            text = fileViewContent ?: "No content",
                                            style = androidx.compose.ui.text.TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp
                                            ),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SftpFileItemRow(
    file: SftpFile,
    strings: com.example.ui.AppStrings,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = if (file.isDirectory) "Folder" else "File",
                tint = if (file.isDirectory) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (file.isDirectory) strings.directory else formatBytes(file.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        file.permissions,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "File Actions")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(strings.delete) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups >= units.size) return "$bytes B"
    return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
