package com.gcap.client.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OpenAiModelListResponse(
    val data: List<OpenAiModelItem>? = null
)

@Serializable
data class OpenAiModelItem(
    val id: String,
    val created: Long? = null,
    @SerialName("owned_by")
    val ownedBy: String? = null
)

@Serializable
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val stream: Boolean = true,
    @SerialName("stream_options")
    val streamOptions: OpenAiStreamOptions? = OpenAiStreamOptions(includeUsage = true),
    val temperature: Float? = null,
    @SerialName("top_p")
    val topP: Float? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    @SerialName("reasoning_effort")
    val reasoningEffort: String? = null
)

@Serializable
data class OpenAiStreamOptions(
    @SerialName("include_usage")
    val includeUsage: Boolean = true
)

@Serializable
data class OpenAiMessage(
    val role: String, // "system", "user", "assistant"
    val content: JsonElement
)

@Serializable
data class OpenAiStreamChunk(
    val id: String? = null,
    val choices: List<OpenAiChoice>? = null,
    val usage: OpenAiUsage? = null
)

@Serializable
data class OpenAiChoice(
    val index: Int? = null,
    val delta: OpenAiDelta? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class OpenAiDelta(
    val role: String? = null,
    val content: String? = null,
    @SerialName("reasoning_content")
    val reasoningContent: String? = null,
    val reasoning: String? = null
)

@Serializable
data class OpenAiUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int? = null,
    @SerialName("completion_tokens")
    val completionTokens: Int? = null,
    @SerialName("total_tokens")
    val totalTokens: Int? = null
)
