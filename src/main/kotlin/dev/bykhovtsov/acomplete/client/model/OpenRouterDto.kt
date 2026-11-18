package dev.bykhovtsov.acomplete.client.model

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double? = null,
    @SerializedName("max_tokens")
    val maxTokens: Int? = null,
    val stream: Boolean = false
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatCompletionResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<ChatChoice>? = null,
    val error: ApiError? = null
)

data class ChatChoice(
    val index: Int? = null,
    val message: ChatMessage? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class ResponsesRequest(
    val model: String,
    val input: String,
    val instructions: String? = null,
    @SerializedName("max_output_tokens")
    val maxOutputTokens: Int? = null,
    val temperature: Double? = null
)

data class ResponsesApiResponse(
    val id: String? = null,
    val model: String? = null,
    val output: List<OutputItem>? = null,
    val error: ApiError? = null
)

data class OutputItem(
    val type: String? = null,
    val role: String? = null,
    val content: List<ContentItem>? = null
)

data class ContentItem(
    val type: String? = null,
    val text: String? = null
)

data class ApiError(
    val message: String? = null,
    val code: Any? = null,
    val type: String? = null
)

data class ConnectionTestResult(
    val isSuccess: Boolean,
    val latencyMs: Long = 0,
    val errorMessage: String? = null
)

data class ModelsListResponse(
    val data: List<ModelItem>? = null
)

data class ModelItem(
    val id: String,
    val name: String? = null
)

