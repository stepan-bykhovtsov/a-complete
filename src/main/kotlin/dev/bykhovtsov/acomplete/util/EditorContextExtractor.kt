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

    fun extractContext(editor: Editor): EditorContext {
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
