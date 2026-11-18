package dev.bykhovtsov.acomplete.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import dev.bykhovtsov.acomplete.service.ACompleteService
import dev.bykhovtsov.acomplete.util.ACompleteBundle
import java.awt.Font
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent

class ACompleteConfigurable : SearchableConfigurable {

    private val settings = ACompleteSettings.instance

    private val apiKeyField = JBPasswordField()
    private val modelComboBox = JComboBox<String>().apply {
        isEditable = true
    }
    private val modelsStatusLabel = JBLabel().apply {
        font = font.deriveFont(Font.PLAIN)
    }
    private val apiBaseUrlField = JBTextField()
    private val timeoutField = JBTextField()
    private val temperatureField = JBTextField()
    private val maxTokensField = JBTextField()
    private val contextWindowField = JBTextField()

    private val testStatusLabel = JBLabel().apply {
        font = font.deriveFont(Font.PLAIN)
    }

    override fun getId(): String = "dev.bykhovtsov.acomplete.settings.ACompleteConfigurable"

    override fun getDisplayName(): String = ACompleteBundle.message("settings.display.name")

    override fun createComponent(): JComponent {
        val rootPanel = panel {
            group(ACompleteBundle.message("settings.group.api")) {
                row(ACompleteBundle.message("settings.apiKey.label")) {
                    cell(apiKeyField)
                        .columns(35)
                        .comment(ACompleteBundle.message("settings.apiKey.comment"))
                }
                row(ACompleteBundle.message("settings.model.label")) {
                    cell(modelComboBox)
                        .comment(ACompleteBundle.message("settings.model.comment"))
                    button(ACompleteBundle.message("settings.refreshModels.button")) {
                        refreshModelsList()
                    }
                    cell(modelsStatusLabel)
                }
                row(ACompleteBundle.message("settings.apiBaseUrl.label")) {
                    cell(apiBaseUrlField)
                        .columns(35)
                        .comment(ACompleteBundle.message("settings.apiBaseUrl.comment"))
                }
                row {
                    button(ACompleteBundle.message("settings.testConnection.button")) {
                        runConnectionTest()
                    }
                    cell(testStatusLabel)
                }
            }

            collapsibleGroup(ACompleteBundle.message("settings.group.advanced")) {
                row(ACompleteBundle.message("settings.timeout.label")) {
                    cell(timeoutField)
                        .columns(10)
                        .comment(ACompleteBundle.message("settings.timeout.comment"))
                }
                row(ACompleteBundle.message("settings.temperature.label")) {
                    cell(temperatureField)
                        .columns(10)
                        .comment(ACompleteBundle.message("settings.temperature.comment"))
                }
                row(ACompleteBundle.message("settings.maxTokens.label")) {
                    cell(maxTokensField)
                        .columns(10)
                        .comment(ACompleteBundle.message("settings.maxTokens.comment"))
                }
                row(ACompleteBundle.message("settings.contextWindow.label")) {
                    cell(contextWindowField)
                        .columns(10)
                        .comment(ACompleteBundle.message("settings.contextWindow.comment"))
                }
            }
        }

        reset()
        return rootPanel
    }

