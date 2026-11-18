package dev.bykhovtsov.acomplete

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "com.acomplete.ACompleteSettings",
    storages = [Storage("ACompletePluginSettings.xml")]
)
class ACompleteSettings : PersistentStateComponent<ACompleteSettings.State> {

    data class State(
        var apiKey: String = "",
        var model: String = "anthropic/claude-3.5-sonnet" // дефолтная модель
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        val instance: ACompleteSettings
            get() = ApplicationManager.getApplication().getService(ACompleteSettings::class.java)
    }
}