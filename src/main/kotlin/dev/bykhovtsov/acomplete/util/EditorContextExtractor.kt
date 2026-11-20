package dev.bykhovtsov.acomplete.util

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import dev.bykhovtsov.acomplete.header.CHeaderResolver
import dev.bykhovtsov.acomplete.header.HeaderFileContext
import dev.bykhovtsov.acomplete.mention.FileMentionResolver
import dev.bykhovtsov.acomplete.mention.MentionedFile
import dev.bykhovtsov.acomplete.settings.ACompleteSettings

data class EditorContext(
    val prompt: String,
    val baseIndentation: String,
    val insertionOffset: Int,
    val surroundingCode: String,
    val fileName: String,
    val fileExtension: String,
    val includedHeaders: List<HeaderFileContext> = emptyList(),
    val mentionedFiles: List<MentionedFile> = emptyList()
)

object EditorContextExtractor {

    fun extractContext(editor: Editor, project: Project? = editor.project): EditorContext {
        val document = editor.document
        val selectionModel = editor.selectionModel

        val prompt: String
        val insertionOffset: Int
        val baseIndentation: String

        if (selectionModel.hasSelection()) {
            prompt = selectionModel.selectedText?.trim().orEmpty()
            insertionOffset = selectionModel.selectionEnd
            val targetLineNumber = document.getLineNumber(insertionOffset)
            val lineStart = document.getLineStartOffset(targetLineNumber)
            val lineEnd = document.getLineEndOffset(targetLineNumber)
            val lineText = document.getText(TextRange(lineStart, lineEnd))
            baseIndentation = lineText.takeWhile { it == ' ' || it == '\t' }
        } else {
            val caretOffset = editor.caretModel.offset
            val targetLineNumber = document.getLineNumber(caretOffset)
            val lineStart = document.getLineStartOffset(targetLineNumber)
            val lineEnd = document.getLineEndOffset(targetLineNumber)
            val currentLineText = document.getText(TextRange(lineStart, lineEnd))
            prompt = currentLineText.trim()
            insertionOffset = lineEnd
            baseIndentation = currentLineText.takeWhile { it == ' ' || it == '\t' }
        }

        val surroundingCode = document.text

        val virtualFile = FileDocumentManager.getInstance().getFile(document)
        val fileName = virtualFile?.name ?: "SourceFile"
        val fileExtension = virtualFile?.extension ?: ""

        val includedHeaders = if (project != null && CHeaderResolver.isCSourceOrHeader(fileExtension)) {
            val settings = ACompleteSettings.instance.state
            CHeaderResolver.resolveHeaders(
                project = project,
                currentFile = virtualFile,
                fileContent = surroundingCode,
                settings = settings
            )
        } else {
            emptyList()
        }

        val mentionedFiles = if (project != null && prompt.isNotBlank()) {
            FileMentionResolver.resolveMentions(prompt, project)
        } else {
            emptyList()
        }

        return EditorContext(
            prompt = prompt,
            baseIndentation = baseIndentation,
            insertionOffset = insertionOffset,
            surroundingCode = surroundingCode,
            fileName = fileName,
            fileExtension = fileExtension,
            includedHeaders = includedHeaders,
            mentionedFiles = mentionedFiles
        )
    }
}
