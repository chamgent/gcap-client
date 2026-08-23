package com.gcap.client

import com.gcap.client.data.model.OpenAiChatRequest
import com.gcap.client.data.model.OpenAiMessage
import com.gcap.client.data.model.OpenAiStreamChunk
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Test
import java.util.concurrent.TimeUnit

class RealOpenAiStreamTest {

    private val openAiJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        isLenient = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Test
    fun testRealStreamingRequest() {
        val apiKey = "sk-VfUQ1ZZdtHbYDMPXLdLmFtPUud2qnh9PbcmbP04JXQg51aRNSDVlDsZ6CNiZTvFm"
        val baseUrl = "https://opencode.ai/zen/go/v1"
        val requestUrl = "$baseUrl/chat/completions"

        val request = OpenAiChatRequest(
            model = "deepseek-v4-flash",
            messages = listOf(
                OpenAiMessage(role = "user", content = JsonPrimitive("Hello! Please reply in 1 sentence."))
            ),
            stream = true,
            temperature = 1.0f,
            topP = 0.95f,
            maxTokens = 65535
        )

        val requestBodyString = openAiJson.encodeToString(request)
        println("Sending request body: $requestBodyString")

        val httpRequest = Request.Builder()
            .url(requestUrl)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .post(requestBodyString.toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(httpRequest).execute()
        println("Response code: ${response.code}")
        val source = response.body?.source()
        if (source == null) {
            println("Body is null")
            return
        }

        var linesRead = 0
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            linesRead++
            if (line.startsWith("data:")) {
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") {
                    println("Received [DONE]")
                    break
                }
                try {
                    val chunk = openAiJson.decodeFromString<OpenAiStreamChunk>(data)
                    val delta = chunk.choices?.firstOrNull()?.delta
                    println("Parsed delta: content='${delta?.content}', reasoning='${delta?.reasoningContent ?: delta?.reasoning}', usage='${chunk.usage}'")
                } catch (e: Exception) {
                    println("Error parsing data: '$data' - $e")
                }
            } else {
                println("Non-data line: $line")
            }
        }
        println("Total lines read: $linesRead")
    }
}
