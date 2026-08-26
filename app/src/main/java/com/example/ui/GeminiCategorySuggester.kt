package com.example.ui

import com.example.BuildConfig
import com.example.data.Content
import com.example.data.GenerateContentRequest
import com.example.data.Part
import com.example.data.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiCategorySuggester {
    suspend fun suggestCategory(note: String, availableCategories: List<String>, userApiKey: String = ""): String? = withContext(Dispatchers.IO) {
        if (note.isBlank() || availableCategories.isEmpty()) return@withContext null
        
        val apiKey = userApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return@withContext null

        val prompt = "Data una spesa con descrizione: \"$note\", suggerisci la categoria migliore scegliendola SOLTANTO tra questa lista esatta: ${availableCategories.joinToString(", ")}. Rispondi solo con il nome della categoria, nient'altro."
        
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val suggested = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (suggested in availableCategories) suggested else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
