package com.gcap.client.data.network

import android.util.Log
import com.gcap.client.data.model.GenerateContentRequest
import com.gcap.client.data.model.StreamResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiApiService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    companion object {
        private const val TAG = "GeminiApiService"
        private const val BASE_URL = "https://aiplatform.googleapis.com"
        private const val GENERATE_CONTENT_PATH = "/v1/publishers/google/models/%s:streamGenerateContent"
    }

    fun streamGenerateContent(
        apiKey: String,
        modelId: String,
        request: GenerateContentRequest
    ): Flow<StreamResponse> = callbackFlow {
        val url = buildString {
            append(BASE_URL)
            append(GENERATE_CONTENT_PATH.format(modelId))
            append("?key=").append(apiKey)
            append("&alt=sse")
        }

        val requestBodyString = json.encodeToString(request)
        Log.d(TAG, "Request to $modelId: $requestBodyString")

        val requestBody = requestBodyString.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val call = okHttpClient.newCall(httpRequest)

        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Request failed", e)
                close(e)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "HTTP Error ${response.code}: $errorBody")
                    close(IOException("HTTP Error ${response.code}: $errorBody"))
                    return
                }

                val source: BufferedSource? = response.body?.source()
                if (source == null) {
                    close(IOException("Response body source is null"))
                    return
                }

                try {
                    SseStreamParser.parse(source, json) { streamResponse ->
                        trySend(streamResponse).isSuccess
                    }
                    close()
                } catch (e: Exception) {
                    Log.e(TAG, "Stream parse error", e)
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
