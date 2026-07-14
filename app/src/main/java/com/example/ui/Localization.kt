package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

enum class AppLanguage {
    SYSTEM, ENGLISH, CHINESE
}

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

interface AppStrings {
    val appName: String
    val addServer: String
    val editServer: String
    val deleteServer: String
    val deleteConfirm: String
    val cancel: String
    val save: String
    val create: String
    val host: String
    val port: String
    val username: String
    val password: String
    val privateKey: String
    val privateKeyPlaceholder: String
    val serverName: String
    val noServersTitle: String
    val noServersDesc: String
    val lastConnected: String
    
    // Monitor
    val dashboard: String
    val cpu: String
    val cpuUsage: String
    val ram: String
    val ramUsage: String
    val disk: String
    val diskStorage: String
    val network: String
    val upload: String
    val download: String
    val cores: String
    val total: String
    val free: String
    val used: String
    val loadingMetrics: String
    val connectionFailed: String
    val retry: String
    val activeProcesses: String
    val kernel: String
    val uptime: String
    
    // Terminal
    val terminal: String
    val terminalPlaceholder: String
    val connectingShell: String
    val reconnect: String
    val send: String
    val shortcuts: String
    val addShortcut: String
    val shortcutName: String
    val shortcutCommand: String
    val noShortcuts: String
    val deleteShortcut: String
    
    // SFTP
    val sftp: String
    val newFolder: String
    val createFolder: String
    val refresh: String
    val emptyDir: String
    val noFiles: String
    val folderName: String
    val size: String
    val permissions: String
    val fileViewer: String
    val directory: String
    val delete: String
    val createFile: String
    val fileName: String
    val fileContent: String
    
    // Settings
    val settings: String
    val theme: String
    val themeLight: String
    val themeDark: String
    val themeSystem: String
    val language: String
    val languageEnglish: String
    val languageChinese: String
    val languageSystem: String
    val shortcutManager: String
}

object EnStrings : AppStrings {
    override val appName = "ServerBox SSH"
    override val addServer = "Add Server"
    override val editServer = "Edit Server"
    override val deleteServer = "Delete Server"
    override val deleteConfirm = "Are you sure you want to delete this server?"
    override val cancel = "Cancel"
    override val save = "Save"
    override val create = "Create"
    override val host = "Host IP / Domain"
    override val port = "Port"
    override val username = "Username"
    override val password = "Password"
    override val privateKey = "Private Key (Optional)"
    override val privateKeyPlaceholder = "Paste OpenSSH private key content..."
    override val serverName = "Server Name"
    override val noServersTitle = "No Servers Configured"
    override val noServersDesc = "Tap the + button below to add your first SSH Linux server config and monitor metrics in real-time."
    override val lastConnected = "Last connected: "
    
    // Monitor
    override val dashboard = "Dashboard"
    override val cpu = "CPU Usage"
    override val cpuUsage = "CPU Usage"
    override val ram = "RAM Usage"
    override val ramUsage = "RAM Usage"
    override val disk = "Disk Mounts"
    override val diskStorage = "Disk Storage"
    override val network = "Network Traffic"
    override val upload = "Upload"
    override val download = "Download"
    override val cores = "Cores"
    override val total = "Total"
    override val free = "Free"
    override val used = "Used"
    override val loadingMetrics = "Connecting & loading live metrics..."
    override val connectionFailed = "SSH Connection Failed"
    override val retry = "Retry"
    override val activeProcesses = "Top Active Processes"
    override val kernel = "Kernel"
    override val uptime = "Uptime"
    
    // Terminal
    override val terminal = "Terminal Console"
    override val terminalPlaceholder = "Type command..."
    override val connectingShell = "Establishing secure SSH Shell..."
    override val reconnect = "Reconnect"
    override val send = "Send"
    override val shortcuts = "Shortcuts"
    override val addShortcut = "Add Shortcut"
    override val shortcutName = "Shortcut Name"
    override val shortcutCommand = "Command Text"
    override val noShortcuts = "No custom shortcut commands. Add some in settings!"
    override val deleteShortcut = "Delete Shortcut"
    
