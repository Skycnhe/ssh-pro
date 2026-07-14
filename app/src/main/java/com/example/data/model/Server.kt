package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class Server(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authType: String = "PASSWORD", // "PASSWORD" or "PRIVATE_KEY"
    val password: String? = null,
    val privateKey: String? = null,
    val passphrase: String? = null,
    val lastConnected: Long = 0
)
