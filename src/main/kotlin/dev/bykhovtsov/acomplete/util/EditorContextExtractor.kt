package dev.bykhovtsov.acomplete.util

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange

data class EditorContext(
    val prompt: String,
    val baseIndentation: String,
    val insertionOffset: Int,
    val surroundingCode: String,
    val fileName: String,
    val fileExtension: String
)

object EditorContextExtractor {

    fun extractContext(editor: Editor, maxContextLines: Int = 250): EditorContext {
        val document = editor.document
        val selectionModel = editor.selectionModel

        val prompt: String
        val insertionOffset: Int
        val targetLineNumber: Int
        val baseIndentation: String

        if (selectionModel.hasSelection()) {
            prompt = selectionModel.selectedText?.trim().orEmpty()
            insertionOffset = selectionModel.selectionEnd
            targetLineNumber = document.getLineNumber(insertionOffset)
            val lineStart = document.getLineStartOffset(targetLineNumber)
            val lineEnd = document.getLineEndOffset(targetLineNumber)
            val lineText = document.getText(TextRange(lineStart, lineEnd))
            baseIndentation = lineText.takeWhile { it == ' ' || it == '\t' }
        } else {
            val caretOffset = editor.caretModel.offset
            targetLineNumber = document.getLineNumber(caretOffset)
            val lineStart = document.getLineStartOffset(targetLineNumber)
            val lineEnd = document.getLineEndOffset(targetLineNumber)
            val currentLineText = document.getText(TextRange(lineStart, lineEnd))
            prompt = currentLineText.trim()
            insertionOffset = lineEnd
            baseIndentation = currentLineText.takeWhile { it == ' ' || it == '\t' }
        }

        val totalLines = document.lineCount
        val surroundingCode = if (totalLines <= maxContextLines) {
            document.text
        } else {
            val linesBefore = (maxContextLines * 0.7).toInt()
            val startLine = (targetLineNumber - linesBefore).coerceAtLeast(0)
            val endLine = (startLine + maxContextLines).coerceAtMost(totalLines - 1)
            val startOffset = document.getLineStartOffset(startLine)
            val endOffset = document.getLineEndOffset(endLine)
            document.getText(TextRange(startOffset, endOffset))
        }

        val virtualFile = FileDocumentManager.getInstance().getFile(document)
        val fileName = virtualFile?.name ?: "SourceFile"
        val fileExtension = virtualFile?.extension ?: ""

        return EditorContext(
            prompt = prompt,
            baseIndentation = baseIndentation,
            insertionOffset = insertionOffset,
            surroundingCode = surroundingCode,
            fileName = fileName,
            fileExtension = fileExtension
        )
    }
}
