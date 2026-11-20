package dev.bykhovtsov.acomplete.mention

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

class FileMentionTypedHandler : TypedHandlerDelegate() {

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (c == '@') {
            val offset = editor.caretModel.offset
            if (offset > 0) {
                val element = file.findElementAt((offset - 1).coerceAtLeast(0))
                val isCommentElement = element != null && (
                    PsiTreeUtil.getParentOfType(element, PsiComment::class.java, false) != null ||
                    element.node?.elementType?.toString()?.uppercase()?.contains("COMMENT") == true
                )

                val doc = editor.document
                val lineStart = doc.getLineStartOffset(doc.getLineNumber(offset))
                val lineBeforeCaret = doc.getText(TextRange(lineStart, offset)).trimStart()
                val isCommentLine = lineBeforeCaret.startsWith("//") ||
                    lineBeforeCaret.startsWith("/*") ||
                    lineBeforeCaret.startsWith("*") ||
                    lineBeforeCaret.startsWith("#") ||
                    lineBeforeCaret.startsWith("--") ||
                    lineBeforeCaret.startsWith(";")

                if (isCommentElement || isCommentLine) {
                    AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
                }
            }
        }
        return Result.CONTINUE
    }
}
