package com.gcap.client.data.network

import android.util.Log
import com.gcap.client.data.local.CustomModelEntity
import com.gcap.client.data.model.Candidate
import com.gcap.client.data.model.Content
import com.gcap.client.data.model.OpenAiChatRequest
import com.gcap.client.data.model.OpenAiModelListResponse
import com.gcap.client.data.model.OpenAiStreamChunk
import com.gcap.client.data.model.Part
import com.gcap.client.data.model.StreamResponse
import com.gcap.client.data.model.UsageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSource
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Singleton
class OpenAiApiService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    private val openAiJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        isLenient = true
    }

    companion object {
        private const val TAG = "OpenAiApiService"

        val VISION_REGEX = Regex(
            "(?i)(vision|4o|4.5|claude|gemini|vl|omni|llava|qwen-vl|minicpm|pixtral|internvl|multimodal|grok-2-vision|image|qvq|glm-4v|sonnet|opus|haiku|step-2|yi-vision)"
        )
        val REASONING_REGEX = Regex(
            "(?i)(r1|o1|o3|o4|reason|reasoner|reasoning|thinking|think|qwq|claude-3-7|claude-3.7|sonnet-3-7|sonnet-3.7|gemini-2.5|gemini-2.0-flash-thinking|k1.5|kimi-k1.5|marco-o1|qvq|deepseek-r1|deepseek-reasoner)"
        )

        fun normalizeBaseUrl(rawUrl: String): String {
            var url = rawUrl.trim().trimEnd('/')
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            if (!url.endsWith("/v1") && !url.contains("/v1/")) {
                url = "$url/v1"
            }
            return url.trimEnd('/')
        }
    }

    suspend fun probeModels(
        providerId: String,
        baseUrl: String,
        apiKey: String
    ): Result<List<CustomModelEntity>> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeBaseUrl(baseUrl)
            val requestUrl = "$normalizedUrl/models"

            val httpRequest = Request.Builder()
                .url(requestUrl)
                .header("Authorization", "Bearer ${apiKey.trim()}")
                .get()
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                return@withContext Result.failure(IOException("HTTP ${response.code}: $errorBody"))
            }

            val bodyString = response.body?.string() ?: ""
            val parsed = openAiJson.decodeFromString<OpenAiModelListResponse>(bodyString)
            val rawModels = parsed.data ?: emptyList()

            val entities = rawModels.map { modelItem ->
                val modelId = modelItem.id
                val supportsVision = VISION_REGEX.containsMatchIn(modelId)
                val supportsReasoning = REASONING_REGEX.containsMatchIn(modelId)

                CustomModelEntity(
                    id = "${providerId}_${modelId}",
                    providerId = providerId,
                    modelId = modelId,
                    displayName = modelId,
                    supportsVision = supportsVision,
                    supportsReasoning = supportsReasoning
                )
            }.sortedBy { it.modelId }

            Result.success(entities)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to probe models", e)
            Result.failure(e)
        }
    }

    fun streamChatCompletion(
        baseUrl: String,
        apiKey: String,
        request: OpenAiChatRequest
    ): Flow<StreamResponse> = callbackFlow {
        val normalizedUrl = normalizeBaseUrl(baseUrl)
        val requestUrl = "$normalizedUrl/chat/completions"

        val requestBodyString = openAiJson.encodeToString(request)
        Log.d(TAG, "OpenAI Request to $requestUrl: $requestBodyString")

        val requestBody = requestBodyString.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(requestUrl)
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("Accept", "text/event-stream")
            .post(requestBody)
            .build()

        val call = okHttpClient.newCall(httpRequest)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "OpenAI Stream failed", e)
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "OpenAI HTTP Error ${response.code}: $errorBody")
                    close(IOException("HTTP Error ${response.code}: $errorBody"))
                    return
                }

                val source: BufferedSource? = response.body?.source()
                if (source == null) {
                    close(IOException("Response body is null"))
                    return
                }

                var insideThinkTag = false

                try {
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.isBlank() || line.startsWith(":")) continue

                        if (line.startsWith("data:")) {
                            val data = line.removePrefix("data:").trim()
                            if (data == "[DONE]") {
                                break
                            }

                            try {
                                val chunk = openAiJson.decodeFromString<OpenAiStreamChunk>(data)
                                val choice = chunk.choices?.firstOrNull()
                                val delta = choice?.delta

                                val reasoningText = delta?.reasoningContent ?: delta?.reasoning
                                val contentText = delta?.content

                                // 1. Server returned delta.reasoning_content (e.g. DeepSeek / OpenRouter standard)
                                if (!reasoningText.isNullOrEmpty()) {
                                    val streamResp = StreamResponse(
                                        candidates = listOf(
                                            Candidate(
                                                content = Content(
                                                    role = "model",
                                                    parts = listOf(Part(text = reasoningText, thought = true))
                                                )
                                            )
                                        )
                                    )
                                    trySend(streamResp).isSuccess
                                }

                                // 2. Server returned delta.content with potential <think>...</think> tags
                                if (!contentText.isNullOrEmpty()) {
                                    if (insideThinkTag) {
                                        if (contentText.contains("</think>")) {
                                            val thoughtPart = contentText.substringBefore("</think>")
                                            val regularPart = contentText.substringAfter("</think>")
                                            insideThinkTag = false

                                            if (thoughtPart.isNotEmpty()) {
                                                trySend(
                                                    StreamResponse(
                                                        candidates = listOf(
                                                            Candidate(
                                                                content = Content(
                                                                    role = "model",
                                                                    parts = listOf(Part(text = thoughtPart, thought = true))
                                                                )
                                                            )
                                                        )
                                                    )
                                                ).isSuccess
                                            }
                                            if (regularPart.isNotEmpty()) {
                                                trySend(
                                                    StreamResponse(
                                                        candidates = listOf(
                                                            Candidate(
                                                                content = Content(
                                                                    role = "model",
                                                                    parts = listOf(Part(text = regularPart, thought = null))
                                                                )
                                                            )
                                                        )
                                                    )
                                                ).isSuccess
                                            }
                                        } else {
                                            trySend(
                                                StreamResponse(
                                                    candidates = listOf(
                                                        Candidate(
                                                            content = Content(
                                                                role = "model",
                                                                parts = listOf(Part(text = contentText, thought = true))
                                                            )
                                                        )
                                                    )
                                                )
                                            ).isSuccess
                                        }
                                    } else {
                                        if (contentText.contains("<think>")) {
                                            val beforeThink = contentText.substringBefore("<think>")
                                            val afterStart = contentText.substringAfter("<think>")
                                            if (beforeThink.isNotEmpty()) {
                                                trySend(
                                                    StreamResponse(
                                                        candidates = listOf(
                                                            Candidate(
                                                                content = Content(
                                                                    role = "model",
                                                                    parts = listOf(Part(text = beforeThink, thought = null))
                                                                )
                                                            )
                                                        )
                                                    )
                                                ).isSuccess
                                            }

                                            if (afterStart.contains("</think>")) {
                                                val thoughtPart = afterStart.substringBefore("</think>")
                                                val afterEnd = afterStart.substringAfter("</think>")
                                                insideThinkTag = false
                                                if (thoughtPart.isNotEmpty()) {
                                                    trySend(
                                                        StreamResponse(
                                                            candidates = listOf(
                                                                Candidate(
                                                                    content = Content(
                                                                        role = "model",
                                                                        parts = listOf(Part(text = thoughtPart, thought = true))
                                                                    )
                                                                )
                                                            )
                                                        )
                                                    ).isSuccess
                                                }
                                                if (afterEnd.isNotEmpty()) {
                                                    trySend(
                                                        StreamResponse(
                                                            candidates = listOf(
                                                                Candidate(
                                                                    content = Content(
                                                                        role = "model",
                                                                        parts = listOf(Part(text = afterEnd, thought = null))
                                                                    )
                                                                )
                                                            )
                                                        )
                                                    ).isSuccess
                                                }
                                            } else {
                                                insideThinkTag = true
                                                if (afterStart.isNotEmpty()) {
                                                    trySend(
                                                        StreamResponse(
                                                            candidates = listOf(
                                                                Candidate(
                                                                    content = Content(
                                                                        role = "model",
                                                                        parts = listOf(Part(text = afterStart, thought = true))
                                                                    )
                                                                )
                                                            )
                                                        )
                                                    ).isSuccess
                                                }
                                            }
                                        } else {
                                            trySend(
                                                StreamResponse(
                                                    candidates = listOf(
                                                        Candidate(
                                                            content = Content(
                                                                role = "model",
                                                                parts = listOf(Part(text = contentText, thought = null))
                                                            )
                                                        )
                                                    )
                                                )
                                            ).isSuccess
                                        }
                                    }
                                }

                                if (chunk.usage != null) {
                                    val streamResp = StreamResponse(
                                        usageMetadata = UsageMetadata(
                                            promptTokenCount = chunk.usage.promptTokens,
                                            candidatesTokenCount = chunk.usage.completionTokens,
                                            totalTokenCount = chunk.usage.totalTokens
                                        )
                                    )
                                    trySend(streamResp).isSuccess
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse chunk line: $data", e)
                            }
                        }
                    }
                    close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error while reading stream", e)
                    close(e)
                } finally {
                    response.close()
                }
            }
        })

        awaitClose {
            call.cancel()
        }
    }.flowOn(Dispatchers.IO)
}
