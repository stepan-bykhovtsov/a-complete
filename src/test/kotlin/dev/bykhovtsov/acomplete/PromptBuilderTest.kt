package dev.bykhovtsov.acomplete

import dev.bykhovtsov.acomplete.prompt.PromptBuilder
import dev.bykhovtsov.acomplete.util.EditorContext
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    @Test
    fun testBuildUserPromptIncludesKeyContext() {
        val context = EditorContext(
            prompt = "// calculate sum of array",
            baseIndentation = "    ",
            insertionOffset = 100,
            surroundingCode = "fun calculate() {\n    // calculate sum of array\n}",
            fileName = "Calculator.kt",
            fileExtension = "kt"
        )

        val userPrompt = PromptBuilder.buildUserPrompt(context)

        assertTrue(userPrompt.contains("Language: kt"))
        assertTrue(userPrompt.contains("File: Calculator.kt"))
        assertTrue(userPrompt.contains("// calculate sum of array"))
        assertTrue(userPrompt.contains("fun calculate()"))
    }

    @Test
    fun testSystemInstructionContainsRules() {
        val instruction = PromptBuilder.SYSTEM_INSTRUCTION
        assertTrue(instruction.contains("code completion engine"))
        assertTrue(instruction.contains("Output pure code only"))
        assertTrue(instruction.contains("no ```"))
    }
}
