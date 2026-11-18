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
            temperature = settings.temperature,
            maxTokens = settings.maxTokens,
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

    companion object {
        val instance: ACompleteService
            get() = ApplicationManager.getApplication().getService(ACompleteService::class.java)
    }
}