    private fun runConnectionTest() {
        val key = String(apiKeyField.password).trim()
        val model = (modelComboBox.editor.item?.toString() ?: modelComboBox.selectedItem?.toString()).orEmpty().trim()
        val url = apiBaseUrlField.text.trim().ifEmpty { ACompleteSettings.DEFAULT_API_BASE_URL }

        if (key.isEmpty()) {
            testStatusLabel.text = ACompleteBundle.message("settings.testConnection.emptyKey")
            testStatusLabel.foreground = JBColor.RED
            return
        }

        testStatusLabel.text = ACompleteBundle.message("settings.testConnection.testing")
        testStatusLabel.foreground = JBColor.GRAY

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = ACompleteService.instance.testConnection(key, model, url)
            ApplicationManager.getApplication().invokeLater {
                if (result.isSuccess) {
                    testStatusLabel.text = ACompleteBundle.message("settings.testConnection.success", result.latencyMs)
                    testStatusLabel.foreground = JBColor(0x28a745, 0x5cb85c)
                } else {
                    testStatusLabel.text = ACompleteBundle.message("settings.testConnection.failed", result.errorMessage ?: "Unknown error")
                    testStatusLabel.foreground = JBColor.RED
                }
            }
        }
    }

    override fun isModified(): Boolean {
        val state = settings.state
        val currentKey = String(apiKeyField.password).trim()
        val storedKey = ACompleteCredentials.getApiKey() ?: ""
        if (currentKey != storedKey) return true

        val currentModel = (modelComboBox.editor.item?.toString() ?: modelComboBox.selectedItem?.toString()).orEmpty().trim()
        if (currentModel != state.model) return true

        if (apiBaseUrlField.text.trim() != state.apiBaseUrl) return true
        if (timeoutField.text.toIntOrNull() != state.timeoutSeconds) return true
        if (temperatureField.text.toDoubleOrNull() != state.temperature) return true
        if (maxTokensField.text.toIntOrNull() != state.maxTokens) return true
        if (contextWindowField.text.toIntOrNull() != state.contextLinesWindow) return true

        return false
    }

    override fun apply() {
        val state = settings.state
        val newKey = String(apiKeyField.password).trim()
        ACompleteCredentials.setApiKey(newKey)

        val selectedModel = (modelComboBox.editor.item?.toString() ?: modelComboBox.selectedItem?.toString()).orEmpty().trim()
        if (selectedModel.isNotEmpty()) {
            state.model = selectedModel
        }

        state.apiBaseUrl = apiBaseUrlField.text.trim().ifEmpty { ACompleteSettings.DEFAULT_API_BASE_URL }
        timeoutField.text.toIntOrNull()?.let { state.timeoutSeconds = it.coerceIn(1, 120) }
        temperatureField.text.toDoubleOrNull()?.let { state.temperature = it.coerceIn(0.0, 2.0) }
        maxTokensField.text.toIntOrNull()?.let { state.maxTokens = it.coerceIn(16, 8192) }
        contextWindowField.text.toIntOrNull()?.let { state.contextLinesWindow = it.coerceIn(50, 2000) }
    }

    private fun refreshModelsList() {
        val url = apiBaseUrlField.text.trim().ifEmpty { ACompleteSettings.DEFAULT_API_BASE_URL }
        val key = String(apiKeyField.password).trim().ifEmpty { ACompleteCredentials.getApiKey() ?: "" }

        modelsStatusLabel.text = ACompleteBundle.message("settings.refreshModels.loading")
        modelsStatusLabel.foreground = JBColor.GRAY

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val models = ACompleteService.instance.fetchModels(url, key)
                ApplicationManager.getApplication().invokeLater {
                    updateModelComboBox(models)
                    modelsStatusLabel.text = ACompleteBundle.message("settings.refreshModels.success", models.size)
                    modelsStatusLabel.foreground = JBColor(0x28a745, 0x5cb85c)
                }
            } catch (ex: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    modelsStatusLabel.text = ACompleteBundle.message("settings.refreshModels.failed", ex.message ?: "Error")
                    modelsStatusLabel.foreground = JBColor.RED
                }
            }
        }
    }

    private fun updateModelComboBox(models: List<String>) {
        val currentSelection = (modelComboBox.editor.item?.toString() ?: modelComboBox.selectedItem?.toString()).orEmpty().trim()
        modelComboBox.model = DefaultComboBoxModel(models.toTypedArray())
        if (currentSelection.isNotEmpty()) {
            modelComboBox.selectedItem = currentSelection
            modelComboBox.editor.item = currentSelection
        }
    }

    override fun reset() {
        val state = settings.state
        val storedKey = ACompleteCredentials.getApiKey() ?: ""
        apiKeyField.text = storedKey

        val cached = state.cachedModels
        if (cached.isNotEmpty()) {
            updateModelComboBox(cached)
        } else {
            // Asynchronously fetch on opening if cache is not yet populated
            refreshModelsList()
        }

        modelComboBox.selectedItem = state.model
        modelComboBox.editor.item = state.model

        apiBaseUrlField.text = state.apiBaseUrl
        timeoutField.text = state.timeoutSeconds.toString()
        temperatureField.text = state.temperature.toString()
        maxTokensField.text = state.maxTokens.toString()
        contextWindowField.text = state.contextLinesWindow.toString()
        testStatusLabel.text = ""
        modelsStatusLabel.text = ""
    }
}
