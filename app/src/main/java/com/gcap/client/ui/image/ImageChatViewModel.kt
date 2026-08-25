package com.gcap.client.ui.image

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcap.client.data.local.ConversationEntity
import com.gcap.client.data.local.ImageStorageManager
import com.gcap.client.data.local.MessageEntity
import com.gcap.client.data.local.SettingsDataStore
import com.gcap.client.data.model.ChatMessage
import com.gcap.client.data.model.Content
import com.gcap.client.data.model.GenerateContentRequest
import com.gcap.client.data.model.GenerationConfig
import com.gcap.client.data.model.GoogleSearchTool
import com.gcap.client.data.model.ImageConfig
import com.gcap.client.data.model.ImageOutputOptions
import com.gcap.client.data.model.InlineData
import com.gcap.client.data.model.MessageImage
import com.gcap.client.data.model.MessageRole
import com.gcap.client.data.model.Part
import com.gcap.client.data.model.SafetySetting
import com.gcap.client.data.model.ThinkingConfig
import com.gcap.client.data.model.Tool
import com.gcap.client.data.repository.GeminiRepository
import com.gcap.client.ui.components.SafetySettingsState
import com.gcap.client.ui.components.formatToolCalls
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

data class ActiveImageStreamState(
    var job: Job? = null,
    val conversationId: String,
    val modelMessageId: String,
    var accumulatedText: String = "",
    var accumulatedThought: String = "",
    var accumulatedToolCall: String = "",
    var promptTokens: Int? = null,
    var candidatesTokens: Int? = null,
    val accumulatedImages: MutableList<MessageImage> = CopyOnWriteArrayList(),
    @Volatile var isStreaming: Boolean = true,
    @Volatile var error: String? = null
)

data class ImageChatUiState(
    val conversationId: String = UUID.randomUUID().toString(),
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val error: String? = null,

    // Parameters
    val temperature: Float = 1.0f,
    val maxOutputTokens: Int = 32768,
    val topP: Float = 0.95f,
    val thinkingLevel: String = "MINIMAL",
    val googleSearchEnabled: Boolean = true,
    val responseModalities: List<String> = listOf("TEXT", "IMAGE"),

    // Image Config
    val aspectRatio: String = "auto",
    val imageSize: String = "1K",
    val outputMimeType: String = "image/png",
    val personGeneration: String = "ALLOW_ALL",

    // Safety
    val safetySettings: SafetySettingsState = SafetySettingsState()
)

