package com.gcap.client.data.model

enum class ModelCategory { CHAT, IMAGE }
enum class ModelRequestFormat { FORMAT_ONE, FORMAT_TWO, FORMAT_IMAGE, FORMAT_OPENAI }

data class ModelDefinition(
    val id: String,
    val displayName: String,
    val category: ModelCategory,
    val requestFormat: ModelRequestFormat,
    val providerName: String = "Google Vertex AI",
    val providerId: String? = null,
    val isCustom: Boolean = false,
    val supportsVision: Boolean = true,
    val supportsReasoning: Boolean = true,
    val supportsTemperature: Boolean = false,
    val supportsTopP: Boolean = false,
    val supportsSystemInstruction: Boolean = true,
    val supportsImageInput: Boolean = true,
    val supportsGoogleSearch: Boolean = true,
    val supportsGoogleMaps: Boolean = true,
    val supportsImageConfig: Boolean = false,
    val defaultMaxOutputTokens: Int = 65535,
    val defaultThinkingLevel: String = "MEDIUM"
)

object ModelRegistry {
    // Format 1 models: no temperature, no topP, has systemInstruction, has image input, has googleMaps
    private val format1Models = listOf(
        ModelDefinition(
            id = "gemini-3.7-flash",
            displayName = "Gemini 3.7 Flash",
            category = ModelCategory.CHAT,
            requestFormat = ModelRequestFormat.FORMAT_ONE,
            providerName = "Google Vertex AI",
            supportsVision = true,
            supportsReasoning = true,
            supportsTemperature = false,
            supportsTopP = false,
            supportsSystemInstruction = true,
            supportsImageInput = true,
            supportsGoogleSearch = true,
            supportsGoogleMaps = true,
            supportsImageConfig = false,
            defaultMaxOutputTokens = 65535,
            defaultThinkingLevel = "MEDIUM"
        ),
        ModelDefinition(
            id = "gemini-3.6-flash",
            displayName = "Gemini 3.6 Flash",
            category = ModelCategory.CHAT,
            requestFormat = ModelRequestFormat.FORMAT_ONE,
            providerName = "Google Vertex AI",
            supportsVision = true,
            supportsReasoning = false,
            supportsTemperature = false,
            supportsTopP = false,
            supportsSystemInstruction = true,
            supportsImageInput = true,
            supportsGoogleSearch = true,
            supportsGoogleMaps = true,
            supportsImageConfig = false,
            defaultMaxOutputTokens = 65535,
            defaultThinkingLevel = "MEDIUM"
        ),
        ModelDefinition(
            id = "gemini-3.5-flash-lite",
            displayName = "Gemini 3.5 Flash Lite",
            category = ModelCategory.CHAT,
            requestFormat = ModelRequestFormat.FORMAT_ONE,
            providerName = "Google Vertex AI",
            supportsVision = true,
            supportsReasoning = false,
            supportsTemperature = false,
            supportsTopP = false,
            supportsSystemInstruction = true,
            supportsImageInput = true,
            supportsGoogleSearch = true,
            supportsGoogleMaps = true,
            supportsImageConfig = false,
            defaultMaxOutputTokens = 65535,
            defaultThinkingLevel = "MEDIUM"
        )
    )

    // Format 2 models: has temperature, has topP, no systemInstruction by default, has googleMaps
    private val format2Models = listOf(
        ModelDefinition(
            id = "gemini-3.5-flash",
            displayName = "Gemini 3.5 Flash",
            category = ModelCategory.CHAT,
            requestFormat = ModelRequestFormat.FORMAT_TWO,
            providerName = "Google Vertex AI",
            supportsVision = true,
            supportsReasoning = false,
            supportsTemperature = true,
            supportsTopP = true,
            supportsSystemInstruction = true,
            supportsImageInput = true,
            supportsGoogleSearch = true,
            supportsGoogleMaps = true,
            supportsImageConfig = false,
            defaultMaxOutputTokens = 65535,
            defaultThinkingLevel = "HIGH"
        ),
        ModelDefinition(
            id = "gemini-3.1-flash-lite",
            displayName = "Gemini 3.1 Flash Lite",
            category = ModelCategory.CHAT,
            requestFormat = ModelRequestFormat.FORMAT_TWO,
            providerName = "Google Vertex AI",
            supportsVision = true,
            supportsReasoning = false,
            supportsTemperature = true,
            supportsTopP = true,
            supportsSystemInstruction = true,
            supportsImageInput = true,
            supportsGoogleSearch = true,
            supportsGoogleMaps = true,
            supportsImageConfig = false,
            defaultMaxOutputTokens = 65535,
            defaultThinkingLevel = "HIGH"
        ),
        ModelDefinition(
            id = "gemini-3.1-pro-preview",
            displayName = "Gemini 3.1 Pro Preview",
            category = ModelCategory.CHAT,
            requestFormat = ModelRequestFormat.FORMAT_TWO,
            providerName = "Google Vertex AI",
            supportsVision = true,
            supportsReasoning = true,
            supportsTemperature = true,
            supportsTopP = true,
            supportsSystemInstruction = true,
            supportsImageInput = true,
            supportsGoogleSearch = true,
            supportsGoogleMaps = true,
            supportsImageConfig = false,
            defaultMaxOutputTokens = 65535,
            defaultThinkingLevel = "HIGH"
        )
    )

    // Image model: has temperature, topP, imageConfig, responseModalities, no googleMaps
    private val imageModels = listOf(
        ModelDefinition(
            id = "gemini-3.1-flash-image",
            displayName = "Gemini 3.1 Flash Image (Nano Banana 2)",
            category = ModelCategory.IMAGE,
            requestFormat = ModelRequestFormat.FORMAT_IMAGE,
            providerName = "Google Vertex AI",
            supportsVision = false,
            supportsReasoning = false,
            supportsTemperature = true,
            supportsTopP = true,
            supportsSystemInstruction = false,
            supportsImageInput = false,
            supportsGoogleSearch = true,
            supportsGoogleMaps = false,
            supportsImageConfig = true,
            defaultMaxOutputTokens = 32768,
            defaultThinkingLevel = "MINIMAL"
        )
    )

    val chatModels: List<ModelDefinition> = format1Models + format2Models
    val imageModels_list: List<ModelDefinition> = imageModels
    val allModels: List<ModelDefinition> = chatModels + imageModels

    fun getModel(id: String): ModelDefinition? = allModels.find { it.id == id }
}
