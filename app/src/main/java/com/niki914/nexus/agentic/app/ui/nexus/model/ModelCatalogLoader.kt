package com.niki914.nexus.agentic.app.ui.nexus.model

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

internal object ModelCatalogLoader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    fun load(endpoint: String, apiKey: String, providerId: String): List<String> {
        require(endpoint.isNotBlank()) { "Endpoint is required" }
        require(apiKey.isNotBlank()) { "API key is required" }

        val request = Request.Builder()
            .url(modelCatalogUrl(endpoint, providerId))
            .header(
                if (providerId == "google") "x-goog-api-key" else "Authorization",
                if (providerId == "google") apiKey else "Bearer $apiKey",
            )
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Model catalog request failed: HTTP ${response.code}")
            }
            return parseModelIds(response.body?.string().orEmpty())
        }
    }

    internal fun modelCatalogUrl(endpoint: String, providerId: String): String {
        val parsed = endpoint.trim().toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid endpoint URL")
        if (providerId == "google") {
            return parsed.newBuilder()
                .encodedPath("/v1beta/models")
                .query(null)
                .build()
                .toString()
        }

        val segments = parsed.pathSegments.filter { it.isNotBlank() }.toMutableList()
        val catalogStart = segments.indexOfFirst { it == "chat" || it == "messages" }
        if (catalogStart >= 0) segments.subList(catalogStart, segments.size).clear()
        if (segments.lastOrNull() != "models") segments.add("models")
        return parsed.newBuilder()
            .encodedPath("/${segments.joinToString("/")}")
            .query(null)
            .build()
            .toString()
    }

    internal fun parseModelIds(body: String): List<String> {
        val root = JSONObject(body)
        val models = root.optJSONArray("data") ?: root.optJSONArray("models") ?: JSONArray()
        return buildList {
            for (index in 0 until models.length()) {
                val model = models.optJSONObject(index) ?: continue
                val id = model.optString("id")
                    .ifBlank { model.optString("name") }
                    .removePrefix("models/")
                    .trim()
                if (id.isNotBlank()) add(id)
            }
        }.distinct().sorted()
    }
}