package dev.bykhovtsov.acomplete

import com.google.gson.Gson
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.checkCanceled
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class GenerateCodeAction : AnAction() {

    private val gson = Gson()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    // --- Модели для JSON (запрос и ответ OpenRouter Responses API) ---
    private data class OpenRouterRequest(
        val model: String,
        val input: String
    )

    private data class OpenRouterResponse(
        val output: List<OutputItem>?,
        val error: ApiError?
    )

    private data class OutputItem(
        val type: String?,
        val content: List<ContentItem>?
    )

    private data class ContentItem(
        val type: String?,
        val text: String?
    )

    private data class ApiError(
        val message: String?
    )
    // -----------------------------------------------------------------

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = (project != null && editor != null)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val caretModel = editor.caretModel

        // 1. Читаем контекст в EDT
        val caretOffset = caretModel.offset
        val lineNumber = document.getLineNumber(caretOffset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)

        val currentLineText = document.getText(TextRange(lineStart, lineEnd))
        val fullFileText = document.text

        val settings = ACompleteSettings.instance.state
        if (settings.apiKey.isBlank()) {
            insertGeneratedCode(project, document, lineEnd, "\n// [A-Complete Error]: Укажите API ключ в Settings -> Tools -> A-Complete")
            return
        }

***REMOVED***
***REMOVED***

        // 2. Фоновая задача
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "A-Complete: Generating code...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.checkCanceled()

                    val prompt = """
                        |Complete code. Output only raw code without markdown backticks. Ensure to match tabs. Your code will be placed after the prompt line. (Do not output full file, only the insertion)
                        |
                        |File:
                        |```
                        |$fullFileText
                        |```
                        |
                        |Prompt:
                        |```
                        |$currentLineText
                        |```
                    """.trimMargin()

                    // Сериализуем тело запроса в JSON (Gson сам экранирует кавычки и переносы строк)
                    val requestJson = gson.toJson(OpenRouterRequest(model = settings.model, input = prompt))

                    val request = HttpRequest.newBuilder()
                        .uri(URI.create("https://openrouter.ai/api/v1/responses"))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", "Bearer ${settings.apiKey}") // Исправлены фигурные скобки!
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                        .build()

                    // Отправляем запрос
                    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

                    indicator.checkCanceled()

                    if (response.statusCode() !in 200..299) {
                        throw RuntimeException("HTTP ${response.statusCode()}: ${response.body()}")
                    }

                    // Парсим JSON ответа
                    val parsedResponse = gson.fromJson(response.body(), OpenRouterResponse::class.java)

                    // Если API вернул ошибку внутри JSON
                    if (parsedResponse.error != null) {
                        throw RuntimeException(parsedResponse.error.message ?: "Unknown API error")
                    }

                    // Достаем текст из output -> type=="message" -> content -> type=="output_text"
                    val rawText = parsedResponse.output
                        ?.filter { it.type == "message" }
                        ?.flatMap { it.content.orEmpty() }
                        ?.filter { it.type == "output_text" }
                        ?.joinToString("\n") { it.text.orEmpty() }
                        ?.takeIf { it.isNotBlank() }
                        ?: throw RuntimeException("Пустой ответ от модели")

                    // На всякий случай очищаем от ```kotlin ... ```, если нейросеть все равно их добавила
                    val cleanCode = stripMarkdownCodeBlock(rawText)

                    // 3. Вставляем сгенерированный код с новой строки
                    insertGeneratedCode(project, document, lineEnd, "\n$cleanCode")

                } catch (ex: Exception) {
                    insertGeneratedCode(project, document, lineEnd, "\n// [A-Complete Error]: ${ex.message}")
                }
            }
        })
    }

    /**
     * Убирает обертку ```язык ... ``` если модель проигнорировала инструкцию
     */
    private fun stripMarkdownCodeBlock(text: String): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            return trimmed
                .substringAfter("\n") // убираем первую строку с ```kotlin
                .substringBeforeLast("```") // убираем закрывающие ```
                .trimEnd()
        }
        return text
    }

    private fun insertGeneratedCode(
        project: Project,
        document: Document,
        offset: Int,
        codeToInsert: String
    ) {
        WriteCommandAction.runWriteCommandAction(project, "A-Complete Code Generation", null, {
            val safeOffset = offset.coerceAtMost(document.textLength)
            document.insertString(safeOffset, codeToInsert)
        })
    }
}