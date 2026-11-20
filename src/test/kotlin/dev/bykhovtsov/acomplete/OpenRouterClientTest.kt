package dev.bykhovtsov.acomplete

import com.google.gson.Gson
import dev.bykhovtsov.acomplete.client.OpenRouterClient
import dev.bykhovtsov.acomplete.client.model.ChatCompletionRequest
import dev.bykhovtsov.acomplete.client.model.ChatMessage
import dev.bykhovtsov.acomplete.client.model.ContentPart
import dev.bykhovtsov.acomplete.client.model.ImageUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun testParseModelsListSortedByDateDescending() {
        val json = """
            {
              "data": [
                { "id": "model/old", "name": "Old Model", "created": 1600000000 },
                { "id": "model/newest", "name": "Newest Model", "created": 1800000000 },
                { "id": "model/mid", "name": "Mid Model", "created": 1700000000 }
              ]
            }
        """.trimIndent()

        val models = client.parseModelsList(json)
        assertEquals(3, models.size)
        // Should be sorted by created descending: newest -> mid -> old
        assertEquals("model/newest", models[0])
        assertEquals("model/mid", models[1])
        assertEquals("model/old", models[2])
    }

    @Test
    fun testMultimodalMessageSerialization() {
        val gson = client.gson
        val parts = listOf(
            ContentPart(type = "text", text = "Generate layout"),
            ContentPart(type = "image_url", imageUrl = ImageUrl("data:image/png;base64,iVBORw0KGgoAAAANSUhEUg==")),
            ContentPart(type = "image_url", imageUrl = ImageUrl("data:application/pdf;base64,JVBERi0xLjQK"))
        )
        val request = ChatCompletionRequest(
            model = "anthropic/claude-3.5-sonnet",
            messages = listOf(
                ChatMessage(role = "system", content = "System prompt"),
                ChatMessage(role = "user", content = parts)
            )
        )

        val json = gson.toJson(request)
        assertTrue(json.contains("\"role\":\"system\""))
        assertTrue(json.contains("\"content\":\"System prompt\""))
        assertTrue(json.contains("\"type\":\"image_url\""))
        assertTrue(json.contains("\"url\":\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUg==\""))
        assertTrue(json.contains("\"url\":\"data:application/pdf;base64,JVBERi0xLjQK\""))
    }

    @Test
    fun testParseChatCompletionResponseWithContentPartsList() {
        val json = """
            {
              "id": "gen-123",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": [
                      {
                        "type": "text",
                        "text": "class HeaderView : JPanel()"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = client.parseResponseText(json)
        assertEquals("class HeaderView : JPanel()", parsed)
    }
}
