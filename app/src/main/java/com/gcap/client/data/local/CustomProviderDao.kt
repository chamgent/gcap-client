package com.gcap.client.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomProviderDao {
    @Query("SELECT * FROM custom_providers ORDER BY createdAt ASC")
    fun getAllProviders(): Flow<List<CustomProviderEntity>>

    @Query("SELECT * FROM custom_providers WHERE id = :providerId LIMIT 1")
    suspend fun getProviderById(providerId: String): CustomProviderEntity?

    @Upsert
    suspend fun upsertProvider(provider: CustomProviderEntity)

    @Delete
    suspend fun deleteProvider(provider: CustomProviderEntity)

    @Query("DELETE FROM custom_providers WHERE id = :providerId")
    suspend fun deleteProviderById(providerId: String)

    @Query("SELECT * FROM custom_models ORDER BY modelId ASC")
    fun getAllCustomModels(): Flow<List<CustomModelEntity>>

    @Query("SELECT * FROM custom_models WHERE providerId = :providerId ORDER BY modelId ASC")
    fun getModelsByProvider(providerId: String): Flow<List<CustomModelEntity>>

    @Query("SELECT * FROM custom_models WHERE providerId = :providerId")
    suspend fun getModelsByProviderSync(providerId: String): List<CustomModelEntity>

    @Upsert
    suspend fun upsertModels(models: List<CustomModelEntity>)

    @Query("DELETE FROM custom_models WHERE providerId = :providerId")
    suspend fun deleteModelsByProvider(providerId: String)

    @Transaction
    suspend fun replaceModelsForProvider(providerId: String, models: List<CustomModelEntity>) {
        deleteModelsByProvider(providerId)
        upsertModels(models)
    }
}
