package dev.bykhovtsov.acomplete.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(
    name = "dev.bykhovtsov.acomplete.settings.ACompleteSettings",
    storages = [Storage("ACompletePluginSettings.xml")]
)
class ACompleteSettings : PersistentStateComponent<ACompleteSettings.State> {

    data class State(
        var model: String = DEFAULT_MODEL,
        var apiBaseUrl: String = DEFAULT_API_BASE_URL,
        var timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS,
        var cachedModels: List<String> = emptyList(),
        var apiKey: String = "" // Kept solely for automatic migration from legacy plaintext storage
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
        // Automatic backward-compatible migration of legacy plaintext API key to PasswordSafe
        if (myState.apiKey.isNotBlank()) {
            if (ACompleteCredentials.getApiKey().isNullOrBlank()) {
                ACompleteCredentials.setApiKey(myState.apiKey)
            }
            myState.apiKey = ""
        }
    }

    companion object {
        const val DEFAULT_MODEL = "~anthropic/claude-haiku-latest"
        const val DEFAULT_API_BASE_URL = "https://openrouter.ai/api/v1"
        const val DEFAULT_TIMEOUT_SECONDS = 60

        val instance: ACompleteSettings
            get() = ApplicationManager.getApplication().getService(ACompleteSettings::class.java)
    }
}
