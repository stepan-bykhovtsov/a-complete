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

        assertEquals("anthropic/claude-3.5-sonnet", state.model)
        assertEquals("https://openrouter.ai/api/v1", state.apiBaseUrl)
        assertEquals(15, state.timeoutSeconds)
        assertEquals(0.2, state.temperature, 0.001)
        assertEquals(1024, state.maxTokens)
        assertEquals(250, state.contextLinesWindow)
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
