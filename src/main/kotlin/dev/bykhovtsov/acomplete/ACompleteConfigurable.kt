package dev.bykhovtsov.acomplete

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import dev.bykhovtsov.acomplete.ACompleteSettings

class ACompleteConfigurable : BoundConfigurable("A-Complete") {

    private val settings = ACompleteSettings.instance

    override fun createPanel(): DialogPanel = panel {
        group("OpenRouter Configuration") {
            row("API Key:") {
                passwordField()
                    .bindText(settings.state::apiKey)
                    .columns(30)
                    .comment("Ключ с openrouter.ai")
            }
            row("Model:") {
                // Используем обычное текстовое поле, чтобы можно было вписать любую новую модель,
                // но при желании можно заменить на comboBox(listOf("model1", "model2"))
                textField()
                    .bindText(settings.state::model)
                    .columns(30)
                    .comment("Например: anthropic/claude-3.5-sonnet или openai/gpt-4o-mini")
            }
        }
    }
}