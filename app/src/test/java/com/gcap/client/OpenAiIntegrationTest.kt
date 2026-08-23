package com.gcap.client

import com.gcap.client.data.model.OpenAiChatRequest
import com.gcap.client.data.model.OpenAiMessage
import com.gcap.client.data.model.OpenAiModelListResponse
import com.gcap.client.data.model.OpenAiStreamChunk
import com.gcap.client.data.network.OpenAiApiService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiIntegrationTest {

    private val openAiJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        isLenient = true
    }

    @Test
    fun testNormalizeBaseUrl() {
        assertEquals("https://api.deepseek.com/v1", OpenAiApiService.normalizeBaseUrl("api.deepseek.com"))
        assertEquals("https://api.deepseek.com/v1", OpenAiApiService.normalizeBaseUrl("https://api.deepseek.com"))
        assertEquals("https://api.deepseek.com/v1", OpenAiApiService.normalizeBaseUrl("https://api.deepseek.com/"))
        assertEquals("https://api.deepseek.com/v1", OpenAiApiService.normalizeBaseUrl("https://api.deepseek.com/v1"))
        assertEquals("https://api.deepseek.com/v1", OpenAiApiService.normalizeBaseUrl("https://api.deepseek.com/v1/"))
        assertEquals("https://openrouter.ai/api/v1", OpenAiApiService.normalizeBaseUrl("https://openrouter.ai/api/v1"))
    }

    @Test
    fun testModelCapabilityDetectionForNextGenModels() {
        // 1. Grok 4.5
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("grok-4.5"))

        // 2. GLM 5 series
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("glm-5.3"))
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("glm-5.2"))
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("glm-5.1"))
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("glm-5"))

        // 3. GPT 5.6 Luna
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("gpt-5.6-luna"))

        // 4. Kimi K-series
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("kimi-k3"))
        assertEquals(Pair(false, true), OpenAiApiService.detectModelCapabilities("kimi-k2.7-code"))
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("kimi-k2.6"))
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("kimi-k2.5"))

        // 5. MiMo series
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("mimo-v2.5"))
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("mimo-v2.5-pro"))
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("mimo-v2-omni"))
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("mimo-v2-pro"))

        // 6. MiniMax series
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("minimax-m3"))
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("minimax-m2.7"))
        assertEquals(Pair(false, false), OpenAiApiService.detectModelCapabilities("minimax-m2.5"))

        // 7. Qwen3 series
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("qwen3.8-max"))
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("qwen3.7-max"))
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("qwen3.7-plus"))
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("qwen3.6-plus"))
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("qwen3.5-plus"))

        // 8. DeepSeek series
        assertEquals(Pair(false, true), OpenAiApiService.detectModelCapabilities("deepseek-v4-pro"))
        assertEquals(Pair(false, true), OpenAiApiService.detectModelCapabilities("deepseek-v4-flash"))
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("deepseek-v4-flash-vision-exp"))

        // 9. Hy3 series
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("hy3"))
        assertEquals(Pair(true, true), OpenAiApiService.detectModelCapabilities("hy3-preview"))
    }

    @Test
    fun testParseStreamChunkWithReasoning() {
        val sampleChunk = """
            {
              "id": "chatcmpl-999",
              "choices": [
                {
                  "index": 0,
                  "delta": {
                    "role": "assistant",
                    "reasoning_content": "正在思考如何解题...",
                    "content": null
                  },
                  "finish_reason": null
                }
              ]
            }
        """.trimIndent()

        val chunk = openAiJson.decodeFromString<OpenAiStreamChunk>(sampleChunk)
        val delta = chunk.choices?.firstOrNull()?.delta
        assertNotNull(delta)
        assertEquals("正在思考如何解题...", delta?.reasoningContent)
    }

    @Test
    fun testSerializeChatRequestWithVideoUrl() {
        val videoPart = buildJsonObject {
            put("type", JsonPrimitive("video_url"))
            put("video_url", buildJsonObject {
                put("url", JsonPrimitive("data:video/mp4;base64,AAAAHGZ0eXBtcDQy..."))
            })
        }
        val textPart = buildJsonObject {
            put("type", JsonPrimitive("text"))
            put("text", JsonPrimitive("请分析视频内容"))
        }

        val request = OpenAiChatRequest(
            model = "mimo-v2-omni",
            messages = listOf(
                OpenAiMessage(
                    role = "user",
                    content = JsonArray(listOf(textPart, videoPart))
                )
            ),
            stream = true,
            temperature = 0.7f,
            topP = 0.95f,
            reasoningEffort = "high"
        )

        val jsonString = openAiJson.encodeToString(request)
        println("OpenAI Video Request JSON: $jsonString")
        assertTrue(jsonString.contains("\"model\":\"mimo-v2-omni\""))
        assertTrue(jsonString.contains("\"type\":\"video_url\""))
        assertTrue(jsonString.contains("\"video_url\":{\"url\":\"data:video/mp4;base64,AAAAHGZ0eXBtcDQy...\"}"))
        assertTrue(jsonString.contains("\"reasoning_effort\":\"high\""))
    }
}