@HiltViewModel
class ImageChatViewModel @Inject constructor(
    private val repository: GeminiRepository,
    private val settingsDataStore: SettingsDataStore,
    private val imageStorageManager: ImageStorageManager,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageChatUiState())
    val uiState: StateFlow<ImageChatUiState> = _uiState.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = repository.getConversationsByCategory("IMAGE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val activeStreams = ConcurrentHashMap<String, ActiveImageStreamState>()
    val modelId = "gemini-3.1-flash-image"

    fun createNewConversation() {
        _uiState.update {
            it.copy(
                conversationId = UUID.randomUUID().toString(),
                messages = emptyList(),
                isGenerating = false,
                error = null
            )
        }
    }

    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            repository.getMessages(conversationId).first().let { entities ->
                val loadedMessages = entities.map { entity ->
                    val images = try {
                        if (entity.imagesJson.isNotBlank()) {
                            json.decodeFromString<List<MessageImage>>(entity.imagesJson)
                        } else emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    ChatMessage(
                        id = entity.id,
                        role = if (entity.role == "USER") MessageRole.USER else MessageRole.MODEL,
                        textContent = entity.textContent,
                        images = images,
                        thinkingContent = entity.thinkingContent,
                        toolCallContent = entity.toolCallContent,
                        timestamp = entity.timestamp,
                        isStreaming = false
                    )
                }.toMutableList()

                val activeStream = activeStreams[conversationId]
                val isGenerating = activeStream?.isStreaming == true
                if (activeStream != null && isGenerating) {
                    if (loadedMessages.none { it.id == activeStream.modelMessageId }) {
                        loadedMessages.add(
                            ChatMessage(
                                id = activeStream.modelMessageId,
                                role = MessageRole.MODEL,
                                textContent = activeStream.accumulatedText,
                                thinkingContent = activeStream.accumulatedThought,
                                toolCallContent = activeStream.accumulatedToolCall,
                                promptTokens = activeStream.promptTokens,
                                candidatesTokens = activeStream.candidatesTokens,
                                images = activeStream.accumulatedImages.toList(),
                                modelName = "Gemini 3.1 Flash Image",
                                timestamp = System.currentTimeMillis(),
                                isStreaming = true
                            )
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        conversationId = conversationId,
                        messages = loadedMessages,
                        isGenerating = isGenerating,
                        error = null
                    )
                }
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            activeStreams.remove(conversationId)?.job?.cancel()
            repository.deleteConversation(conversationId)
            if (_uiState.value.conversationId == conversationId) {
                createNewConversation()
            }
        }
    }

    fun updateTemperature(temp: Float) = _uiState.update { it.copy(temperature = temp) }
    fun updateMaxOutputTokens(tokens: Int) = _uiState.update { it.copy(maxOutputTokens = tokens) }
    fun updateTopP(topP: Float) = _uiState.update { it.copy(topP = topP) }
    fun updateThinkingLevel(level: String) = _uiState.update { it.copy(thinkingLevel = level) }
    fun updateGoogleSearchEnabled(enabled: Boolean) = _uiState.update { it.copy(googleSearchEnabled = enabled) }
    fun updateResponseModalities(modalities: List<String>) = _uiState.update { it.copy(responseModalities = modalities) }

    fun updateAspectRatio(ratio: String) = _uiState.update { it.copy(aspectRatio = ratio) }
    fun updateImageSize(size: String) = _uiState.update { it.copy(imageSize = size) }
    fun updateOutputMimeType(mimeType: String) = _uiState.update { it.copy(outputMimeType = mimeType) }
    fun updatePersonGeneration(pg: String) = _uiState.update { it.copy(personGeneration = pg) }
    fun updateSafetySettings(settings: SafetySettingsState) = _uiState.update { it.copy(safetySettings = settings) }

    fun stopGeneration() {
        val currentConvId = _uiState.value.conversationId
        val activeStream = activeStreams.remove(currentConvId)
        activeStream?.job?.cancel()

        if (activeStream != null && (activeStream.accumulatedText.isNotBlank() || activeStream.accumulatedImages.isNotEmpty())) {
            viewModelScope.launch {
                val imagesForDb = activeStream.accumulatedImages.map { img ->
                    val localUri = img.uri ?: (img.base64Data?.let { imageStorageManager.saveBase64Image(it, img.mimeType) })
                    MessageImage(uri = localUri, mimeType = img.mimeType)
                }
                repository.insertMessage(
                    MessageEntity(
                        id = activeStream.modelMessageId,
                        conversationId = currentConvId,
                        role = "MODEL",
                        textContent = activeStream.accumulatedText,
                        imagesJson = if (imagesForDb.isNotEmpty()) json.encodeToString(imagesForDb) else "",
                        thinkingContent = activeStream.accumulatedThought,
                        toolCallContent = activeStream.accumulatedToolCall,
                        timestamp = System.currentTimeMillis(),
                        orderIndex = _uiState.value.messages.size
                    )
                )
            }
        }

        _uiState.update { state ->
            if (state.conversationId != currentConvId) state else {
                val updated = state.messages.map { if (it.isStreaming) it.copy(isStreaming = false) else it }
                state.copy(isGenerating = false, messages = updated)
            }
        }
    }

    fun sendMessage(text: String, images: List<MessageImage> = emptyList()) {
        if (text.isBlank() && images.isEmpty()) return

        val currentState = _uiState.value
        val targetConversationId = currentState.conversationId
        if (activeStreams[targetConversationId]?.isStreaming == true) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            textContent = text,
            images = images,
            timestamp = System.currentTimeMillis()
        )

        val modelMessageId = UUID.randomUUID().toString()
        val initialModelMessage = ChatMessage(
            id = modelMessageId,
            role = MessageRole.MODEL,
            textContent = "",
            thinkingContent = "",
            modelName = "Gemini 3.1 Flash Image",
            timestamp = System.currentTimeMillis(),
            isStreaming = true
        )

        val updatedMessages = currentState.messages + userMessage + initialModelMessage
        _uiState.update { state ->
            if (state.conversationId == targetConversationId) {
                state.copy(
                    messages = updatedMessages,
                    isGenerating = true,
                    error = null
                )
            } else state
        }

        // Persist conversation and user message
        viewModelScope.launch {
            repository.insertConversation(
                ConversationEntity(
                    id = targetConversationId,
                    title = if (currentState.messages.isEmpty()) text.take(30).ifEmpty { "图片创作" } else "图片对话",
                    modelId = modelId,
                    modelCategory = "IMAGE",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )

            repository.insertMessage(
                MessageEntity(
                    id = userMessage.id,
                    conversationId = targetConversationId,
                    role = "USER",
                    textContent = userMessage.textContent,
                    imagesJson = if (images.isNotEmpty()) json.encodeToString(images) else "",
                    thinkingContent = "",
                    toolCallContent = "",
                    timestamp = userMessage.timestamp,
                    orderIndex = currentState.messages.size
                )
            )
        }

        val historyMessages = currentState.messages + userMessage
        val streamState = ActiveImageStreamState(
            conversationId = targetConversationId,
            modelMessageId = modelMessageId
        )

        val job = viewModelScope.launch {
            val apiKey = try {
                settingsDataStore.getApiKey().first()
            } catch (e: Exception) {
                ""
            }

            if (apiKey.isBlank()) {
                _uiState.update { state ->
                    if (state.conversationId == targetConversationId) {
                        state.copy(error = "请先在设置中输入 API Key", isGenerating = false)
                    } else state
                }
                return@launch
            }

            try {
                val request = buildRequest(currentState, historyMessages)
                val searchQueries = mutableListOf<String>()
                val groundingSources = mutableListOf<Pair<String, String>>()
                val functionCalls = mutableListOf<String>()

                repository.streamGenerateContent(apiKey, modelId, request).collect { response ->
                    response.usageMetadata?.let { usage ->
                        if (usage.promptTokenCount != null) streamState.promptTokens = usage.promptTokenCount
                        if (usage.candidatesTokenCount != null) streamState.candidatesTokens = usage.candidatesTokenCount
                    }
                    val candidate = response.candidates?.firstOrNull()
                    candidate?.groundingMetadata?.let { gm ->
                        gm.webSearchQueries?.forEach { q -> if (q !in searchQueries) searchQueries.add(q) }
                        gm.groundingChunks?.forEach { chunk ->
                            chunk.web?.let { web ->
                                if (!web.uri.isNullOrBlank()) {
                                    val title = web.title?.ifBlank { web.uri } ?: web.uri
                                    if (groundingSources.none { it.second == web.uri }) {
                                        groundingSources.add(title to web.uri)
                                    }
                                }
                            }
                        }
                    }

                    candidate?.content?.parts?.forEach { part ->
                        part.functionCall?.let { fc ->
                            val fcName = fc.name ?: "tool"
                            val desc = if (fc.args != null) "$fcName(${fc.args})" else "$fcName()"
                            if (desc !in functionCalls) functionCalls.add(desc)
                        }
                        if (part.thought == true) {
                            part.text?.let { chunk ->
                                streamState.accumulatedThought += chunk
                            }
                        } else {
                            part.text?.let { chunk ->
                                streamState.accumulatedText += chunk
                            }
                        }
                        part.inlineData?.let { inline ->
                            if (inline.data.isNotBlank()) {
                                val fileUri = imageStorageManager.saveBase64Image(inline.data, inline.mimeType)
                                streamState.accumulatedImages.add(
                                    MessageImage(
                                        base64Data = inline.data,
                                        mimeType = inline.mimeType.ifBlank { "image/png" },
                                        uri = fileUri
                                    )
                                )
                            }
                        }
                    }

                    streamState.accumulatedToolCall = formatToolCalls(functionCalls, searchQueries, groundingSources)

                    _uiState.update { state ->
                        if (state.conversationId != targetConversationId) state else {
                            val currentList = state.messages.map { msg ->
                                if (msg.id == modelMessageId) {
                                    msg.copy(
                                        textContent = streamState.accumulatedText,
                                        thinkingContent = streamState.accumulatedThought,
                                        toolCallContent = streamState.accumulatedToolCall,
                                        promptTokens = streamState.promptTokens,
                                        candidatesTokens = streamState.candidatesTokens,
                                        images = streamState.accumulatedImages.toList(),
                                        isStreaming = true
                                    )
                                } else msg
                            }
                            state.copy(messages = currentList, isGenerating = true)
                        }
                    }
                }

                // Complete
                streamState.isStreaming = false

                _uiState.update { state ->
                    if (state.conversationId != targetConversationId) state else {
                        val finalMessages = state.messages.map { msg ->
                            if (msg.id == modelMessageId) {
                                msg.copy(isStreaming = false)
                            } else msg
                        }
                        state.copy(messages = finalMessages, isGenerating = false)
                    }
                }

                val imagesForDb = streamState.accumulatedImages.map { img ->
                    val localUri = img.uri ?: (img.base64Data?.let { imageStorageManager.saveBase64Image(it, img.mimeType) })
                    MessageImage(
                        uri = localUri,
                        mimeType = img.mimeType
                    )
                }

                // Persist model message
                repository.insertMessage(
                    MessageEntity(
                        id = modelMessageId,
                        conversationId = targetConversationId,
                        role = "MODEL",
                        textContent = streamState.accumulatedText,
                        imagesJson = if (imagesForDb.isNotEmpty()) json.encodeToString(imagesForDb) else "",
                        thinkingContent = streamState.accumulatedThought,
                        toolCallContent = streamState.accumulatedToolCall,
                        timestamp = System.currentTimeMillis(),
                        orderIndex = historyMessages.size + 1
                    )
                )

                repository.updateConversationTitle(
                    conversationId = targetConversationId,
                    title = text.take(30).ifEmpty { "图片创作" },
                    updatedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                streamState.isStreaming = false
                streamState.error = e.message ?: "图片生成失败"
                _uiState.update { state ->
                    if (state.conversationId == targetConversationId) {
                        val errorMessages = state.messages.map { msg ->
                            if (msg.id == modelMessageId) {
                                msg.copy(isStreaming = false, error = e.message ?: "图片生成失败")
                            } else msg
                        }
                        state.copy(messages = errorMessages, isGenerating = false, error = e.message ?: "图片生成失败")
                    } else state
                }
            } finally {
                activeStreams.remove(targetConversationId)
            }
        }
        streamState.job = job
        activeStreams[targetConversationId] = streamState
    }

    fun editModelMessage(messageId: String, newText: String) {
        _uiState.update { state ->
            val updated = state.messages.map { msg ->
                if (msg.id == messageId) msg.copy(textContent = newText) else msg
            }
            state.copy(messages = updated)
        }
        viewModelScope.launch {
            repository.updateMessageText(messageId, newText)
        }
    }

    fun editAndResendUserMessage(messageId: String, newText: String) {
        val currentState = _uiState.value
        val targetConversationId = currentState.conversationId
        val userMsgIndex = currentState.messages.indexOfFirst { it.id == messageId }
        if (userMsgIndex == -1) return

        activeStreams.remove(targetConversationId)?.job?.cancel()

        val targetUserMsg = currentState.messages[userMsgIndex].copy(textContent = newText)
        val truncatedHistory = currentState.messages.take(userMsgIndex) + targetUserMsg

        val modelMessageId = UUID.randomUUID().toString()
        val initialModelMessage = ChatMessage(
            id = modelMessageId,
            role = MessageRole.MODEL,
            textContent = "",
            thinkingContent = "",
            timestamp = System.currentTimeMillis(),
            isStreaming = true
        )

        val newMessages = truncatedHistory + initialModelMessage
        _uiState.update { state ->
            if (state.conversationId == targetConversationId) {
                state.copy(
                    messages = newMessages,
                    isGenerating = true,
                    error = null
                )
            } else state
        }

        viewModelScope.launch {
            repository.deleteMessagesAfterOrder(targetConversationId, userMsgIndex)
            repository.updateMessageText(messageId, newText)
        }

        val streamState = ActiveImageStreamState(
            conversationId = targetConversationId,
            modelMessageId = modelMessageId
        )

        val job = viewModelScope.launch {
            val apiKey = try {
                settingsDataStore.getApiKey().first()
            } catch (e: Exception) {
                ""
            }

            if (apiKey.isBlank()) {
                _uiState.update { state ->
                    if (state.conversationId == targetConversationId) {
                        state.copy(error = "请先在设置中输入 API Key", isGenerating = false)
                    } else state
                }
                return@launch
            }

            try {
                val request = buildRequest(currentState, truncatedHistory)
                val searchQueries = mutableListOf<String>()
                val groundingSources = mutableListOf<Pair<String, String>>()
                val functionCalls = mutableListOf<String>()

                repository.streamGenerateContent(apiKey, modelId, request).collect { response ->
                    response.usageMetadata?.let { usage ->
                        if (usage.promptTokenCount != null) streamState.promptTokens = usage.promptTokenCount
                        if (usage.candidatesTokenCount != null) streamState.candidatesTokens = usage.candidatesTokenCount
                    }
                    val candidate = response.candidates?.firstOrNull()
                    candidate?.groundingMetadata?.let { gm ->
                        gm.webSearchQueries?.forEach { q -> if (q !in searchQueries) searchQueries.add(q) }
                        gm.groundingChunks?.forEach { chunk ->
                            chunk.web?.let { web ->
                                if (!web.uri.isNullOrBlank()) {
                                    val title = web.title?.ifBlank { web.uri } ?: web.uri
                                    if (groundingSources.none { it.second == web.uri }) {
                                        groundingSources.add(title to web.uri)
                                    }
                                }
                            }
                        }
                    }

                    candidate?.content?.parts?.forEach { part ->
                        part.functionCall?.let { fc ->
                            val fcName = fc.name ?: "tool"
                            val desc = if (fc.args != null) "$fcName(${fc.args})" else "$fcName()"
                            if (desc !in functionCalls) functionCalls.add(desc)
                        }
                        if (part.thought == true) {
                            part.text?.let { chunk ->
                                streamState.accumulatedThought += chunk
                            }
                        } else {
                            part.text?.let { chunk ->
                                streamState.accumulatedText += chunk
                            }
                        }
                        part.inlineData?.let { inline ->
                            if (inline.data.isNotBlank()) {
                                val fileUri = imageStorageManager.saveBase64Image(inline.data, inline.mimeType)
                                streamState.accumulatedImages.add(
                                    MessageImage(
                                        base64Data = inline.data,
                                        mimeType = inline.mimeType.ifBlank { "image/png" },
                                        uri = fileUri
                                    )
                                )
                            }
                        }
                    }

                    streamState.accumulatedToolCall = formatToolCalls(functionCalls, searchQueries, groundingSources)

                    _uiState.update { state ->
                        if (state.conversationId != targetConversationId) state else {
                            val currentList = state.messages.map { msg ->
                                if (msg.id == modelMessageId) {
                                    msg.copy(
                                        textContent = streamState.accumulatedText,
                                        thinkingContent = streamState.accumulatedThought,
                                        toolCallContent = streamState.accumulatedToolCall,
                                        promptTokens = streamState.promptTokens,
                                        candidatesTokens = streamState.candidatesTokens,
                                        images = streamState.accumulatedImages.toList(),
                                        isStreaming = true
                                    )
                                } else msg
                            }
                            state.copy(messages = currentList, isGenerating = true)
                        }
                    }
                }

                // Complete
                streamState.isStreaming = false

                _uiState.update { state ->
                    if (state.conversationId != targetConversationId) state else {
                        val finalMessages = state.messages.map { msg ->
                            if (msg.id == modelMessageId) {
                                msg.copy(isStreaming = false)
                            } else msg
                        }
                        state.copy(messages = finalMessages, isGenerating = false)
                    }
                }

                val imagesForDb = streamState.accumulatedImages.map { img ->
                    val localUri = img.uri ?: (img.base64Data?.let { imageStorageManager.saveBase64Image(it, img.mimeType) })
                    MessageImage(
                        uri = localUri,
                        mimeType = img.mimeType
                    )
                }

                // Persist model message
                repository.insertMessage(
                    MessageEntity(
                        id = modelMessageId,
                        conversationId = targetConversationId,
                        role = "MODEL",
                        textContent = streamState.accumulatedText,
                        imagesJson = if (imagesForDb.isNotEmpty()) json.encodeToString(imagesForDb) else "",
                        thinkingContent = streamState.accumulatedThought,
                        toolCallContent = streamState.accumulatedToolCall,
                        timestamp = System.currentTimeMillis(),
                        orderIndex = userMsgIndex + 1
                    )
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                streamState.isStreaming = false
                streamState.error = e.message ?: "图片生成失败"
                _uiState.update { state ->
                    if (state.conversationId == targetConversationId) {
                        val errorMessages = state.messages.map { msg ->
                            if (msg.id == modelMessageId) {
                                msg.copy(isStreaming = false, error = e.message ?: "图片生成失败")
                            } else msg
                        }
                        state.copy(messages = errorMessages, isGenerating = false, error = e.message ?: "图片生成失败")
                    } else state
                }
            } finally {
                activeStreams.remove(targetConversationId)
            }
        }
        streamState.job = job
        activeStreams[targetConversationId] = streamState
    }

    private fun buildRequest(state: ImageChatUiState, messages: List<ChatMessage>): GenerateContentRequest {
        val contents = messages.mapNotNull { msg ->
            if (msg.error != null && msg.textContent.isBlank() && msg.images.isEmpty()) {
                return@mapNotNull null
            }
            val parts = mutableListOf<Part>()
            msg.images.forEach { img ->
                val base64 = img.base64Data ?: (img.uri?.let { imageStorageManager.getBase64FromUri(it) })
                base64?.let { data ->
                    val cleanBase64 = data.substringAfter("base64,").trim()
                    if (cleanBase64.isNotEmpty()) {
                        parts.add(Part(inlineData = InlineData(mimeType = img.mimeType, data = cleanBase64)))
                    }
                }
            }
            if (msg.textContent.isNotBlank()) {
                parts.add(Part(text = msg.textContent))
            }
            if (parts.isEmpty()) {
                null
            } else {
                Content(
                    role = if (msg.role == MessageRole.USER) "user" else "model",
                    parts = parts
                )
            }
        }

        val safetySettings = listOf(
            SafetySetting("HARM_CATEGORY_HATE_SPEECH", state.safetySettings.hateSpeech),
            SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", state.safetySettings.dangerousContent),
            SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", state.safetySettings.sexuallyExplicit),
            SafetySetting("HARM_CATEGORY_HARASSMENT", state.safetySettings.harassment)
        )

        val tools = if (state.googleSearchEnabled) {
            listOf(Tool(googleSearch = GoogleSearchTool()))
        } else null

        val thinkingConfig = if (state.thinkingLevel != "OFF") {
            ThinkingConfig(thinkingLevel = state.thinkingLevel)
        } else null

        val aspectRatioVal = if (state.aspectRatio == "auto" || state.aspectRatio.isBlank()) null else state.aspectRatio
        val imageSizeVal = if (state.imageSize.isBlank()) null else state.imageSize
        val mimeTypeVal = if (state.outputMimeType.isBlank()) null else state.outputMimeType
        val personGenVal = if (state.personGeneration.isBlank()) null else state.personGeneration

        val imageConfig = if (aspectRatioVal == null && imageSizeVal == null && mimeTypeVal == null && personGenVal == null) {
            null
        } else {
            ImageConfig(
                aspectRatio = aspectRatioVal,
                imageSize = imageSizeVal,
                imageOutputOptions = if (mimeTypeVal != null) ImageOutputOptions(mimeType = mimeTypeVal) else null,
                personGeneration = personGenVal
            )
        }

        val generationConfig = GenerationConfig(
            temperature = state.temperature,
            maxOutputTokens = state.maxOutputTokens,
            responseModalities = state.responseModalities.ifEmpty { listOf("TEXT", "IMAGE") },
            topP = state.topP,
            imageConfig = imageConfig,
            thinkingConfig = thinkingConfig
        )

        return GenerateContentRequest(
            contents = contents,
            generationConfig = generationConfig,
            safetySettings = safetySettings,
            tools = tools
        )
    }
}
