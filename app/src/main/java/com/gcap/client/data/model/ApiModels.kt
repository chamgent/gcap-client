package com.gcap.client.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// ========== Request Models ==========

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: SystemInstruction? = null,
    val generationConfig: GenerationConfig? = null,
    val safetySettings: List<SafetySetting>? = null,
    val tools: List<Tool>? = null,
    val toolConfig: ToolConfig? = null
)

@Serializable
data class Content(
    val role: String,
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null,
    val thought: Boolean? = null,
    val functionCall: FunctionCall? = null,
    val functionResponse: FunctionResponse? = null
)

@Serializable
data class FunctionCall(
    val name: String? = null,
    val args: JsonObject? = null
)

@Serializable
data class FunctionResponse(
    val name: String? = null,
    val response: JsonObject? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class SystemInstruction(
    val parts: List<SystemPart>
)

@Serializable
data class SystemPart(
    val text: String
)

@Serializable
data class GenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null,
    val topP: Float? = null,
    val thinkingConfig: ThinkingConfig? = null,
    val responseModalities: List<String>? = null,
    val imageConfig: ImageConfig? = null
)

@Serializable
data class ThinkingConfig(
    val thinkingLevel: String  // "NONE", "MINIMAL", "MEDIUM", "HIGH"
)

@Serializable
data class SafetySetting(
    val category: String,
    val threshold: String
)

@Serializable
data class Tool(
    val googleSearch: GoogleSearchTool? = null,
    val googleMaps: GoogleMapsTool? = null
)

@Serializable
class GoogleSearchTool  // empty object {}

@Serializable
class GoogleMapsTool  // empty object {}

@Serializable
data class ToolConfig(
    val retrievalConfig: RetrievalConfig? = null
)

@Serializable
data class RetrievalConfig(
    val languageCode: String? = null
)

@Serializable
data class ImageConfig(
    val aspectRatio: String? = null,
    val imageSize: String? = null,
    val imageOutputOptions: ImageOutputOptions? = null,
    val personGeneration: String? = null
)

@Serializable
data class ImageOutputOptions(
    val mimeType: String? = null
)

// ========== Response Models ==========

@Serializable
data class StreamResponse(
    val candidates: List<Candidate>? = null,
    val usageMetadata: UsageMetadata? = null,
    val modelVersion: String? = null
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null,
    val index: Int? = null,
    val safetyRatings: List<SafetyRating>? = null,
    val groundingMetadata: GroundingMetadata? = null
)

@Serializable
data class GroundingMetadata(
    val webSearchQueries: List<String>? = null,
    val searchEntryPoint: SearchEntryPoint? = null,
    val groundingChunks: List<GroundingChunk>? = null,
    val groundingSupports: List<GroundingSupport>? = null
)

@Serializable
data class SearchEntryPoint(
    val renderedContent: String? = null
)

@Serializable
data class GroundingChunk(
    val web: GroundingWebChunk? = null
)

@Serializable
data class GroundingWebChunk(
    val uri: String? = null,
    val title: String? = null
)

@Serializable
data class GroundingSupport(
    val groundIndices: List<Int>? = null,
    val confidenceScores: List<Float>? = null
)

@Serializable
data class SafetyRating(
    val category: String? = null,
    val probability: String? = null,
    val blocked: Boolean? = null
)

@Serializable
data class UsageMetadata(
    val promptTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val totalTokenCount: Int? = null,
    val thoughtsTokenCount: Int? = null
)
