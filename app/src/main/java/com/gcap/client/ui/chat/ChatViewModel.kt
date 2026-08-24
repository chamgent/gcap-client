package com.gcap.client.ui.chat

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
import com.gcap.client.data.model.GoogleMapsTool
import com.gcap.client.data.model.GoogleSearchTool
import com.gcap.client.data.model.InlineData
import com.gcap.client.data.model.MessageImage
import com.gcap.client.data.model.MessageRole
import com.gcap.client.data.model.ModelCategory
import com.gcap.client.data.model.ModelDefinition
import com.gcap.client.data.model.ModelRegistry
import com.gcap.client.data.model.ModelRequestFormat
import com.gcap.client.data.model.OpenAiChatRequest
import com.gcap.client.data.model.OpenAiMessage
import com.gcap.client.data.model.Part
import com.gcap.client.data.model.RetrievalConfig
import com.gcap.client.data.model.SafetySetting
import com.gcap.client.data.model.StreamResponse
import com.gcap.client.data.model.SystemInstruction
import com.gcap.client.data.model.SystemPart
import com.gcap.client.data.model.ThinkingConfig
import com.gcap.client.data.model.Tool
import com.gcap.client.data.model.ToolConfig
import com.gcap.client.data.repository.GeminiRepository
import com.gcap.client.ui.components.SafetySettingsState
import com.gcap.client.ui.components.formatToolCalls
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val conversationId: String = UUID.randomUUID().toString(),
    val messages: List<ChatMessage> = emptyList(),
    val selectedModel: ModelDefinition = ModelRegistry.chatModels.first(),
    val systemInstruction: String = "",
    val maxOutputTokens: Int = 65535,
    val temperature: Float = 1.0f,
    val topP: Float = 0.95f,
    val thinkingLevel: String = "MEDIUM",
    val safetySettings: SafetySettingsState = SafetySettingsState(),
    val googleSearchEnabled: Boolean = true,
    val googleMapsEnabled: Boolean = true,
    val toolConfigLanguageCode: String = "zh_CN",
    val isGenerating: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: GeminiRepository,
    private val settingsDataStore: SettingsDataStore,
    private val imageStorageManager: ImageStorageManager,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = repository.getConversationsByCategory("CHAT")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableModels: StateFlow<List<ModelDefinition>> = combine(
        repository.getAllProviders(),
        repository.getAllCustomModels()
    ) { providers, customModels ->
        val providerMap = providers.associateBy { it.id }
        val customModelDefs = customModels.mapNotNull { cm ->
            val provider = providerMap[cm.providerId] ?: return@mapNotNull null
            ModelDefinition(
                id = cm.id,
                displayName = cm.displayName,
                category = ModelCategory.CHAT,
                requestFormat = ModelRequestFormat.FORMAT_OPENAI,
                providerName = provider.name,
                providerId = provider.id,
                isCustom = true,
                supportsVision = cm.supportsVision,
                supportsReasoning = cm.supportsReasoning,
                supportsTemperature = true,
                supportsTopP = true,
                supportsSystemInstruction = true,
                supportsImageInput = cm.supportsVision,
                supportsGoogleSearch = false,
                supportsGoogleMaps = false,
                defaultMaxOutputTokens = 65535,
                defaultThinkingLevel = "MEDIUM"
            )
        }
        ModelRegistry.chatModels + customModelDefs
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModelRegistry.chatModels)

    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            val defaultModelId = settingsDataStore.getDefaultChatModel().first()
            val model = ModelRegistry.chatModels.find { it.id == defaultModelId } ?: ModelRegistry.chatModels.first()
            selectModel(model)
        }
    }

    fun selectModel(model: ModelDefinition) {
        _uiState.update {
            it.copy(
                selectedModel = model,
                maxOutputTokens = model.defaultMaxOutputTokens,
                thinkingLevel = model.defaultThinkingLevel
            )
        }
    }

    fun createNewConversation() {
        val newId = UUID.randomUUID().toString()
        _uiState.update {
            it.copy(
                conversationId = newId,
                messages = emptyList(),
                isGenerating = false,
                error = null
            )
        }
    }

    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            val conversation = repository.getConversationById(conversationId)
            val restoredSystemInstruction = conversation?.systemInstruction ?: ""
            val restoredModelId = conversation?.modelId
            val restoredModel = if (restoredModelId != null) {
                availableModels.value.find { it.id == restoredModelId }
                    ?: ModelRegistry.getModel(restoredModelId)
                    ?: _uiState.value.selectedModel
            } else _uiState.value.selectedModel

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
                        modelName = entity.modelName.ifBlank {
                            restoredModel.displayName
                        },
                        timestamp = entity.timestamp,
                        isStreaming = false
                    )
                }
                _uiState.update {
                    it.copy(
                        conversationId = conversationId,
                        messages = loadedMessages,
                        selectedModel = restoredModel,
                        systemInstruction = restoredSystemInstruction,
                        isGenerating = false,
                        error = null
                    )
                }
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
            if (_uiState.value.conversationId == conversationId) {
                createNewConversation()
            }
        }
    }

    fun updateSystemInstruction(instruction: String) {
        _uiState.update { it.copy(systemInstruction = instruction) }
        viewModelScope.launch {
            val currentId = _uiState.value.conversationId
            val conv = repository.getConversationById(currentId)
            if (conv != null) {
                repository.insertConversation(conv.copy(systemInstruction = instruction, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun updateMaxOutputTokens(tokens: Int) {
        _uiState.update { it.copy(maxOutputTokens = tokens) }
    }

    fun updateTemperature(temp: Float) {
        _uiState.update { it.copy(temperature = temp) }
    }

    fun updateTopP(p: Float) {
        _uiState.update { it.copy(topP = p) }
    }

    fun updateThinkingLevel(level: String) {
        _uiState.update { it.copy(thinkingLevel = level) }
    }

    fun updateSafetySettings(settings: SafetySettingsState) {
        _uiState.update { it.copy(safetySettings = settings) }
    }

    fun updateGoogleSearch(enabled: Boolean) {
        _uiState.update { it.copy(googleSearchEnabled = enabled) }
    }

    fun updateGoogleMaps(enabled: Boolean) {
        _uiState.update { it.copy(googleMapsEnabled = enabled) }
    }

    fun updateToolConfigLanguage(code: String) {
        _uiState.update { it.copy(toolConfigLanguageCode = code) }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _uiState.update { state ->
            val messages = state.messages.map { msg ->
                if (msg.isStreaming) msg.copy(isStreaming = false) else msg
            }
            state.copy(messages = messages, isGenerating = false)
        }
    }

    fun sendMessage(text: String, images: List<MessageImage> = emptyList()) {
        if (text.isBlank() && images.isEmpty()) return
        val currentState = _uiState.value
        if (currentState.isGenerating) return

        generationJob = viewModelScope.launch {
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
                modelName = currentState.selectedModel.displayName,
                timestamp = System.currentTimeMillis(),
                isStreaming = true
            )

            val updatedMessages = currentState.messages + userMessage + initialModelMessage
            _uiState.update {
                it.copy(
                    messages = updatedMessages,
                    isGenerating = true,
                    error = null
                )
            }

            // Persist conversation and user message to Room
            repository.insertConversation(
                ConversationEntity(
                    id = currentState.conversationId,
                    title = if (currentState.messages.isEmpty()) text.take(30).ifEmpty { "对话" } else "对话",
                    modelId = currentState.selectedModel.id,
                    modelCategory = "CHAT",
                    systemInstruction = currentState.systemInstruction,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )

            repository.insertMessage(
                MessageEntity(
                    id = userMessage.id,
                    conversationId = currentState.conversationId,
                    role = "USER",
                    textContent = userMessage.textContent,
                    imagesJson = if (images.isNotEmpty()) json.encodeToString(images) else "",
                    thinkingContent = "",
                    toolCallContent = "",
                    timestamp = userMessage.timestamp,
                    orderIndex = currentState.messages.size
                )
            )

            try {
                executeStreaming(currentState, modelMessageId, currentState.messages + userMessage)
            } catch (e: Exception) {
                _uiState.update { state ->
                    val errorMessages = state.messages.map { msg ->
                        if (msg.id == modelMessageId) {
                            msg.copy(isStreaming = false, error = e.message ?: "请求失败")
                        } else msg
                    }
                    state.copy(messages = errorMessages, isGenerating = false, error = e.message ?: "请求失败")
                }
            }
        }
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
        val userMsgIndex = currentState.messages.indexOfFirst { it.id == messageId }
        if (userMsgIndex == -1) return

        val targetUserMsg = currentState.messages[userMsgIndex].copy(textContent = newText)
        val truncatedHistory = currentState.messages.take(userMsgIndex) + targetUserMsg

        val modelMessageId = UUID.randomUUID().toString()
        val initialModelMessage = ChatMessage(
            id = modelMessageId,
            role = MessageRole.MODEL,
            textContent = "",
            thinkingContent = "",
            modelName = currentState.selectedModel.displayName,
            timestamp = System.currentTimeMillis(),
            isStreaming = true
        )

        val newMessages = truncatedHistory + initialModelMessage
        _uiState.update {
            it.copy(
                messages = newMessages,
                isGenerating = true,
                error = null
            )
        }

        viewModelScope.launch {
            repository.deleteMessagesAfterOrder(currentState.conversationId, userMsgIndex)
            repository.updateMessageText(messageId, newText)
        }

        generationJob = viewModelScope.launch {
            try {
                executeStreaming(currentState, modelMessageId, truncatedHistory)
            } catch (e: Exception) {
                _uiState.update { state ->
                    val errorMessages = state.messages.map { msg ->
                        if (msg.id == modelMessageId) {
                            msg.copy(isStreaming = false, error = e.message ?: "请求失败")
                        } else msg
                    }
                    state.copy(messages = errorMessages, isGenerating = false, error = e.message ?: "请求失败")
                }
            }
        }
    }

    private suspend fun executeStreaming(
        currentState: ChatUiState,
        modelMessageId: String,
        historyMessages: List<ChatMessage>
    ) {
        var accumulatedText = ""
        var accumulatedThought = ""
        var accumulatedToolCall = ""
        var promptTokens: Int? = null
        var candidatesTokens: Int? = null
        val searchQueries = mutableListOf<String>()
        val groundingSources = mutableListOf<Pair<String, String>>()
        val functionCalls = mutableListOf<String>()
        val accumulatedImages = mutableListOf<MessageImage>()

        val streamFlow: Flow<StreamResponse> = if (currentState.selectedModel.requestFormat == ModelRequestFormat.FORMAT_OPENAI) {
            val providerId = currentState.selectedModel.providerId
            val provider = if (providerId != null) repository.getProviderById(providerId) else null
            if (provider == null) {
                _uiState.update { state ->
                    val errorMessages = state.messages.map { msg ->
                        if (msg.id == modelMessageId) msg.copy(isStreaming = false, error = "未找到服务商配置") else msg
                    }
                    state.copy(messages = errorMessages, isGenerating = false, error = "未找到服务商配置")
                }
                return
            }
            if (provider.apiKey.isBlank()) {
                _uiState.update { state ->
                    val errorMessages = state.messages.map { msg ->
                        if (msg.id == modelMessageId) msg.copy(isStreaming = false, error = "该服务商未配置 API Key") else msg
                    }
                    state.copy(messages = errorMessages, isGenerating = false, error = "该服务商未配置 API Key")
                }
                return
            }
            val rawModelId = if (currentState.selectedModel.id.startsWith("${provider.id}_")) {
                currentState.selectedModel.id.removePrefix("${provider.id}_")
            } else {
                currentState.selectedModel.id
            }
            val openAiRequest = buildOpenAiRequest(currentState, historyMessages, rawModelId)
            repository.streamOpenAiChat(provider.baseUrl, provider.apiKey, openAiRequest)
        } else {
            val apiKey = try {
                settingsDataStore.getApiKey().first()
            } catch (e: Exception) {
                ""
            }
            if (apiKey.isBlank()) {
                _uiState.update { state ->
                    val errorMessages = state.messages.map { msg ->
                        if (msg.id == modelMessageId) msg.copy(isStreaming = false, error = "请先在设置中输入 Google API Key") else msg
                    }
                    state.copy(messages = errorMessages, isGenerating = false, error = "请先在设置中输入 Google API Key")
                }
                return
            }
            val request = buildRequest(currentState, historyMessages)
            repository.streamGenerateContent(apiKey, currentState.selectedModel.id, request)
        }

        streamFlow.collect { response ->
            response.usageMetadata?.let { usage ->
                if (usage.promptTokenCount != null) promptTokens = usage.promptTokenCount
                if (usage.candidatesTokenCount != null) candidatesTokens = usage.candidatesTokenCount
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
                        accumulatedThought += chunk
                    }
                } else {
                    part.text?.let { chunk ->
                        accumulatedText += chunk
                    }
                }
                part.inlineData?.let { inline ->
                    if (inline.data.isNotBlank()) {
                        val fileUri = imageStorageManager.saveBase64Image(inline.data, inline.mimeType)
                        accumulatedImages.add(
                            MessageImage(
                                base64Data = inline.data,
                                mimeType = inline.mimeType.ifBlank { "image/png" },
                                uri = fileUri
                            )
                        )
                    }
                }
            }

            accumulatedToolCall = formatToolCalls(functionCalls, searchQueries, groundingSources)

            _uiState.update { state ->
                val currentList = state.messages.map { msg ->
                    if (msg.id == modelMessageId) {
                        msg.copy(
                            textContent = accumulatedText,
                            thinkingContent = accumulatedThought,
                            toolCallContent = accumulatedToolCall,
                            promptTokens = promptTokens,
                            candidatesTokens = candidatesTokens,
                            images = accumulatedImages.toList()
                        )
                    } else msg
                }
                state.copy(messages = currentList)
            }
        }

        // Streaming done
        _uiState.update { state ->
            val finalMessages = state.messages.map { msg ->
                if (msg.id == modelMessageId) {
                    msg.copy(isStreaming = false)
                } else msg
            }
            state.copy(messages = finalMessages, isGenerating = false)
        }

        val imagesForDb = accumulatedImages.map { img ->
            val localUri = img.uri ?: (img.base64Data?.let { imageStorageManager.saveBase64Image(it, img.mimeType) })
            MessageImage(
                uri = localUri,
                mimeType = img.mimeType
            )
        }

        repository.insertMessage(
            MessageEntity(
                id = modelMessageId,
                conversationId = currentState.conversationId,
                role = "MODEL",
                textContent = accumulatedText,
                imagesJson = if (imagesForDb.isNotEmpty()) json.encodeToString(imagesForDb) else "",
                thinkingContent = accumulatedThought,
                toolCallContent = accumulatedToolCall,
                modelName = currentState.selectedModel.displayName,
                timestamp = System.currentTimeMillis(),
                orderIndex = currentState.messages.size + 1
            )
        )

        repository.updateConversationTitle(
            conversationId = currentState.conversationId,
            title = (currentState.messages.firstOrNull()?.textContent ?: historyMessages.firstOrNull()?.textContent ?: "新对话").take(30).ifEmpty { "新对话" },
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun buildOpenAiRequest(
        state: ChatUiState,
        messages: List<ChatMessage>,
        rawModelId: String
    ): OpenAiChatRequest {
        val openAiMessages = mutableListOf<OpenAiMessage>()

        if (state.systemInstruction.isNotBlank()) {
            openAiMessages.add(
                OpenAiMessage(
                    role = "system",
                    content = JsonPrimitive(state.systemInstruction)
                )
            )
        }

        messages.forEach { msg ->
            val role = if (msg.role == MessageRole.USER) "user" else "assistant"
            if (msg.role == MessageRole.USER && msg.images.isNotEmpty() && state.selectedModel.supportsVision) {
                val parts = mutableListOf<kotlinx.serialization.json.JsonObject>()
                if (msg.textContent.isNotBlank()) {
                    parts.add(
                        buildJsonObject {
                            put("type", JsonPrimitive("text"))
                            put("text", JsonPrimitive(msg.textContent))
                        }
                    )
                }
                msg.images.forEach { img ->
                    val base64 = img.base64Data ?: img.uri?.let { imageStorageManager.getBase64FromUri(it) }
                    if (!base64.isNullOrBlank()) {
                        val cleanBase64 = base64.substringAfter("base64,").trim()
                        val isVideo = img.mimeType.startsWith("video/")
                        val dataUrl = "data:${img.mimeType};base64,$cleanBase64"
                        if (isVideo) {
                            parts.add(
                                buildJsonObject {
                                    put("type", JsonPrimitive("video_url"))
                                    put("video_url", buildJsonObject {
                                        put("url", JsonPrimitive(dataUrl))
                                    })
                                }
                            )
                        } else {
                            parts.add(
                                buildJsonObject {
                                    put("type", JsonPrimitive("image_url"))
                                    put("image_url", buildJsonObject {
                                        put("url", JsonPrimitive(dataUrl))
                                    })
                                }
                            )
                        }
                    }
                }
                openAiMessages.add(
                    OpenAiMessage(
                        role = role,
                        content = JsonArray(parts)
                    )
                )
            } else {
                openAiMessages.add(
                    OpenAiMessage(
                        role = role,
                        content = JsonPrimitive(msg.textContent)
                    )
                )
            }
        }

        val reasoningEffortVal = if (state.selectedModel.supportsReasoning && state.thinkingLevel != "OFF") {
            when (state.thinkingLevel) {
                "MINIMAL", "LOW" -> "low"
                "MEDIUM" -> "medium"
                "HIGH" -> "high"
                else -> null
            }
        } else null

        return OpenAiChatRequest(
            model = rawModelId,
            messages = openAiMessages,
            stream = true,
            temperature = if (state.selectedModel.supportsTemperature) state.temperature else null,
            topP = if (state.selectedModel.supportsTopP) state.topP else null,
            maxTokens = if (state.maxOutputTokens in 1..65535) state.maxOutputTokens else null,
            reasoningEffort = reasoningEffortVal
        )
    }

    private fun buildRequest(state: ChatUiState, messages: List<ChatMessage>): GenerateContentRequest {
        val contents = messages.map { msg ->
            val parts = mutableListOf<Part>()
            if (msg.textContent.isNotBlank()) {
                parts.add(Part(text = msg.textContent))
            }
            msg.images.forEach { img ->
                val base64 = img.base64Data ?: img.uri?.let { imageStorageManager.getBase64FromUri(it) }
                if (!base64.isNullOrBlank()) {
                    val cleanBase64 = base64.substringAfter("base64,").trim()
                    parts.add(
                        Part(
                            inlineData = InlineData(
                                mimeType = img.mimeType,
                                data = cleanBase64
                            )
                        )
                    )
                }
            }
            Content(
                role = if (msg.role == MessageRole.USER) "user" else "model",
                parts = parts
            )
        }

        val systemInstruction = if (state.selectedModel.supportsSystemInstruction && state.systemInstruction.isNotBlank()) {
            SystemInstruction(parts = listOf(SystemPart(text = state.systemInstruction)))
        } else null

        val thinkingConfigVal = if (state.thinkingLevel != "OFF") {
            ThinkingConfig(thinkingLevel = state.thinkingLevel)
        } else null

        val generationConfig = when (state.selectedModel.requestFormat) {
            ModelRequestFormat.FORMAT_ONE -> {
                GenerationConfig(
                    maxOutputTokens = state.maxOutputTokens,
                    thinkingConfig = thinkingConfigVal
                )
            }
            ModelRequestFormat.FORMAT_TWO -> {
                GenerationConfig(
                    temperature = state.temperature,
                    topP = state.topP,
                    maxOutputTokens = state.maxOutputTokens,
                    thinkingConfig = thinkingConfigVal
                )
            }
            ModelRequestFormat.FORMAT_IMAGE, ModelRequestFormat.FORMAT_OPENAI -> {
                GenerationConfig(
                    temperature = state.temperature,
                    topP = state.topP,
                    maxOutputTokens = state.maxOutputTokens
                )
            }
        }

        val safetySettings = listOf(
            SafetySetting("HARM_CATEGORY_HATE_SPEECH", state.safetySettings.hateSpeech),
            SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", state.safetySettings.dangerousContent),
            SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", state.safetySettings.sexuallyExplicit),
            SafetySetting("HARM_CATEGORY_HARASSMENT", state.safetySettings.harassment)
        )

        val tools = mutableListOf<Tool>()
        if (state.selectedModel.supportsGoogleSearch && state.googleSearchEnabled) {
            tools.add(Tool(googleSearch = GoogleSearchTool()))
        }
        if (state.selectedModel.supportsGoogleMaps && state.googleMapsEnabled) {
            tools.add(Tool(googleMaps = GoogleMapsTool()))
        }

        val toolConfig = if (tools.isNotEmpty() && state.toolConfigLanguageCode.isNotBlank()) {
            ToolConfig(retrievalConfig = RetrievalConfig(languageCode = state.toolConfigLanguageCode))
        } else null

        return GenerateContentRequest(
            contents = contents,
            systemInstruction = systemInstruction,
            generationConfig = generationConfig,
            safetySettings = safetySettings.ifEmpty { null },
            tools = tools.ifEmpty { null },
            toolConfig = toolConfig
        )
    }
}
