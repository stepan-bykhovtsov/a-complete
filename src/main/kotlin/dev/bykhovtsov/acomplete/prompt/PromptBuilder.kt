package dev.bykhovtsov.acomplete.prompt

import dev.bykhovtsov.acomplete.header.IncludeType
import dev.bykhovtsov.acomplete.util.EditorContext

object PromptBuilder {

    const val SYSTEM_INSTRUCTION = """You are a lightning-fast, high-precision code completion engine.
Generate ONLY the exact code snippet that should be inserted directly after the prompt line.
Strict rules:
1. Output pure code only. Do NOT wrap in markdown backticks or code blocks (no ```).
2. Do NOT repeat the prompt line or surrounding code.
3. Do NOT output any explanations, conversation, or greeting.
4. Match indentation, naming style, and conventions of the surrounding code."""

    fun buildUserPrompt(context: EditorContext): String {
        val langInfo = if (context.fileExtension.isNotEmpty()) {
            "Language: ${context.fileExtension}\nFile: ${context.fileName}"
        } else {
            "File: ${context.fileName}"
        }

        val headersSection = if (context.includedHeaders.isNotEmpty()) {
            val headersContent = context.includedHeaders.joinToString("\n\n") { header ->
                val includeStr = if (header.includeType == IncludeType.ANGLE) {
                    "#include <${header.includePath}>"
                } else {
                    "#include \"${header.includePath}\""
                }
                """
                |Header: $includeStr (resolved: ${header.resolvedPath})
                |```
                |${header.content}
                |```
                """.trimMargin()
            }
            """
            |Included Headers:
            |$headersContent
            """.trimMargin()
        } else {
            ""
        }

        val headerBlock = if (headersSection.isNotEmpty()) "\n\n$headersSection" else ""

        return """
            |$langInfo$headerBlock
            |
            |Context:
            |```
            |${context.surroundingCode}
            |```
            |
            |Prompt:
            |```
            |${context.prompt}
            |```
            |
            |Instruction: Complete the prompt above. Output ONLY the code to be inserted directly after it.
        """.trimMargin()
    }
}
