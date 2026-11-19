package com.iamcomia.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class GeminiClient {
    private val http = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun reply(apiKey: String, history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Informe sua chave da API Gemini nas configurações." }
        val contents = JSONArray().apply {
            history.takeLast(20).forEach { message ->
                if (message.role == MessageRole.USER || message.role == MessageRole.ASSISTANT) {
                    put(JSONObject()
                        .put("role", if (message.role == MessageRole.USER) "user" else "model")
                        .put("parts", JSONArray().put(JSONObject().put("text", message.text))))
                }
            }
        }
        val body = JSONObject().put("contents", contents)
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        http.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching {
                    JSONObject(responseText).optJSONObject("error")?.optString("message")
                }.getOrNull().orEmpty()
                throw IOException(detail.ifBlank { "Falha na API Gemini (HTTP ${response.code}). Confira a chave e tente novamente." })
            }
            val candidates = JSONObject(responseText).optJSONArray("candidates")
                ?: throw IOException("A IA não retornou uma resposta. Tente novamente.")
            val parts = candidates.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                ?: throw IOException("A resposta da IA veio vazia.")
            (0 until parts.length()).mapNotNull { index ->
                parts.optJSONObject(index)?.optString("text")?.takeIf(String::isNotBlank)
            }.joinToString("\n").ifBlank { throw IOException("A resposta da IA veio vazia.") }
        }
    }
}
