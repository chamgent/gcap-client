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
                  "object": "model"
                },
                {
                  "id": "deepseek-reasoner",
                  "object": "model"
                },
                {
                  "id": "deepseek-r1",
                  "object": "model"
                },
                {
                  "id": "gpt-4o",
                  "object": "model"
                },
                {
                  "id": "o3-mini",
                  "object": "model"
                },
                {
                  "id": "claude-3-7-sonnet",
                  "object": "model"
                },
                {
                  "id": "gemini-2.5-flash",
                  "object": "model"
                },
                {
                  "id": "qwq-32b",
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
        assertEquals(9, models!!.size)

        val modelMap = models.associateBy { it.id }

        // deepseek-chat (General chat)
        assertFalse(OpenAiApiService.REASONING_REGEX.containsMatchIn(modelMap["deepseek-chat"]!!.id))

        // deepseek-reasoner & deepseek-r1 (Reasoning)
        assertTrue(OpenAiApiService.REASONING_REGEX.containsMatchIn(modelMap["deepseek-reasoner"]!!.id))
        assertTrue(OpenAiApiService.REASONING_REGEX.containsMatchIn(modelMap["deepseek-r1"]!!.id))

        // gpt-4o (Vision)
        assertTrue(OpenAiApiService.VISION_REGEX.containsMatchIn(modelMap["gpt-4o"]!!.id))

        // o3-mini (Reasoning)
        assertTrue(OpenAiApiService.REASONING_REGEX.containsMatchIn(modelMap["o3-mini"]!!.id))

        // claude-3-7-sonnet (Both Vision & Hybrid Reasoning)
        assertTrue(OpenAiApiService.REASONING_REGEX.containsMatchIn(modelMap["claude-3-7-sonnet"]!!.id))
        assertTrue(OpenAiApiService.VISION_REGEX.containsMatchIn(modelMap["claude-3-7-sonnet"]!!.id))

        // gemini-2.5-flash (Both Vision & Reasoning)
        assertTrue(OpenAiApiService.REASONING_REGEX.containsMatchIn(modelMap["gemini-2.5-flash"]!!.id))
        assertTrue(OpenAiApiService.VISION_REGEX.containsMatchIn(modelMap["gemini-2.5-flash"]!!.id))

        // qwq-32b (Reasoning)
        assertTrue(OpenAiApiService.REASONING_REGEX.containsMatchIn(modelMap["qwq-32b"]!!.id))

        // qwen-vl-max (Vision)
        assertTrue(OpenAiApiService.VISION_REGEX.containsMatchIn(modelMap["qwen-vl-max"]!!.id))
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
            topP = 0.95f,
            reasoningEffort = "high"
        )

        val jsonString = openAiJson.encodeToString(request)
        println("OpenAI Request JSON: $jsonString")
        assertTrue(jsonString.contains("\"model\":\"deepseek-reasoner\""))
        assertTrue(jsonString.contains("\"stream\":true"))
        assertTrue(jsonString.contains("\"temperature\":0.7"))
        assertTrue(jsonString.contains("\"reasoning_effort\":\"high\""))
    }
}
