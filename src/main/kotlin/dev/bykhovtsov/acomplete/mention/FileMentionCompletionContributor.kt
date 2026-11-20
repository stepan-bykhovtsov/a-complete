package dev.bykhovtsov.acomplete.mention

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

class FileMentionCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.completionType != CompletionType.BASIC) return

        val editor = parameters.editor
        val project = editor.project ?: return
        val document = editor.document
        val offset = parameters.offset

        val lineNumber = document.getLineNumber(offset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineTextBeforeCaret = document.getText(TextRange(lineStartOffset, offset))

        if (!isInComment(parameters.position, lineTextBeforeCaret)) {
            return
        }

        val atIndexInLine = lineTextBeforeCaret.lastIndexOf('@')
        if (atIndexInLine == -1) return

        val textAfterAt = lineTextBeforeCaret.substring(atIndexInLine + 1)
        if (textAfterAt.any { it.isWhitespace() }) return

        val atAbsoluteOffset = lineStartOffset + atIndexInLine

        val pathPrefix = if (textAfterAt.startsWith("file:")) {
            textAfterAt.removePrefix("file:")
        } else {
            textAfterAt
        }

        result.restartCompletionOnAnyPrefixChange()
        val customResult = result.withPrefixMatcher(pathPrefix)

        val projectFiles = ProjectFileUtils.listProjectFiles(project)
        if (projectFiles.isEmpty()) return

        val insertHandler = InsertHandler<LookupElement> { context, item ->
            val doc = context.document
            val lookupStr = item.lookupString
            val formattedPath = if (lookupStr.contains(' ')) "\"$lookupStr\"" else lookupStr
            val replacement = "@file:$formattedPath"

            val currentTail = context.tailOffset
            val safeStart = atAbsoluteOffset.coerceIn(0, doc.textLength)
            val safeEnd = currentTail.coerceIn(safeStart, doc.textLength)

            if (safeStart <= safeEnd) {
                doc.replaceString(safeStart, safeEnd, replacement)
                context.editor.caretModel.moveToOffset(safeStart + replacement.length)
            }
        }

        for (fileInfo in projectFiles) {
            val vf = fileInfo.virtualFile
            val relativePath = fileInfo.relativePath
            val fileName = vf.name

            val builder = LookupElementBuilder.create(relativePath)
                .withLookupString(fileName)
                .withPresentableText(fileName)
                .withTailText(" ($relativePath)", true)
                .withTypeText(vf.fileType.name, true)
                .withIcon(vf.fileType.icon)
                .withInsertHandler(insertHandler)

            customResult.addElement(builder)
        }
    }

    private fun isInComment(position: PsiElement, lineText: String): Boolean {
        if (PsiTreeUtil.getParentOfType(position, PsiComment::class.java, false) != null) {
            return true
        }

        var curr: PsiElement? = position
        while (curr != null && curr !is PsiFile) {
            val typeName = curr.node?.elementType?.toString()?.uppercase().orEmpty()
            if (typeName.contains("COMMENT")) return true
            curr = curr.parent
        }

        val trimmed = lineText.trimStart()
        return trimmed.startsWith("//") ||
                trimmed.startsWith("/*") ||
                trimmed.startsWith("*") ||
                trimmed.startsWith("#") ||
                trimmed.startsWith("--") ||
                trimmed.startsWith(";") ||
                trimmed.startsWith("%")
    }
}
