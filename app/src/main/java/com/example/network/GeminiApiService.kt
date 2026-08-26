package com.example.network

import com.example.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<JsonObject>? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class ResponseFormat(
    val text: ResponseFormatText? = null
)

@Serializable
data class ResponseFormatText(
    val mimeType: String,
    val schema: JsonObject? = null
)

@Serializable
data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val responseModalities: List<String>? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

data class GeminiAnalysisResult(
    val isTransaction: Boolean,
    val reason: String
)

suspend fun isRealTransactionViaGemini(appName: String, title: String, text: String): GeminiAnalysisResult = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
        return@withContext GeminiAnalysisResult(true, "Fallback: API Key non configurata")
    }

    val prompt = """
        You are a highly accurate financial assistant.
        Analyze the following Android notification and determine if it represents an ACTUAL financial transaction (e.g. a payment, a purchase, money sent/received, charge).
        Exclude generic financial news, promotional alerts, login alerts, stock market updates, and non-transactional messages.
        
        App Name: ${"$"}appName
        Title: ${"$"}title
        Text: ${"$"}text
        
        Respond with exactly this format:
        YES|Brief reason why it's a transaction
        OR
        NO|Brief reason why it's not a transaction
        
        Example 1:
        YES|Contiene un importo pagato tramite Apple Pay.
        Example 2:
        NO|È una news finanziaria sul mercato azionario.
    """.trimIndent()

    val request = GenerateContentRequest(
        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
        generationConfig = GenerationConfig(temperature = 0.0f)
    )

    try {
        val response = RetrofitClient.service.generateContent(apiKey, request)
        val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
        
        if (reply.contains("|")) {
            val parts = reply.split("|", limit = 2)
            val isTransaction = parts[0].trim().uppercase() == "YES"
            val reason = parts[1].trim()
            GeminiAnalysisResult(isTransaction, reason)
        } else {
            val isTransaction = reply.uppercase().contains("YES")
            GeminiAnalysisResult(isTransaction, "Motivazione non formattata correttamente: ${"$"}reply")
        }
    } catch (e: Exception) {
        GeminiAnalysisResult(true, "Fallback: Errore di rete/API (${"$"}{e.message})")
    }
}
