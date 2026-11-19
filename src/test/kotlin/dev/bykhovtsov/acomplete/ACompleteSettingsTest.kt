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
        assertTrue(state.includeHeadersEnabled)
        assertEquals(ACompleteSettings.DEPTH_PRESET_3, state.headerSearchDepthPreset)
        assertEquals(5, state.customHeaderSearchDepth)
        assertTrue(state.includeQuoteHeaders)
        assertTrue(state.includeAngleHeaders)
    }

    @Test
    fun testHeaderDepthCalculation() {
        val state = ACompleteSettings.State()

        // Default: preset 3
        assertEquals(3, state.getEffectiveDepth())

        // Depth 0 (Off)
        state.headerSearchDepthPreset = ACompleteSettings.DEPTH_PRESET_0
        assertEquals(0, state.getEffectiveDepth())

        // Depth 1
        state.headerSearchDepthPreset = ACompleteSettings.DEPTH_PRESET_1
        assertEquals(1, state.getEffectiveDepth())

        // Depth inf
        state.headerSearchDepthPreset = ACompleteSettings.DEPTH_PRESET_INF
        assertEquals(Int.MAX_VALUE, state.getEffectiveDepth())

        // Depth custom
        state.headerSearchDepthPreset = ACompleteSettings.DEPTH_PRESET_CUSTOM
        state.customHeaderSearchDepth = 7
        assertEquals(7, state.getEffectiveDepth())

        // When feature is disabled, effective depth is always 0
        state.includeHeadersEnabled = false
        assertEquals(0, state.getEffectiveDepth())
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
