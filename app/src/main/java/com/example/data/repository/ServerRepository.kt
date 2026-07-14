package com.example.data.repository

import com.example.data.db.ServerDao
import com.example.data.db.ShortcutCommandDao
import com.example.data.model.Server
import com.example.data.model.ShortcutCommand
import kotlinx.coroutines.flow.Flow

class ServerRepository(
    private val serverDao: ServerDao,
    private val shortcutDao: ShortcutCommandDao
) {
    val allServers: Flow<List<Server>> = serverDao.getAllServers()
    val allShortcuts: Flow<List<ShortcutCommand>> = shortcutDao.getAllShortcuts()

    suspend fun getServerById(id: Int): Server? {
        return serverDao.getServerById(id)
    }

    suspend fun insertServer(server: Server): Long {
        return serverDao.insertServer(server)
    }

    suspend fun updateServer(server: Server) {
        serverDao.updateServer(server)
    }

    suspend fun deleteServer(server: Server) {
        serverDao.deleteServer(server)
    }

    suspend fun insertShortcut(shortcut: ShortcutCommand): Long {
        return shortcutDao.insertShortcut(shortcut)
    }

    suspend fun updateShortcut(shortcut: ShortcutCommand) {
        shortcutDao.updateShortcut(shortcut)
    }

    suspend fun deleteShortcut(shortcut: ShortcutCommand) {
        shortcutDao.deleteShortcut(shortcut)
    }
}