    // SFTP
    override val sftp = "SFTP File Explorer"
    override val newFolder = "New Folder"
    override val createFolder = "Create Folder"
    override val refresh = "Refresh"
    override val emptyDir = "This directory is empty"
    override val noFiles = "This directory is empty"
    override val folderName = "Folder Name"
    override val size = "Size"
    override val permissions = "Permissions"
    override val fileViewer = "File Viewer"
    override val directory = "Directory"
    override val delete = "Delete"
    override val createFile = "Create File"
    override val fileName = "File Name"
    override val fileContent = "File Content"
    
    // Settings
    override val settings = "Settings"
    override val theme = "Theme Mode"
    override val themeLight = "Light Theme (Day)"
    override val themeDark = "Dark Theme (Night)"
    override val themeSystem = "Follow System"
    override val language = "App Language"
    override val languageEnglish = "English"
    override val languageChinese = "简体中文"
    override val languageSystem = "Follow System"
    override val shortcutManager = "Shortcut Commands Manager"
}

object ZhStrings : AppStrings {
    override val appName = "ServerBox SSH"
    override val addServer = "添加服务器"
    override val editServer = "编辑服务器"
    override val deleteServer = "删除服务器"
    override val deleteConfirm = "确定要删除此服务器吗？"
    override val cancel = "取消"
    override val save = "保存"
    override val create = "创建"
    override val host = "主机 IP / 域名"
    override val port = "端口"
    override val username = "用户名"
    override val password = "密码"
    override val privateKey = "私钥 (选填)"
    override val privateKeyPlaceholder = "粘贴 OpenSSH 私钥内容..."
    override val serverName = "服务器名称"
    override val noServersTitle = "暂无服务器配置"
    override val noServersDesc = "点击下方的 + 按钮，添加您的第一个 SSH Linux 服务器配置，即可实时监控各项性能指标。"
    override val lastConnected = "上次连接: "
    
    // Monitor
    override val dashboard = "监控看板"
    override val cpu = "CPU 使用率"
    override val cpuUsage = "CPU 使用率"
    override val ram = "内存使用率"
    override val ramUsage = "内存使用率"
    override val disk = "磁盘挂载"
    override val diskStorage = "磁盘存储"
    override val network = "网络流量"
    override val upload = "上行"
    override val download = "下行"
    override val cores = "核心"
    override val total = "总量"
    override val free = "空闲"
    override val used = "已用"
    override val loadingMetrics = "正在建立连接并加载实时指标..."
    override val connectionFailed = "SSH 连接失败"
    override val retry = "重试"
    override val activeProcesses = "活跃进程"
    override val kernel = "内核"
    override val uptime = "运行时间"
    
    // Terminal
    override val terminal = "终端控制台"
    override val terminalPlaceholder = "输入命令..."
    override val connectingShell = "正在建立安全 SSH 终端会话..."
    override val reconnect = "重新连接"
    override val send = "发送"
    override val shortcuts = "快捷命令"
    override val addShortcut = "添加快捷命令"
    override val shortcutName = "快捷名称"
    override val shortcutCommand = "命令内容"
    override val noShortcuts = "暂无快捷命令，请在设置中添加！"
    override val deleteShortcut = "删除快捷命令"
    
    // SFTP
    override val sftp = "SFTP 文件浏览器"
    override val newFolder = "新建文件夹"
    override val createFolder = "新建文件夹"
    override val refresh = "刷新"
    override val emptyDir = "此目录为空"
    override val noFiles = "此目录为空"
    override val folderName = "文件夹名称"
    override val size = "大小"
    override val permissions = "权限"
    override val fileViewer = "文件查看器"
    override val directory = "目录"
    override val delete = "删除"
    override val createFile = "新建文件"
    override val fileName = "文件名称"
    override val fileContent = "文件内容"
    
    // Settings
    override val settings = "系统设置"
    override val theme = "主题模式"
    override val themeLight = "浅色模式 (白天)"
    override val themeDark = "深色模式 (夜晚)"
    override val themeSystem = "跟随系统"
    override val language = "应用语言"
    override val languageEnglish = "English"
    override val languageChinese = "简体中文"
    override val languageSystem = "跟随系统"
    override val shortcutManager = "快捷命令管理"
}

@Composable
fun getStrings(language: AppLanguage): AppStrings {
    return when (language) {
        AppLanguage.ENGLISH -> EnStrings
        AppLanguage.CHINESE -> ZhStrings
        AppLanguage.SYSTEM -> {
            val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
            if (locale.language.lowercase().startsWith("zh")) {
                ZhStrings
            } else {
                EnStrings
            }
        }
    }
}
