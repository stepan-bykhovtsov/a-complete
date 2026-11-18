package dev.bykhovtsov.acomplete.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import dev.bykhovtsov.acomplete.notification.ACompleteNotifier
import dev.bykhovtsov.acomplete.service.ACompleteService
import dev.bykhovtsov.acomplete.settings.ACompleteCredentials
import dev.bykhovtsov.acomplete.settings.ACompleteSettings
import dev.bykhovtsov.acomplete.util.ACompleteBundle
import dev.bykhovtsov.acomplete.util.EditorContextExtractor

class GenerateCodeAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val isWritable = editor?.document?.isWritable == true
        e.presentation.isEnabledAndVisible = (project != null && editor != null && isWritable)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document

        if (!ACompleteCredentials.hasApiKey()) {
            ACompleteNotifier.notifyMissingApiKey(project)
            return
        }

        val settings = ACompleteSettings.instance.state
        val context = EditorContextExtractor.extractContext(editor, settings.contextLinesWindow)

        if (context.prompt.isBlank()) {
            ACompleteNotifier.notifyError(project, ACompleteBundle.message("action.error.empty.prompt"))
            return
        }

        val rangeMarker = document.createRangeMarker(context.insertionOffset, context.insertionOffset).apply {
            isGreedyToRight = true
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            ACompleteBundle.message("action.generating.title"),
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.checkCanceled()
                    val generatedCode = ACompleteService.instance.generateCode(context, indicator)
                    indicator.checkCanceled()

                    val textToInsert = if (generatedCode.startsWith("\n")) generatedCode else "\n$generatedCode"

                    ApplicationManager.getApplication().invokeLater({
                        try {
                            if (project.isDisposed || editor.isDisposed || !rangeMarker.isValid) {
                                return@invokeLater
                            }

                            WriteCommandAction.runWriteCommandAction(
                                project,
                                ACompleteBundle.message("action.command.name"),
                                "AComplete",
                                Runnable {
                                    if (editor.isDisposed || !rangeMarker.isValid) return@Runnable
                                    val safeOffset = rangeMarker.startOffset.coerceAtMost(document.textLength)
                                    document.insertString(safeOffset, textToInsert)
                                    val endOffset = (safeOffset + textToInsert.length).coerceAtMost(document.textLength)
                                    editor.caretModel.moveToOffset(endOffset)
                                    editor.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
                                }
                            )
                        } finally {
                            rangeMarker.dispose()
                        }
                    }, ModalityState.defaultModalityState())

                } catch (_: ProcessCanceledException) {
                    rangeMarker.dispose()
                } catch (ex: Exception) {
                    rangeMarker.dispose()
                    ACompleteNotifier.notifyError(project, ex.message ?: "Code generation failed")
                }
            }
        })
    }
}
