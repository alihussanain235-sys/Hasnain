package com.example.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GeminiResult {
    data class Success(val text: String) : GeminiResult()
    data class Error(val message: String) : GeminiResult()
}

class GeminiApiClient {

    companion object {
        private const val TAG = "GeminiApiClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getEffectiveApiKey(customKey: String): String {
        if (customKey.isNotBlank()) return customKey.trim()
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") return buildKey.trim()
        return ""
    }

    suspend fun generateResponse(
        prompt: String,
        customKey: String = ""
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(customKey)

        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Success(
                getLocalSmartResponse(prompt)
            )
        }

        // Try gemini-2.5-flash first, then gemini-1.5-flash fallback
        val models = listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-2.0-flash")

        for (model in models) {
            val url = "$BASE_URL/$model:generateContent?key=$apiKey"
            val requestBodyJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contents)

                val systemInstruction = JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put(
                                "text",
                                "You are Gemini Voice AI, an ultra-fast, intelligent, polite, and helpful Android voice assistant. " +
                                        "Keep spoken answers natural, engaging, and concise (about 2 to 4 sentences). " +
                                        "Avoid markdown symbols like asterisks or complex tables that sound awkward when read by a Text-To-Speech engine."
                            )
                        })
                    })
                }
                put("systemInstruction", systemInstruction)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 500)
                }
                put("generationConfig", genConfig)
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Content-Type", "application/json")
                .build()

            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val text = parts?.getJSONObject(0)?.optString("text").orEmpty()
                        if (text.isNotBlank()) {
                            return@withContext GeminiResult.Success(text.trim())
                        }
                    }
                } else {
                    Log.w(TAG, "Model $model returned error ${response.code}: $body")
                    if (response.code == 400 || response.code == 404) {
                        // try next model
                        continue
                    }
                    val errorMsg = try {
                        JSONObject(body).optJSONObject("error")?.optString("message") ?: "API Error ${response.code}"
                    } catch (e: Exception) {
                        "API Error ${response.code}"
                    }
                    return@withContext GeminiResult.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Request failed for model $model", e)
            }
        }

        // If network failed or models exhausted, return intelligent local fallback
        GeminiResult.Success(getLocalSmartResponse(prompt))
    }

    private fun getLocalSmartResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            "who are you" in lower || "what are you" in lower ->
                "I am Gemini Voice AI, your intelligent Android voice assistant. I can open apps like WhatsApp, YouTube, TikTok, and Easypaisa, read your unread notifications aloud, answer queries, and export your Python assistant script."

            "time" in lower ->
                "The current time is " + java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date()) + "."

            "date" in lower || "day" in lower ->
                "Today is " + java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date()) + "."

            "joke" in lower ->
                "Why do programmers prefer dark mode? Because light attracts bugs!"

            "how are you" in lower ->
                "I am doing fantastic, charged up and ready to assist you. What can I do for you today?"

            "help" in lower ->
                "You can say: 'Open WhatsApp', 'Launch YouTube', 'Open TikTok', 'Open Easypaisa', 'Read my notifications', or ask any general question!"

            else ->
                "I heard '$prompt'. To connect directly to the full cloud Gemini intelligence, you can configure your Gemini API Key in the Settings panel. You can also use voice commands to launch WhatsApp, YouTube, TikTok, Easypaisa, or read notifications!"
        }
    }
}
