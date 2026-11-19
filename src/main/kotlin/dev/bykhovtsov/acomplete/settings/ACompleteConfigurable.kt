package dev.bykhovtsov.acomplete.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
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

    private val testStatusLabel = JBLabel().apply {
        font = font.deriveFont(Font.PLAIN)
    }

    // C / C++ Header Context UI components
    private val includeHeadersCheckBox = JBCheckBox(ACompleteBundle.message("settings.includeHeaders.label"))
    private val headerDepthComboBox = JComboBox(ACompleteSettings.DEPTH_PRESETS)
    private val customDepthField = JBTextField()
    private val includeQuoteHeadersCheckBox = JBCheckBox(ACompleteBundle.message("settings.includeQuoteHeaders.label"))
    private val includeAngleHeadersCheckBox = JBCheckBox(ACompleteBundle.message("settings.includeAngleHeaders.label"))

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
                row(ACompleteBundle.message("settings.timeout.label")) {
                    cell(timeoutField)
                        .columns(10)
                        .comment(ACompleteBundle.message("settings.timeout.comment"))
                }
                row {
                    button(ACompleteBundle.message("settings.testConnection.button")) {
                        runConnectionTest()
                    }
                    cell(testStatusLabel)
                }
            }

            group(ACompleteBundle.message("settings.group.cHeaderContext")) {
                row {
                    cell(includeHeadersCheckBox)
                        .comment(ACompleteBundle.message("settings.includeHeaders.comment"))
                }
                row(ACompleteBundle.message("settings.headerDepth.label")) {
                    cell(headerDepthComboBox)
                        .comment(ACompleteBundle.message("settings.headerDepth.comment"))
                    cell(customDepthField)
                        .columns(5)
                        .comment(ACompleteBundle.message("settings.customDepth.comment"))
                }
                row(ACompleteBundle.message("settings.includeTypes.label")) {
                    cell(includeQuoteHeadersCheckBox)
                        .comment(ACompleteBundle.message("settings.includeQuoteHeaders.comment"))
                    cell(includeAngleHeadersCheckBox)
                        .comment(ACompleteBundle.message("settings.includeAngleHeaders.comment"))
                }
            }
        }

        includeHeadersCheckBox.addActionListener { updateHeadersUiState() }
        headerDepthComboBox.addActionListener { updateHeadersUiState() }

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

        if (includeHeadersCheckBox.isSelected != state.includeHeadersEnabled) return true
        val currentPreset = headerDepthComboBox.selectedItem?.toString() ?: ACompleteSettings.DEPTH_PRESET_3
        if (currentPreset != getDepthPresetItem(state.headerSearchDepthPreset)) return true
        if (customDepthField.text.toIntOrNull() != state.customHeaderSearchDepth) return true
        if (includeQuoteHeadersCheckBox.isSelected != state.includeQuoteHeaders) return true
        if (includeAngleHeadersCheckBox.isSelected != state.includeAngleHeaders) return true

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
        timeoutField.text.toIntOrNull()?.let { state.timeoutSeconds = it.coerceIn(1, 300) }

        state.includeHeadersEnabled = includeHeadersCheckBox.isSelected
        state.headerSearchDepthPreset = headerDepthComboBox.selectedItem?.toString() ?: ACompleteSettings.DEPTH_PRESET_3
        customDepthField.text.toIntOrNull()?.let { state.customHeaderSearchDepth = it.coerceIn(1, 1000) }
        state.includeQuoteHeaders = includeQuoteHeadersCheckBox.isSelected
        state.includeAngleHeaders = includeAngleHeadersCheckBox.isSelected
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
        testStatusLabel.text = ""
        modelsStatusLabel.text = ""

        includeHeadersCheckBox.isSelected = state.includeHeadersEnabled
        headerDepthComboBox.selectedItem = getDepthPresetItem(state.headerSearchDepthPreset)
        customDepthField.text = state.customHeaderSearchDepth.toString()
        includeQuoteHeadersCheckBox.isSelected = state.includeQuoteHeaders
        includeAngleHeadersCheckBox.isSelected = state.includeAngleHeaders
        updateHeadersUiState()
    }

    private fun getDepthPresetItem(savedPreset: String): String {
        val lower = savedPreset.trim().lowercase()
        return when {
            lower.startsWith("0") || lower.contains("off") -> ACompleteSettings.DEPTH_PRESET_0
            lower.startsWith("1") -> ACompleteSettings.DEPTH_PRESET_1
            lower.startsWith("3") -> ACompleteSettings.DEPTH_PRESET_3
            lower.contains("inf") || lower.contains("entire") || lower.contains("unlimited") -> ACompleteSettings.DEPTH_PRESET_INF
            lower.contains("custom") -> ACompleteSettings.DEPTH_PRESET_CUSTOM
            else -> ACompleteSettings.DEPTH_PRESET_CUSTOM
        }
    }

    private fun updateHeadersUiState() {
        val enabled = includeHeadersCheckBox.isSelected
        headerDepthComboBox.isEnabled = enabled
        val isCustom = headerDepthComboBox.selectedItem == ACompleteSettings.DEPTH_PRESET_CUSTOM
        customDepthField.isEnabled = enabled && isCustom
        includeQuoteHeadersCheckBox.isEnabled = enabled
        includeAngleHeadersCheckBox.isEnabled = enabled
    }
}
