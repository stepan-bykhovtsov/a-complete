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
    fun testPopularModelsNotEmpty() {
        assertTrue(ACompleteSettings.POPULAR_MODELS.isNotEmpty())
        assertTrue(ACompleteSettings.POPULAR_MODELS.contains("anthropic/claude-3.5-sonnet"))
        assertTrue(ACompleteSettings.POPULAR_MODELS.contains("openai/gpt-4o-mini"))
        assertTrue(ACompleteSettings.POPULAR_MODELS.contains("deepseek/deepseek-chat"))
        assertTrue(ACompleteSettings.POPULAR_MODELS.contains("google/gemini-2.5-flash"))
    }
}
