package com.gcap.client.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversationById(conversationId: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE modelCategory = :category ORDER BY updatedAt DESC")
    fun getConversationsByCategory(category: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY orderIndex ASC")
    fun getMessages(conversationId: String): Flow<List<MessageEntity>>

    @Upsert
    suspend fun insertConversation(conversation: ConversationEntity)

    @Upsert
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET title = :newTitle, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun updateConversationTitle(conversationId: String, newTitle: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE messages SET textContent = :newText WHERE id = :messageId")
    suspend fun updateMessageText(messageId: String, newText: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND orderIndex > :orderIndex")
    suspend fun deleteMessagesAfterOrder(conversationId: String, orderIndex: Int)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}
