package com.example.cornell.api

import com.example.cornell.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GptRepository {

    private const val ENDPOINT = "https://models.inference.ai.azure.com/chat/completions"
    private const val MODEL = "gpt-4o"

    // El usuario puede sobreescribir el token desde Ajustes
    // Si está vacío, usa el que viene de GitHub Actions (BuildConfig)
    var githubToken: String = ""

    private fun resolvedToken(): String =
        if (githubToken.isNotBlank()) githubToken else BuildConfig.GITHUB_TOKEN

    suspend fun generateText(prompt: String): String? = withContext(Dispatchers.IO) {
        chat(listOf(Pair("user", prompt)))
    }

    suspend fun chat(messages: List<Pair<String, String>>): String? = withContext(Dispatchers.IO) {
        val token = resolvedToken()
        if (token.isBlank()) return@withContext "⚠️ Token no configurado"
        try {
            val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 60000
            }
            val msgs = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "Eres un asistente educativo experto en el Método Cornell. Responde siempre en español.")
                })
                messages.forEach { (role, content) ->
                    put(JSONObject().apply {
                        put("role", role)
                        put("content", content)
                    })
                }
            }
            val body = JSONObject().apply {
                put("model", MODEL)
                put("messages", msgs)
                put("max_tokens", 1500)
                put("temperature", 0.7)
            }.toString()
            OutputStreamWriter(conn.outputStream).use { it.write(body) }
            val code = conn.responseCode
            val resp = (if (code == 200) conn.inputStream else conn.errorStream)
                .bufferedReader().readText()
            if (code == 200) {
                JSONObject(resp)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            } else "⚠️ Error $code"
        } catch (e: Exception) {
            "⚠️ Error: ${e.message}"
        }
    }
}
