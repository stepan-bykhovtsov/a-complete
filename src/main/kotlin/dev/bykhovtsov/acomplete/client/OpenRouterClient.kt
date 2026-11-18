package dev.bykhovtsov.acomplete.client

import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import dev.bykhovtsov.acomplete.client.model.ApiError
import dev.bykhovtsov.acomplete.client.model.ChatChoice
import dev.bykhovtsov.acomplete.client.model.ChatCompletionRequest
import dev.bykhovtsov.acomplete.client.model.ChatCompletionResponse
import dev.bykhovtsov.acomplete.client.model.ChatMessage
import dev.bykhovtsov.acomplete.client.model.ConnectionTestResult
import dev.bykhovtsov.acomplete.client.model.ModelsListResponse
import dev.bykhovtsov.acomplete.client.model.ResponsesApiResponse
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class OpenRouterClient(
    private val connectTimeoutSeconds: Long = 10L
) {
    private val logger = Logger.getInstance(OpenRouterClient::class.java)
    private val gson = Gson()

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
        .build()

    /**
     * Executes a completion request against OpenRouter or an OpenAI-compatible endpoint.
     */
    fun sendCompletion(
        apiKey: String,
        model: String,
        baseUrl: String,
        systemInstruction: String,
        userPrompt: String,
        timeoutSeconds: Int,
        temperature: Double,
        maxTokens: Int,
        indicator: ProgressIndicator? = null
    ): String {
        val endpointUrl = resolveEndpointUrl(baseUrl)

        val requestPayload = ChatCompletionRequest(
            model = model,
            messages = listOf(
                ChatMessage(role = "system", content = systemInstruction),
                ChatMessage(role = "user", content = userPrompt)
            ),
            temperature = temperature,
            maxTokens = maxTokens
        )

        val requestJson = gson.toJson(requestPayload)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(endpointUrl))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://github.com/stepan-byhkovtsov/a-complete")
            .header("X-Title", "A-Complete")
            .timeout(Duration.ofSeconds(timeoutSeconds.toLong()))
            .POST(HttpRequest.BodyPublishers.ofString(requestJson))
            .build()

        val responseBody = executeWithIndicator(request, indicator)
        return parseResponseText(responseBody)
    }

    /**
     * Tests connectivity to the API using a minimal request.
     */
    fun testConnection(
        apiKey: String,
        model: String,
        baseUrl: String
    ): ConnectionTestResult {
        val startTime = System.currentTimeMillis()
        return try {
            val endpointUrl = resolveEndpointUrl(baseUrl)
            val requestPayload = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "user", content = "ping")
                ),
                maxTokens = 5,
                temperature = 0.0
            )

            val requestJson = gson.toJson(requestPayload)
            val request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://github.com/stepan-byhkovtsov/a-complete")
                .header("X-Title", "A-Complete")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val latency = System.currentTimeMillis() - startTime

            if (response.statusCode() in 200..299) {
                // Verify response can be parsed
                parseResponseText(response.body())
                ConnectionTestResult(isSuccess = true, latencyMs = latency)
            } else {
                val errorMsg = extractErrorMessage(response.body()) ?: "HTTP ${response.statusCode()}"
                ConnectionTestResult(isSuccess = false, latencyMs = latency, errorMessage = errorMsg)
            }
        } catch (ex: Exception) {
            val latency = System.currentTimeMillis() - startTime
            val message = ex.cause?.message ?: ex.message ?: "Unknown network error"
            ConnectionTestResult(isSuccess = false, latencyMs = latency, errorMessage = message)
        }
    }

    private fun executeWithIndicator(
        request: HttpRequest,
        indicator: ProgressIndicator?
    ): String {
        val future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())

        try {
            while (!future.isDone) {
                if (indicator != null && indicator.isCanceled) {
                    future.cancel(true)
                    throw ProcessCanceledException()
                }
                Thread.sleep(30)
            }

            val response = future.get()
            val statusCode = response.statusCode()
            val body = response.body()

            if (statusCode !in 200..299) {
                val errorDetails = extractErrorMessage(body)
                val msg = if (errorDetails != null) {
                    "HTTP $statusCode: $errorDetails"
                } else {
                    "HTTP $statusCode: ${body.take(300)}"
                }
                throw RuntimeException(msg)
            }

            return body
        } catch (pce: ProcessCanceledException) {
            future.cancel(true)
            throw pce
        } catch (ex: ExecutionException) {
            val cause = ex.cause ?: ex
            logger.warn("OpenRouter API request failed", cause)
            throw RuntimeException("Network request failed: ${cause.message}", cause)
        } catch (ex: InterruptedException) {
            future.cancel(true)
            Thread.currentThread().interrupt()
            throw ProcessCanceledException()
        }
    }

    /**
     * Resolves the base URL into a concrete endpoint URL.
     */
    fun resolveEndpointUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return when {
            trimmed.endsWith("/chat/completions") || trimmed.endsWith("/responses") -> trimmed
            else -> "$trimmed/chat/completions"
        }
    }

    /**
     * Parses the response body supporting both OpenAI / OpenRouter Chat Completions schema
     * and OpenRouter Responses API schema.
     */
    fun parseResponseText(responseBody: String): String {
        // First check for error in response JSON
        val errorMsg = extractErrorMessage(responseBody)
        if (errorMsg != null) {
            throw RuntimeException(errorMsg)
        }

        // Try ChatCompletionResponse
        try {
            val chatResponse = gson.fromJson(responseBody, ChatCompletionResponse::class.java)
            val choiceContent = chatResponse?.choices?.firstOrNull()?.message?.content
            if (!choiceContent.isNullOrBlank()) {
                return choiceContent
            }
        } catch (_: Exception) {
            // Not a standard chat completion response, fall through
        }

        // Try ResponsesApiResponse
        try {
            val responsesApi = gson.fromJson(responseBody, ResponsesApiResponse::class.java)
            val text = responsesApi?.output
                ?.filter { it.type == "message" }
                ?.flatMap { it.content.orEmpty() }
                ?.filter { it.type == "output_text" }
                ?.joinToString("\n") { it.text.orEmpty() }
                ?.takeIf { it.isNotBlank() }

            if (!text.isNullOrBlank()) {
                return text
            }
        } catch (_: Exception) {
            // Not a responses API format
        }

        throw RuntimeException("Unable to extract text from API response")
    }

    private fun extractErrorMessage(responseBody: String): String? {
        return try {
            val root = gson.fromJson(responseBody, Map::class.java)
            val errorObj = root["error"]
            if (errorObj is Map<*, *>) {
                errorObj["message"]?.toString()
            } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resolves the models URL from the base URL.
     */
    fun resolveModelsUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        val root = when {
            trimmed.endsWith("/chat/completions") -> trimmed.removeSuffix("/chat/completions")
            trimmed.endsWith("/responses") -> trimmed.removeSuffix("/responses")
            trimmed.endsWith("/models") -> return trimmed
            else -> trimmed
        }
        return "$root/models"
    }

    /**
     * Fetches the list of available model IDs from the endpoint.
     */
    fun fetchAvailableModels(baseUrl: String, apiKey: String? = null): List<String> {
        val url = resolveModelsUrl(baseUrl)
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .header("HTTP-Referer", "https://github.com/stepan-byhkovtsov/a-complete")
            .header("X-Title", "A-Complete")
            .timeout(Duration.ofSeconds(15))
            .GET()

        if (!apiKey.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            val errorMsg = extractErrorMessage(response.body()) ?: "HTTP ${response.statusCode()}"
            throw RuntimeException("Failed to fetch models: $errorMsg")
        }

        return parseModelsList(response.body())
    }

    /**
     * Parses the models JSON response into a sorted list of model IDs.
     */
    fun parseModelsList(jsonBody: String): List<String> {
        val listResponse = gson.fromJson(jsonBody, ModelsListResponse::class.java)
        val models = listResponse?.data?.mapNotNull { it.id.trim().takeIf { id -> id.isNotEmpty() } }
        if (models.isNullOrEmpty()) {
            throw RuntimeException("No models found in API response")
        }
        return models.distinct().sorted()
    }
}
