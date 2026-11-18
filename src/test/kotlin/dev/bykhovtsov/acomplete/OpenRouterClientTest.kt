package dev.bykhovtsov.acomplete

import dev.bykhovtsov.acomplete.client.OpenRouterClient
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class OpenRouterClientTest {

    private val client = OpenRouterClient()

    @Test
    fun testResolveEndpointUrl() {
        assertEquals(
            "https://openrouter.ai/api/v1/chat/completions",
            client.resolveEndpointUrl("https://openrouter.ai/api/v1")
        )
        assertEquals(
            "https://openrouter.ai/api/v1/chat/completions",
            client.resolveEndpointUrl("https://openrouter.ai/api/v1/")
        )
        assertEquals(
            "https://openrouter.ai/api/v1/chat/completions",
            client.resolveEndpointUrl("https://openrouter.ai/api/v1/chat/completions")
        )
        assertEquals(
            "https://openrouter.ai/api/v1/responses",
            client.resolveEndpointUrl("https://openrouter.ai/api/v1/responses")
        )
    }

    @Test
    fun testParseStandardChatCompletionResponse() {
        val json = """
            {
              "id": "gen-12345",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "val answer = 42"
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = client.parseResponseText(json)
        assertEquals("val answer = 42", parsed)
    }

    @Test
    fun testParseResponsesApiResponse() {
        val json = """
            {
              "id": "gen-67890",
              "output": [
                {
                  "type": "message",
                  "content": [
                    {
                      "type": "output_text",
                      "text": "println(\"Hello World\")"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsed = client.parseResponseText(json)
        assertEquals("println(\"Hello World\")", parsed)
    }

    @Test
    fun testParseApiErrorResponse() {
        val json = """
            {
              "error": {
                "message": "User key has insufficient balance",
                "code": 402
              }
            }
        """.trimIndent()

        try {
            client.parseResponseText(json)
            fail("Expected RuntimeException on API error")
        } catch (ex: RuntimeException) {
            assertEquals("User key has insufficient balance", ex.message)
        }
    }

    @Test
    fun testResolveModelsUrl() {
        assertEquals("https://openrouter.ai/api/v1/models", client.resolveModelsUrl("https://openrouter.ai/api/v1"))
        assertEquals("https://openrouter.ai/api/v1/models", client.resolveModelsUrl("https://openrouter.ai/api/v1/"))
        assertEquals("https://openrouter.ai/api/v1/models", client.resolveModelsUrl("https://openrouter.ai/api/v1/chat/completions"))
        assertEquals("https://openrouter.ai/api/v1/models", client.resolveModelsUrl("https://openrouter.ai/api/v1/responses"))
        assertEquals("https://openrouter.ai/api/v1/models", client.resolveModelsUrl("https://openrouter.ai/api/v1/models"))
    }

    @Test
    fun testParseModelsList() {
        val json = """
            {
              "data": [
                { "id": "openai/gpt-4o", "name": "GPT-4o" },
                { "id": "anthropic/claude-3.5-sonnet", "name": "Claude 3.5 Sonnet" },
                { "id": "google/gemini-2.5-flash", "name": "Gemini 2.5 Flash" }
              ]
            }
        """.trimIndent()

        val models = client.parseModelsList(json)
        assertEquals(3, models.size)
        // Should be sorted alphabetically
        assertEquals("anthropic/claude-3.5-sonnet", models[0])
        assertEquals("google/gemini-2.5-flash", models[1])
        assertEquals("openai/gpt-4o", models[2])
    }
}
