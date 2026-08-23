package com.gcap.client.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_models",
    foreignKeys = [
        ForeignKey(
            entity = CustomProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["providerId"])]
)
data class CustomModelEntity(
    @PrimaryKey val id: String, // e.g. "${providerId}_${modelId}"
    val providerId: String,
    val modelId: String,
    val displayName: String,
    val supportsVision: Boolean = false,
    val supportsReasoning: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
