package com.example.ssh

import com.example.data.model.Server
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties

object SshManager {
    private val jsch = JSch()

    // Active session pool mapped by server ID
    private val activeSessions = mutableMapOf<Int, Session>()

    /**
     * Get or create an active SSH Session for a server
     */
    private suspend fun getOrCreateSession(server: Server): Session = withContext(Dispatchers.IO) {
        val existing = activeSessions[server.id]
        if (existing != null && existing.isConnected) {
            return@withContext existing
        }

        // Clean up any stale session
        try { existing?.disconnect() } catch (e: Exception) {}

        val session = jsch.getSession(server.username, server.host, server.port)
        
        if (server.authType == "PASSWORD") {
            session.setPassword(server.password)
        } else {
            // Private Key Authentication
            jsch.removeAllIdentity()
            val prvKeyBytes = server.privateKey?.toByteArray(Charsets.UTF_8)
            val passphraseBytes = server.passphrase?.toByteArray(Charsets.UTF_8)
            jsch.addIdentity(server.name, prvKeyBytes, null, passphraseBytes)
        }

        val config = Properties().apply {
            put("StrictHostKeyChecking", "no")
            put("PreferredAuthentications", "publickey,password,keyboard-interactive")
        }
        session.setConfig(config)
        session.connect(15000) // 15s timeout

        activeSessions[server.id] = session
        session
    }

    /**
     * Close the active session for a server
     */
    suspend fun disconnect(serverId: Int) = withContext(Dispatchers.IO) {
        val session = activeSessions.remove(serverId)
        try {
            session?.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Close all active sessions
     */
    suspend fun disconnectAll() = withContext(Dispatchers.IO) {
        activeSessions.keys.toList().forEach { id ->
            disconnect(id)
        }
    }

    /**
     * Run a command and return stdout + stderr combined
     */
    suspend fun executeCommand(server: Server, command: String): String = withContext(Dispatchers.IO) {
        val session = getOrCreateSession(server)
        val channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(command)

        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        channel.outputStream = outputStream
        channel.setErrStream(errorStream)

        channel.connect(10000)

        // Wait until command completes or timeout
        val startTime = System.currentTimeMillis()
        while (channel.isConnected && (System.currentTimeMillis() - startTime) < 15000) {
            withContext(Dispatchers.IO) {
                Thread.sleep(100)
            }
        }

        val result = outputStream.toString("UTF-8")
        val error = errorStream.toString("UTF-8")

        channel.disconnect()

        if (error.isNotEmpty()) {
            if (result.isNotEmpty()) "$result\nError: $error" else "Error: $error"
        } else {
            result
        }
    }

    /**
     * Open an interactive Shell channel
     */
    suspend fun openShell(server: Server): ShellConnection = withContext(Dispatchers.IO) {
        val session = getOrCreateSession(server)
        val channel = session.openChannel("shell") as ChannelShell
        
        channel.setPty(true)
        channel.setPtyType("xterm")
        
        val inputStream = channel.inputStream
        val outputStream = channel.outputStream

        channel.connect(10000)

        ShellConnection(channel, inputStream, outputStream)
    }

    /**
     * SFTP List Files
     */
    suspend fun listFiles(server: Server, path: String): List<SftpFile> = withContext(Dispatchers.IO) {
        val session = getOrCreateSession(server)
        val channel = session.openChannel("sftp") as ChannelSftp
        channel.connect(10000)

        val files = mutableListOf<SftpFile>()
        try {
            channel.cd(path)
            val currentDir = channel.pwd()
            val list = channel.ls(".")
            for (entry in list) {
                if (entry is ChannelSftp.LsEntry) {
                    val attrs = entry.attrs
                    val name = entry.filename
                    if (name == "." || name == "..") continue

                    val isDir = attrs.isDir
                    val size = attrs.size
                    val permissions = attrs.permissionsString
                    val mtime = attrs.mtimeString
                    val fullPath = if (currentDir.endsWith("/")) "$currentDir$name" else "$currentDir/$name"

                    files.add(SftpFile(name, size, isDir, permissions, mtime, fullPath))
                }
            }
        } finally {
            channel.disconnect()
        }
        // Sort directories first, then alphabetical
        files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    /**
     * SFTP Delete File/Directory
     */
    suspend fun deleteFile(server: Server, path: String, isDirectory: Boolean) = withContext(Dispatchers.IO) {
        val session = getOrCreateSession(server)
        val channel = session.openChannel("sftp") as ChannelSftp
        channel.connect(10000)
        try {
            if (isDirectory) {
                channel.rmdir(path)
            } else {
                channel.rm(path)
            }
        } finally {
            channel.disconnect()
        }
    }

    /**
     * SFTP Create Directory
     */
    suspend fun createDirectory(server: Server, parentPath: String, dirName: String) = withContext(Dispatchers.IO) {
        val session = getOrCreateSession(server)
        val channel = session.openChannel("sftp") as ChannelSftp
        channel.connect(10000)
        try {
            val path = if (parentPath.endsWith("/")) "$parentPath$dirName" else "$parentPath/$dirName"
            channel.mkdir(path)
        } finally {
            channel.disconnect()
        }
    }

    /**
     * SFTP Read/Download File content as String
     */
    suspend fun readFileContent(server: Server, filePath: String): String = withContext(Dispatchers.IO) {
        val session = getOrCreateSession(server)
        val channel = session.openChannel("sftp") as ChannelSftp
        channel.connect(10000)
        val outputStream = ByteArrayOutputStream()
        try {
            channel.get(filePath, outputStream)
            outputStream.toString("UTF-8")
        } finally {
            channel.disconnect()
        }
    }

    /**
     * SFTP Write/Upload File content from String
     */
    suspend fun writeFileContent(server: Server, filePath: String, content: String) = withContext(Dispatchers.IO) {
        val session = getOrCreateSession(server)
        val channel = session.openChannel("sftp") as ChannelSftp
        channel.connect(10000)
        try {
            val inputStream = content.byteInputStream(Charsets.UTF_8)
            channel.put(inputStream, filePath, ChannelSftp.OVERWRITE)
        } finally {
            channel.disconnect()
        }
    }

    /**
     * SFTP Rename File/Directory
     */
    suspend fun renameFile(server: Server, oldPath: String, newPath: String) = withContext(Dispatchers.IO) {
        val session = getOrCreateSession(server)
        val channel = session.openChannel("sftp") as ChannelSftp
        channel.connect(10000)
        try {
            channel.rename(oldPath, newPath)
        } finally {
            channel.disconnect()
        }
    }
}

/**
 * Represent an active interactive shell connection
 */
class ShellConnection(
    private val channel: ChannelShell,
    val inputStream: InputStream,
    val outputStream: OutputStream
) {
    fun writeCommand(cmd: String) {
        try {
            outputStream.write(cmd.toByteArray(Charsets.UTF_8))
            outputStream.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isConnected(): Boolean = channel.isConnected

    fun disconnect() {
        try { channel.disconnect() } catch (e: Exception) {}
    }
}

/**
 * Data Model representing a File in SFTP explorer
 */
data class SftpFile(
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val permissions: String,
    val mtime: String,
    val fullPath: String
)
