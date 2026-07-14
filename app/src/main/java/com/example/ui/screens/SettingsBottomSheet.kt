package com.example.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppLanguage
import com.example.ui.AppTheme
import com.example.ui.ServerViewModel
import com.example.ui.getStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    viewModel: ServerViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val shortcuts by viewModel.allShortcuts.collectAsState()
    val strings = getStrings(appLanguage)

    val context = LocalContext.current

    // Setup Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(
                uri = uri,
                onSuccess = {
                    android.widget.Toast.makeText(context, strings.backupSuccess, android.widget.Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    android.widget.Toast.makeText(context, "${strings.backupFailed}: $err", android.widget.Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // Setup Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(
                uri = uri,
                onSuccess = { msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                },
                onError = { err ->
                    android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    var newShortcutName by remember { mutableStateOf("") }
    var newShortcutCommand by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = strings.settings,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = strings.settings,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Language preference
                item {
                    Column {
                        Text(
                            text = strings.language,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val languages = listOf(
                                AppLanguage.SYSTEM to strings.languageSystem,
                                AppLanguage.ENGLISH to strings.languageEnglish,
                                AppLanguage.CHINESE to strings.languageChinese
                            )
                            languages.forEach { (lang, label) ->
                                FilterChip(
                                    selected = appLanguage == lang,
                                    onClick = { viewModel.setLanguage(lang) },
                                    label = { Text(label, fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 2. Theme Mode preference (Day / Night)
                item {
                    Column {
                        Text(
                            text = strings.theme,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themes = listOf(
                                AppTheme.SYSTEM to strings.themeSystem,
                                AppTheme.LIGHT to strings.themeLight,
                                AppTheme.DARK to strings.themeDark
                            )
                            themes.forEach { (theme, label) ->
                                FilterChip(
                                    selected = appTheme == theme,
                                    onClick = { viewModel.setTheme(theme) },
                                    label = { Text(label, fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 2.5. Backup & Restore
                item {
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.backupRestore,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.backupDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    exportLauncher.launch("flux_backup_${System.currentTimeMillis() / 1000}.json")
                                },
                                modifier = Modifier.weight(1f).testTag("export_backup_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.backupBtn, fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    importLauncher.launch("application/json")
                                },
                                modifier = Modifier.weight(1f).testTag("import_backup_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.restoreBtn, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // 3. Shortcut Commands Manager Title
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = strings.shortcutManager,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Form to add a new shortcut command
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = strings.addShortcut,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newShortcutName,
                                onValueChange = {
                                    newShortcutName = it
                                    errorMessage = null
                                },
                                label = { Text(strings.shortcutName) },
                                modifier = Modifier.fillMaxWidth().testTag("shortcut_name_input"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newShortcutCommand,
                                onValueChange = {
                                    newShortcutCommand = it
                                    errorMessage = null
                                },
                                label = { Text(strings.shortcutCommand) },
                                modifier = Modifier.fillMaxWidth().testTag("shortcut_cmd_input"),
                                singleLine = true
                            )

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (newShortcutName.isBlank() || newShortcutCommand.isBlank()) {
                                        errorMessage = "Fields cannot be blank!"
                                    } else {
                                        // Ensure standard newline command trailing for convenience
                                        val cleanCmd = if (newShortcutCommand.endsWith("\n")) {
                                            newShortcutCommand
                                        } else {
                                            newShortcutCommand + "\n"
                                        }
                                        viewModel.addShortcut(newShortcutName.trim(), cleanCmd)
                                        newShortcutName = ""
                                        newShortcutCommand = ""
                                        errorMessage = null
                                    }
                                },
                                modifier = Modifier.align(Alignment.End).testTag("add_shortcut_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.create)
                            }
                        }
                    }
                }

                // List of shortcut commands
                if (shortcuts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.noShortcuts,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    items(shortcuts) { shortcut ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = shortcut.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = shortcut.command.replace("\n", " ↵"),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteShortcut(shortcut) },
                                    modifier = Modifier.testTag("delete_shortcut_${shortcut.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = strings.deleteShortcut,
                                        tint = MaterialTheme.colorScheme.error
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
