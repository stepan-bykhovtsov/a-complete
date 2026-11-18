package dev.bykhovtsov.acomplete

import dev.bykhovtsov.acomplete.settings.ACompleteSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ACompleteSettingsTest {

    @Test
    fun testDefaultStateValues() {
        val state = ACompleteSettings.State()

        assertEquals("~anthropic/claude-haiku-latest", state.model)
        assertEquals("https://openrouter.ai/api/v1", state.apiBaseUrl)
        assertEquals(60, state.timeoutSeconds)
        assertEquals("", state.apiKey)
    }

    @Test
    fun testCachedModelsState() {
        val state = ACompleteSettings.State()
        assertTrue(state.cachedModels.isEmpty())

        state.cachedModels = listOf("model-1", "model-2")
        assertEquals(2, state.cachedModels.size)
        assertEquals("model-1", state.cachedModels[0])
    }
}
