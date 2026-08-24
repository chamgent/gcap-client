package com.gcap.client.data.repository

import com.gcap.client.data.local.ConversationDao
import com.gcap.client.data.local.ConversationEntity
import com.gcap.client.data.local.CustomModelEntity
import com.gcap.client.data.local.CustomProviderDao
import com.gcap.client.data.local.CustomProviderEntity
import com.gcap.client.data.local.MessageEntity
import com.gcap.client.data.local.SettingsDataStore
import com.gcap.client.data.model.GenerateContentRequest
import com.gcap.client.data.model.OpenAiChatRequest
import com.gcap.client.data.model.StreamResponse
import com.gcap.client.data.network.GeminiApiService
import com.gcap.client.data.network.OpenAiApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor(
    private val apiService: GeminiApiService,
    private val openAiApiService: OpenAiApiService,
    private val conversationDao: ConversationDao,
    private val customProviderDao: CustomProviderDao,
    private val settingsDataStore: SettingsDataStore
) {
    fun streamGenerateContent(
        apiKey: String,
        modelId: String,
        request: GenerateContentRequest
    ): Flow<StreamResponse> {
        return apiService.streamGenerateContent(apiKey, modelId, request)
    }

    fun streamOpenAiChat(
        baseUrl: String,
        apiKey: String,
        request: OpenAiChatRequest
    ): Flow<StreamResponse> {
        return openAiApiService.streamChatCompletion(baseUrl, apiKey, request)
    }

    suspend fun probeAndSaveModels(provider: CustomProviderEntity): Result<List<CustomModelEntity>> {
        val probeResult = openAiApiService.probeModels(
            providerId = provider.id,
            baseUrl = provider.baseUrl,
            apiKey = provider.apiKey
        )
        if (probeResult.isSuccess) {
            val models = probeResult.getOrDefault(emptyList())
            customProviderDao.replaceModelsForProvider(provider.id, models)
        }
        return probeResult
    }

    val apiKeyFlow: Flow<String> = settingsDataStore.apiKeyFlow

    fun getAllConversations(): Flow<List<ConversationEntity>> = conversationDao.getAllConversations()

    fun getConversationsByCategory(category: String): Flow<List<ConversationEntity>> =
        conversationDao.getConversationsByCategory(category)

    suspend fun getConversationById(conversationId: String): ConversationEntity? =
        conversationDao.getConversationById(conversationId)

    fun getMessages(conversationId: String): Flow<List<MessageEntity>> =
        conversationDao.getMessages(conversationId)

    fun getAllProviders(): Flow<List<CustomProviderEntity>> = customProviderDao.getAllProviders()

    suspend fun getProviderById(providerId: String): CustomProviderEntity? =
        customProviderDao.getProviderById(providerId)

    suspend fun upsertProvider(provider: CustomProviderEntity) {
        customProviderDao.upsertProvider(provider)
    }

    suspend fun deleteProvider(providerId: String) {
        customProviderDao.deleteProviderById(providerId)
    }

    fun getAllCustomModels(): Flow<List<CustomModelEntity>> = customProviderDao.getAllCustomModels()

    suspend fun updateModelCapabilities(modelId: String, supportsVision: Boolean, supportsReasoning: Boolean) {
        customProviderDao.updateModelCapabilities(modelId, supportsVision, supportsReasoning)
    }

    fun getModelsByProvider(providerId: String): Flow<List<CustomModelEntity>> =
        customProviderDao.getModelsByProvider(providerId)

    suspend fun insertConversation(conversation: ConversationEntity) {
        conversationDao.insertConversation(conversation)
    }

    suspend fun insertMessage(message: MessageEntity) {
        conversationDao.insertMessage(message)
    }

    suspend fun updateConversation(conversation: ConversationEntity) {
        conversationDao.updateConversation(conversation)
    }

    suspend fun updateConversationTitle(conversationId: String, title: String, updatedAt: Long = System.currentTimeMillis()) {
        conversationDao.updateConversationTitle(conversationId, title, updatedAt)
    }

    suspend fun updateMessageText(messageId: String, newText: String) {
        conversationDao.updateMessageText(messageId, newText)
    }

    suspend fun deleteMessagesAfterOrder(conversationId: String, orderIndex: Int) {
        conversationDao.deleteMessagesAfterOrder(conversationId, orderIndex)
    }

    suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteConversation(conversationId)
    }

    suspend fun deleteAllConversations() {
        conversationDao.deleteAllConversations()
    }
}
