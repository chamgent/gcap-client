package com.gcap.client.data.repository

import com.gcap.client.data.local.ConversationDao
import com.gcap.client.data.local.ConversationEntity
import com.gcap.client.data.local.MessageEntity
import com.gcap.client.data.local.SettingsDataStore
import com.gcap.client.data.model.Content
import com.gcap.client.data.model.GenerateContentRequest
import com.gcap.client.data.model.GenerationConfig
import com.gcap.client.data.model.GoogleMapsTool
import com.gcap.client.data.model.GoogleSearchTool
import com.gcap.client.data.model.ImageConfig
import com.gcap.client.data.model.ImageOutputOptions
import com.gcap.client.data.model.ModelRequestFormat
import com.gcap.client.data.model.RetrievalConfig
import com.gcap.client.data.model.SafetySetting
import com.gcap.client.data.model.StreamResponse
import com.gcap.client.data.model.SystemInstruction
import com.gcap.client.data.model.SystemPart
import com.gcap.client.data.model.ThinkingConfig
import com.gcap.client.data.model.Tool
import com.gcap.client.data.model.ToolConfig
import com.gcap.client.data.network.GeminiApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor(
    private val apiService: GeminiApiService,
    private val conversationDao: ConversationDao,
    private val settingsDataStore: SettingsDataStore
) {
    fun streamGenerateContent(
        apiKey: String,
        modelId: String,
        request: GenerateContentRequest
    ): Flow<StreamResponse> {
        return apiService.streamGenerateContent(apiKey, modelId, request)
    }

    val apiKeyFlow: Flow<String> = settingsDataStore.apiKeyFlow

    fun getAllConversations(): Flow<List<ConversationEntity>> = conversationDao.getAllConversations()

    fun getConversationsByCategory(category: String): Flow<List<ConversationEntity>> =
        conversationDao.getConversationsByCategory(category)

    fun getMessages(conversationId: String): Flow<List<MessageEntity>> =
        conversationDao.getMessages(conversationId)

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
