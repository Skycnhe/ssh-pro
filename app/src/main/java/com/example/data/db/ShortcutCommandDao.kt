package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ShortcutCommand
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutCommandDao {
    @Query("SELECT * FROM shortcut_commands ORDER BY id ASC")
    fun getAllShortcuts(): Flow<List<ShortcutCommand>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: ShortcutCommand): Long

    @Update
    suspend fun updateShortcut(shortcut: ShortcutCommand)

    @Delete
    suspend fun deleteShortcut(shortcut: ShortcutCommand)
}
