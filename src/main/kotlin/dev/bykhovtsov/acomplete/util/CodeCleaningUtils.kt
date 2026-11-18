package dev.bykhovtsov.acomplete.util

object CodeCleaningUtils {

    private val CODE_BLOCK_REGEX = Regex("""(?s)```[a-zA-Z0-9_#+.-]*\r?\n(.*?)\r?\n```""")

    /**
     * Strips markdown code blocks (e.g., ```kotlin ... ```) and extracts raw code.
     * If multiple code blocks exist, extracts the most relevant one or concatenates.
     * If no code blocks exist, strips any trailing/leading backticks or fences.
     */
    fun cleanGeneratedCode(rawText: String): String {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return ""

        // Case 1: Regex matches standard fenced code block ```lang ... ```
        val match = CODE_BLOCK_REGEX.find(trimmed)
        if (match != null) {
            val extracted = match.groupValues[1].trimEnd()
            if (extracted.isNotEmpty()) {
                return extracted
            }
        }

        // Case 2: Starts with ``` and ends with ```
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            val content = trimmed
                .substringAfter("\n", missingDelimiterValue = "")
                .substringBeforeLast("```", missingDelimiterValue = "")
                .trimEnd()
            if (content.isNotEmpty()) {
                return content
            }
        }

        // Case 3: Output starts with ``` but might be missing closing fence
        if (trimmed.startsWith("```")) {
            val content = trimmed.substringAfter("\n", missingDelimiterValue = "").trimEnd()
            if (content.isNotEmpty()) {
                return content
            }
        }

        // Case 4: Pure code returned without fences
        return trimmed
    }

    /**
     * Adjusts the indentation of multi-line generated code to match the base indentation of the prompt line.
     * Preserves relative indentation between lines.
     */
    fun applyIndentation(code: String, baseIndentation: String): String {
        if (code.isEmpty() || baseIndentation.isEmpty()) return code

        val lines = code.lines()
        if (lines.size <= 1) {
            // Single line: if it doesn't already have base indentation, prepend it
            return if (lines[0].startsWith(baseIndentation)) lines[0] else baseIndentation + lines[0].trimStart()
        }

        // Find the minimum common indentation of non-blank lines
        val nonBlankLines = lines.filter { it.isNotBlank() }
        val minExistingIndent = nonBlankLines.minOfOrNull { line ->
            line.takeWhile { it.isWhitespace() }.length
        } ?: 0

        return lines.joinToString("\n") { line ->
            if (line.isBlank()) {
                ""
            } else {
                val lineIndent = line.takeWhile { it.isWhitespace() }.length
                val relativeIndent = (lineIndent - minExistingIndent).coerceAtLeast(0)
                baseIndentation + " ".repeat(relativeIndent) + line.trimStart()
            }
        }
    }
}
