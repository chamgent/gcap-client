package com.gcap.client.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

enum class MessageRole { USER, MODEL }

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val textContent: String = "",
    val images: List<MessageImage> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val thinkingContent: String = "",
    val toolCallContent: String = "",
    val modelName: String = "",
    val promptTokens: Int? = null,
    val candidatesTokens: Int? = null,
    val error: String? = null
)

@Serializable
data class MessageImage(
    val base64Data: String? = null,
    val mimeType: String = "image/png",
    val uri: String? = null,
    val name: String? = null,
    val size: Long = 0
)
