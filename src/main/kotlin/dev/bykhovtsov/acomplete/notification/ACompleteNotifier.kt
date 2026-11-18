package dev.bykhovtsov.acomplete.notification

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import dev.bykhovtsov.acomplete.settings.ACompleteConfigurable
import dev.bykhovtsov.acomplete.util.ACompleteBundle

object ACompleteNotifier {

    private const val NOTIFICATION_GROUP_ID = "A-Complete"

    fun notifyError(project: Project?, message: String, action: AnAction? = null) {
        val group = NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID)
        val notification = group.createNotification(
            ACompleteBundle.message("notification.error.title"),
            message,
            NotificationType.ERROR
        )
        if (action != null) {
            notification.addAction(action)
        }
        notification.notify(project)
    }

    fun notifyMissingApiKey(project: Project?) {
        val openSettingsAction = object : AnAction(ACompleteBundle.message("action.action.open.settings")) {
            override fun actionPerformed(e: AnActionEvent) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, ACompleteConfigurable::class.java)
            }
        }
        notifyError(
            project = project,
            message = ACompleteBundle.message("action.error.missing.key"),
            action = openSettingsAction
        )
    }

    fun notifyInfo(project: Project?, message: String) {
        val group = NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID)
        group.createNotification(
            ACompleteBundle.message("notification.info.title"),
            message,
            NotificationType.INFORMATION
        ).notify(project)
    }
}
