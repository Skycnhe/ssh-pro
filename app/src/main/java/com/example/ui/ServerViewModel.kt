package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Server
import com.example.data.repository.ServerRepository
import com.example.ssh.SshManager
import com.example.ssh.ServerMonitor
import com.example.ssh.ServerPerformanceMetrics
import com.example.ssh.ShellConnection
import com.example.ssh.SftpFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader

class ServerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ServerRepository
    val allServers: StateFlow<List<Server>>
    val allShortcuts: StateFlow<List<com.example.data.model.ShortcutCommand>>

    private val prefs = application.getSharedPreferences("serverbox_prefs", android.content.Context.MODE_PRIVATE)

    private val _appLanguage = MutableStateFlow(
        AppLanguage.valueOf(prefs.getString("app_language", AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name)
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _appTheme = MutableStateFlow(
        AppTheme.valueOf(prefs.getString("app_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name)
    )
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        val serverDao = db.serverDao()
        val shortcutDao = db.shortcutDao()
        repository = ServerRepository(serverDao, shortcutDao)
        
        val serversFlow = repository.allServers
        val tempFlow = MutableStateFlow<List<Server>>(emptyList())
        allServers = tempFlow
        
        viewModelScope.launch {
            serversFlow.collect {
                tempFlow.value = it
            }
        }

        val shortcutsFlow = repository.allShortcuts
        val tempShortcutsFlow = MutableStateFlow<List<com.example.data.model.ShortcutCommand>>(emptyList())
        allShortcuts = tempShortcutsFlow
        
        viewModelScope.launch {
            shortcutsFlow.collect { list ->
                if (list.isEmpty()) {
                    // Seed standard shortcuts on first start
                    repository.insertShortcut(com.example.data.model.ShortcutCommand(name = "ls -la", command = "ls -la\n"))
                    repository.insertShortcut(com.example.data.model.ShortcutCommand(name = "df -h", command = "df -h\n"))
                    repository.insertShortcut(com.example.data.model.ShortcutCommand(name = "free -h", command = "free -h\n"))
                    repository.insertShortcut(com.example.data.model.ShortcutCommand(name = "top", command = "top -b -n 1 | head -n 12\n"))
                } else {
                    tempShortcutsFlow.value = list
                }
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
        prefs.edit().putString("app_language", language.name).apply()
    }

    fun setTheme(theme: AppTheme) {
        _appTheme.value = theme
        prefs.edit().putString("app_theme", theme.name).apply()
    }

    fun addShortcut(name: String, command: String) {
        viewModelScope.launch {
            repository.insertShortcut(com.example.data.model.ShortcutCommand(name = name, command = command))
        }
    }

    fun deleteShortcut(shortcut: com.example.data.model.ShortcutCommand) {
        viewModelScope.launch {
            repository.deleteShortcut(shortcut)
        }
    }

    // Selected server for detailed view
    private val _selectedServer = MutableStateFlow<Server?>(null)
    val selectedServer: StateFlow<Server?> = _selectedServer.asStateFlow()

    // 1. Dashboard Monitor State
    private val _currentMetrics = MutableStateFlow<ServerPerformanceMetrics?>(null)
    val currentMetrics: StateFlow<ServerPerformanceMetrics?> = _currentMetrics.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _monitorError = MutableStateFlow<String?>(null)
    val monitorError: StateFlow<String?> = _monitorError.asStateFlow()

    private var monitorJob: Job? = null

    // 2. SFTP File Explorer State
    private val _currentSftpPath = MutableStateFlow("/")
    val currentSftpPath: StateFlow<String> = _currentSftpPath.asStateFlow()

    private val _sftpFiles = MutableStateFlow<List<SftpFile>>(emptyList())
    val sftpFiles: StateFlow<List<SftpFile>> = _sftpFiles.asStateFlow()

    private val _isSftpLoading = MutableStateFlow(false)
    val isSftpLoading: StateFlow<Boolean> = _isSftpLoading.asStateFlow()

    private val _sftpError = MutableStateFlow<String?>(null)
    val sftpError: StateFlow<String?> = _sftpError.asStateFlow()

    // 3. Interactive SSH Terminal State
    private var shellConnection: ShellConnection? = null
    private val _terminalOutput = MutableStateFlow<String>("")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()

    private val _isTerminalConnecting = MutableStateFlow(false)
    val isTerminalConnecting: StateFlow<Boolean> = _isTerminalConnecting.asStateFlow()

    private val _terminalError = MutableStateFlow<String?>(null)
    val terminalError: StateFlow<String?> = _terminalError.asStateFlow()

    private var terminalReaderJob: Job? = null

    // Database Actions
    fun insertServer(server: Server) {
        viewModelScope.launch {
            repository.insertServer(server)
        }
    }

    fun updateServer(server: Server) {
        viewModelScope.launch {
            repository.updateServer(server)
        }
    }

    fun deleteServer(server: Server) {
        viewModelScope.launch {
            repository.deleteServer(server)
            if (_selectedServer.value?.id == server.id) {
                _selectedServer.value = null
            }
        }
    }

    fun selectServer(server: Server?) {
        _selectedServer.value = server
        // Reset states
        _currentMetrics.value = null
        _monitorError.value = null
        _currentSftpPath.value = "/"
        _sftpFiles.value = emptyList()
        _sftpError.value = null
        _terminalOutput.value = ""
        _terminalError.value = null
        
        if (server != null) {
            // Update last connected timestamp in DB
            viewModelScope.launch {
                repository.updateServer(server.copy(lastConnected = System.currentTimeMillis()))
            }
        }
    }

    // --- MONITOR ACTIONS ---
    fun startMonitoring() {
        val server = _selectedServer.value ?: return
        if (_isMonitoring.value) return

        _isMonitoring.value = true
        _monitorError.value = null

        monitorJob = viewModelScope.launch {
            var lastMetrics: ServerPerformanceMetrics? = null
            while (_isMonitoring.value) {
                try {
                    val metrics = ServerMonitor.fetchMetrics(server, lastMetrics)
                    _currentMetrics.value = metrics
                    lastMetrics = metrics
                    _monitorError.value = null
                } catch (e: Exception) {
                    _monitorError.value = e.localizedMessage ?: e.toString()
                    _currentMetrics.value = null
                }
                delay(3000) // Poll every 3 seconds
            }
        }
    }

    fun stopMonitoring() {
        _isMonitoring.value = false
        monitorJob?.cancel()
        monitorJob = null
    }

    fun killProcess(pid: String, commandName: String, onResult: (Boolean, String) -> Unit) {
        val server = _selectedServer.value ?: return
        viewModelScope.launch {
            try {
                val output = SshManager.executeCommand(server, "kill -9 $pid")
                // Check if there was an error output
                if (output.lowercase().contains("error") || output.lowercase().contains("not permitted") || output.lowercase().contains("no such process")) {
                    onResult(false, output)
                } else {
                    onResult(true, "Process $commandName ($pid) terminated.")
                    // Refresh metrics soon
                    delay(800)
                    try {
                        val metrics = ServerMonitor.fetchMetrics(server, _currentMetrics.value)
                        _currentMetrics.value = metrics
                    } catch (e: Exception) {
                        // ignore refresh failures
                    }
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: e.toString())
            }
        }
    }

    // --- SFTP ACTIONS ---
    fun loadSftpDirectory(path: String) {
        val server = _selectedServer.value ?: return
        _isSftpLoading.value = true
        _sftpError.value = null
        _currentSftpPath.value = path

        viewModelScope.launch {
            try {
                val files = SshManager.listFiles(server, path)
                _sftpFiles.value = files
                _sftpError.value = null
            } catch (e: Exception) {
                _sftpError.value = e.localizedMessage ?: e.toString()
                _sftpFiles.value = emptyList()
            } finally {
                _isSftpLoading.value = false
            }
        }
    }

    fun createSftpDirectory(dirName: String) {
        val server = _selectedServer.value ?: return
        viewModelScope.launch {
            try {
                SshManager.createDirectory(server, _currentSftpPath.value, dirName)
                loadSftpDirectory(_currentSftpPath.value) // Refresh
            } catch (e: Exception) {
                _sftpError.value = "Create dir failed: ${e.localizedMessage ?: e.toString()}"
            }
        }
    }

    fun deleteSftpFile(filePath: String, isDirectory: Boolean) {
        val server = _selectedServer.value ?: return
        viewModelScope.launch {
            try {
                SshManager.deleteFile(server, filePath, isDirectory)
                loadSftpDirectory(_currentSftpPath.value) // Refresh
            } catch (e: Exception) {
                _sftpError.value = "Delete failed: ${e.localizedMessage ?: e.toString()}"
            }
        }
    }

    fun uploadSftpFile(fileName: String, content: String) {
        val server = _selectedServer.value ?: return
        val path = if (_currentSftpPath.value.endsWith("/")) "${_currentSftpPath.value}$fileName" else "${_currentSftpPath.value}/$fileName"
        viewModelScope.launch {
            try {
                SshManager.writeFileContent(server, path, content)
                loadSftpDirectory(_currentSftpPath.value) // Refresh
            } catch (e: Exception) {
                _sftpError.value = "Upload failed: ${e.localizedMessage ?: e.toString()}"
            }
        }
    }

    fun renameSftpFile(oldPath: String, newName: String) {
        val server = _selectedServer.value ?: return
        val lastSlash = oldPath.lastIndexOf('/')
        val parent = if (lastSlash >= 0) oldPath.substring(0, lastSlash + 1) else ""
        val newPath = "$parent$newName"
        viewModelScope.launch {
            try {
                SshManager.renameFile(server, oldPath, newPath)
                loadSftpDirectory(_currentSftpPath.value) // Refresh
            } catch (e: Exception) {
                _sftpError.value = "Rename failed: ${e.localizedMessage ?: e.toString()}"
            }
        }
    }

    // --- INTERACTIVE TERMINAL ACTIONS ---
    fun connectTerminal() {
        val server = _selectedServer.value ?: return
        if (shellConnection != null) return

        _isTerminalConnecting.value = true
        _terminalError.value = null
        _terminalOutput.value = "Connecting to ${server.name}...\n"

        viewModelScope.launch {
            try {
                val conn = SshManager.openShell(server)
                shellConnection = conn
                _isTerminalConnecting.value = false
                _terminalOutput.value = _terminalOutput.value + "Connected successfully!\n\n"

                // Launch terminal reader loop
                startTerminalReader(conn.inputStream)
            } catch (e: Exception) {
                _isTerminalConnecting.value = false
                _terminalError.value = e.localizedMessage ?: e.toString()
                _terminalOutput.value = _terminalOutput.value + "Connection failed: ${e.localizedMessage ?: e.toString()}\n"
            }
        }
    }

    private fun startTerminalReader(inputStream: InputStream) {
        terminalReaderJob = viewModelScope.launch(Dispatchers.IO) {
            val reader = InputStreamReader(inputStream, Charsets.UTF_8)
            val buffer = CharArray(1024)
            try {
                while (true) {
                    val read = reader.read(buffer)
                    if (read == -1) break
                    if (read > 0) {
                        val textChunk = String(buffer, 0, read)
                        val cleanedChunk = stripAnsiCodes(textChunk)
                        withContext(Dispatchers.Main) {
                            // Limit buffer size to last 20,000 chars for memory performance
                            val currentText = _terminalOutput.value
                            val newText = if (currentText.length > 20000) {
                                currentText.takeLast(10000) + cleanedChunk
                            } else {
                                currentText + cleanedChunk
                            }
                            _terminalOutput.value = newText
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _terminalOutput.value = _terminalOutput.value + "\nConnection lost: ${e.localizedMessage ?: e.toString()}\n"
                }
            } finally {
                disconnectTerminal()
            }
        }
    }

    fun sendTerminalInput(input: String) {
        val conn = shellConnection
        if (conn != null && conn.isConnected()) {
            viewModelScope.launch(Dispatchers.IO) {
                conn.writeCommand(input)
            }
        } else {
            _terminalOutput.value = _terminalOutput.value + "\nNot connected. Reconnecting...\n"
            connectTerminal()
        }
    }

    fun disconnectTerminal() {
        terminalReaderJob?.cancel()
        terminalReaderJob = null
        shellConnection?.disconnect()
        shellConnection = null
    }

    private fun stripAnsiCodes(text: String): String {
        // Strip ANSI codes so rendering is clean and standard
        return text.replace(Regex("\u001B\\[[;\\d]*[A-Za-z]"), "")
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
        disconnectTerminal()
        viewModelScope.launch {
            SshManager.disconnectAll()
        }
    }
}
