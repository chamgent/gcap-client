package com.gcap.client.data.network

import android.util.Log
import com.gcap.client.data.model.StreamResponse
import kotlinx.serialization.json.Json
import okio.BufferedSource

object SseStreamParser {
    private const val TAG = "SseStreamParser"

    fun parse(source: BufferedSource, json: Json, onEvent: (StreamResponse) -> Unit) {
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            val trimmedLine = line.trim()
            if (trimmedLine.isBlank()) continue

            if (trimmedLine.startsWith("data:", ignoreCase = true)) {
                val data = trimmedLine.substring(5).trim()
                if (data == "[DONE]" || data.isEmpty()) {
                    continue
                }
                try {
                    val streamResponse = json.decodeFromString<StreamResponse>(data)
                    onEvent(streamResponse)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse SSE line data: $data", e)
                }
            } else if (trimmedLine.startsWith("{") && trimmedLine.endsWith("}")) {
                try {
                    val streamResponse = json.decodeFromString<StreamResponse>(trimmedLine)
                    onEvent(streamResponse)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse direct JSON object: $trimmedLine", e)
                }
            } else if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                try {
                    val list = json.decodeFromString<List<StreamResponse>>(trimmedLine)
                    list.forEach { onEvent(it) }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse direct JSON array: $trimmedLine", e)
                }
            }
        }
    }
}
