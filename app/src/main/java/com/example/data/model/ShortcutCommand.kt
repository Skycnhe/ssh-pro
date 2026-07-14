package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shortcut_commands")
data class ShortcutCommand(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val command: String
)
