package com.gcap.client

import com.gcap.client.data.model.OpenAiChatRequest
import com.gcap.client.data.model.OpenAiMessage
import com.gcap.client.data.model.OpenAiModelListResponse
import com.gcap.client.data.model.OpenAiStreamChunk
import com.gcap.client.data.network.OpenAiApiService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
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
    fun testParseModelListResponse() {
        val sampleJson = """
            {
              "object": "list",
              "data": [
                {
                  "id": "deepseek-chat",
                  "object": "model",
                  "created": 1700000000,
                  "owned_by": "deepseek"
                },
                {
                  "id": "deepseek-reasoner",
                  "object": "model",
                  "created": 1700000000,
                  "owned_by": "deepseek"
                },
                {
                  "id": "gpt-4o",
                  "object": "model"
                },
                {
                  "id": "qwen-vl-max",
                  "object": "model"
                }
              ]
            }
        """.trimIndent()

        val parsed = openAiJson.decodeFromString<OpenAiModelListResponse>(sampleJson)
        val models = parsed.data
        assertNotNull(models)
        assertEquals(4, models!!.size)

        val reasoningRegex = Regex("(?i)(r1|o1|o3|o4|reason|reasoner|reasoning|deepseek-r1|qwq|thinking)")
        val visionRegex = Regex("(?i)(vision|4o|4.5|claude|gemini|vl|omni|llava|qwen-vl|minicpm|pixtral|internvl|multimodal)")

        // deepseek-chat
        assertFalse(reasoningRegex.containsMatchIn(models[0].id))
        assertFalse(visionRegex.containsMatchIn(models[0].id))

        // deepseek-reasoner
        assertTrue(reasoningRegex.containsMatchIn(models[1].id))

        // gpt-4o
        assertTrue(visionRegex.containsMatchIn(models[2].id))

        // qwen-vl-max
        assertTrue(visionRegex.containsMatchIn(models[3].id))
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
    fun testSerializeChatRequest() {
        val request = OpenAiChatRequest(
            model = "deepseek-reasoner",
            messages = listOf(
                OpenAiMessage(
                    role = "user",
                    content = JsonPrimitive("Hello World")
                )
            ),
            stream = true,
            temperature = 0.7f,
            topP = 0.95f
        )

        val jsonString = openAiJson.encodeToString(request)
        println("OpenAI Request JSON: $jsonString")
        assertTrue(jsonString.contains("\"model\":\"deepseek-reasoner\""))
        assertTrue(jsonString.contains("\"stream\":true"))
        assertTrue(jsonString.contains("\"temperature\":0.7"))
    }
}
