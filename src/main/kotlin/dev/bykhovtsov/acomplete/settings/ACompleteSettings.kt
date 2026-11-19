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
        var apiKey: String = "", // Kept solely for automatic migration from legacy plaintext storage

        // C / C++ Header Context Settings
        var includeHeadersEnabled: Boolean = true,
        var headerSearchDepthPreset: String = DEPTH_PRESET_3,
        var customHeaderSearchDepth: Int = 5,
        var includeQuoteHeaders: Boolean = true,
        var includeAngleHeaders: Boolean = true
    ) {
        fun getEffectiveDepth(): Int {
            if (!includeHeadersEnabled) return 0
            val preset = headerSearchDepthPreset.trim().lowercase()
            return when {
                preset.startsWith("0") || preset.contains("off") -> 0
                preset.startsWith("1") -> 1
                preset.startsWith("3") -> 3
                preset.contains("inf") || preset.contains("entire") || preset.contains("unlimited") -> Int.MAX_VALUE
                preset.contains("custom") -> customHeaderSearchDepth.coerceAtLeast(1)
                else -> preset.toIntOrNull()?.coerceAtLeast(0) ?: 3
            }
        }
    }

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

        const val DEPTH_PRESET_0 = "0 (Off)"
        const val DEPTH_PRESET_1 = "1 (Direct headers only)"
        const val DEPTH_PRESET_3 = "3 (3 levels deep)"
        const val DEPTH_PRESET_INF = "inf (Entire project)"
        const val DEPTH_PRESET_CUSTOM = "Custom"

        val DEPTH_PRESETS = arrayOf(
            DEPTH_PRESET_0,
            DEPTH_PRESET_1,
            DEPTH_PRESET_3,
            DEPTH_PRESET_INF,
            DEPTH_PRESET_CUSTOM
        )

        val instance: ACompleteSettings
            get() = ApplicationManager.getApplication()?.getService(ACompleteSettings::class.java)
                ?: ACompleteSettings()
    }
}
