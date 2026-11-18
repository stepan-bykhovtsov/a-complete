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
}
