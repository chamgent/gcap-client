package com.gcap.client.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "custom_providers")
data class CustomProviderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
