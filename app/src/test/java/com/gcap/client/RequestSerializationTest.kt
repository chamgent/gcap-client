package com.gcap.client

import com.gcap.client.data.model.Content
import com.gcap.client.data.model.GenerateContentRequest
import com.gcap.client.data.model.GenerationConfig
import com.gcap.client.data.model.GoogleSearchTool
import com.gcap.client.data.model.ImageConfig
import com.gcap.client.data.model.ImageOutputOptions
import com.gcap.client.data.model.Part
import com.gcap.client.data.model.SafetySetting
import com.gcap.client.data.model.ThinkingConfig
import com.gcap.client.data.model.Tool
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
        isLenient = true
    }

    @Test
    fun testImageRequestSerialization() {
        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(Part(text = "猫咪"))
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 1.0f,
                maxOutputTokens = 32768,
                responseModalities = listOf("TEXT", "IMAGE"),
                topP = 0.95f,
                imageConfig = ImageConfig(
                    aspectRatio = "auto",
                    imageSize = "1K",
                    imageOutputOptions = ImageOutputOptions(mimeType = "image/png"),
                    personGeneration = "ALLOW_ALL"
                ),
                thinkingConfig = ThinkingConfig(thinkingLevel = "MINIMAL")
            ),
            safetySettings = listOf(
                SafetySetting("HARM_CATEGORY_HATE_SPEECH", "OFF"),
                SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "OFF"),
                SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "OFF"),
                SafetySetting("HARM_CATEGORY_HARASSMENT", "OFF")
            ),
            tools = listOf(Tool(googleSearch = GoogleSearchTool()))
        )

        val jsonString = json.encodeToString(request)
        println("Generated Image Request JSON: $jsonString")

        // Must not contain any null properties
        assertFalse("Should not contain null systemInstruction", jsonString.contains("\"systemInstruction\":null"))
        assertFalse("Should not contain null toolConfig", jsonString.contains("\"toolConfig\":null"))
        assertFalse("Should not contain null googleMaps", jsonString.contains("\"googleMaps\":null"))
        assertFalse("Should not contain null thought", jsonString.contains("\"thought\":null"))
        assertFalse("Should not contain null inlineData", jsonString.contains("\"inlineData\":null"))

        // Must contain required image fields
        assertTrue(jsonString.contains("\"responseModalities\":[\"TEXT\",\"IMAGE\"]"))
        assertTrue(jsonString.contains("\"aspectRatio\":\"auto\""))
        assertTrue(jsonString.contains("\"imageSize\":\"1K\""))
        assertTrue(jsonString.contains("\"mimeType\":\"image/png\""))
        assertTrue(jsonString.contains("\"personGeneration\":\"ALLOW_ALL\""))
        assertTrue(jsonString.contains("\"googleSearch\":{}"))
    }
}
