package dev.bykhovtsov.acomplete.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import dev.bykhovtsov.acomplete.client.OpenRouterClient
import dev.bykhovtsov.acomplete.client.model.ConnectionTestResult
import dev.bykhovtsov.acomplete.prompt.PromptBuilder
import dev.bykhovtsov.acomplete.settings.ACompleteCredentials
import dev.bykhovtsov.acomplete.settings.ACompleteSettings
import dev.bykhovtsov.acomplete.util.ACompleteBundle
import dev.bykhovtsov.acomplete.util.CodeCleaningUtils
import dev.bykhovtsov.acomplete.util.EditorContext

@Service(Service.Level.APP)
class ACompleteService {

    private val client = OpenRouterClient()

    fun generateCode(context: EditorContext, indicator: ProgressIndicator? = null): String {
        val apiKey = ACompleteCredentials.getApiKey()
        if (apiKey.isNullOrBlank()) {
            throw IllegalStateException(ACompleteBundle.message("action.error.missing.key"))
        }

        if (context.prompt.isBlank()) {
            throw IllegalArgumentException(ACompleteBundle.message("action.error.empty.prompt"))
        }

        val settings = ACompleteSettings.instance.state
        val systemInstruction = PromptBuilder.SYSTEM_INSTRUCTION
        val userPrompt = PromptBuilder.buildUserPrompt(context)

        val rawGeneratedCode = client.sendCompletion(
            apiKey = apiKey,
            model = settings.model,
            baseUrl = settings.apiBaseUrl,
            systemInstruction = systemInstruction,
            userPrompt = userPrompt,
            timeoutSeconds = settings.timeoutSeconds,
            indicator = indicator
        )

        val cleanedCode = CodeCleaningUtils.cleanGeneratedCode(rawGeneratedCode)
        if (cleanedCode.isBlank()) {
            throw IllegalStateException(ACompleteBundle.message("action.error.empty.response"))
        }

        return CodeCleaningUtils.applyIndentation(cleanedCode, context.baseIndentation)
    }

    fun testConnection(apiKey: String, model: String, baseUrl: String): ConnectionTestResult {
        return client.testConnection(
            apiKey = apiKey,
            model = model,
            baseUrl = baseUrl
        )
    }

    /**
     * Fetches fresh models from the endpoint and updates cache.
     */
    fun fetchModels(baseUrl: String = ACompleteSettings.instance.state.apiBaseUrl, apiKey: String? = null): List<String> {
        val key = apiKey ?: ACompleteCredentials.getApiKey()
        val models = client.fetchAvailableModels(baseUrl, key)
        if (models.isNotEmpty()) {
            ACompleteSettings.instance.state.cachedModels = models
        }
        return models
    }

    /**
     * Returns cached models if available, otherwise attempts to fetch from endpoint.
     */
    fun getAvailableModels(baseUrl: String = ACompleteSettings.instance.state.apiBaseUrl): List<String> {
        val cached = ACompleteSettings.instance.state.cachedModels
        if (cached.isNotEmpty()) {
            return cached
        }
        return try {
            fetchModels(baseUrl)
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        val instance: ACompleteService
            get() = ApplicationManager.getApplication().getService(ACompleteService::class.java)
    }
}
