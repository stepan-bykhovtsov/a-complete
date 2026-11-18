package dev.bykhovtsov.acomplete

import dev.bykhovtsov.acomplete.util.CodeCleaningUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeCleaningUtilsTest {

    @Test
    fun testStripMarkdownWithLanguageHeader() {
        val input = """
            |```kotlin
            |fun hello(): String {
            |    return "world"
            |}
            |```
        """.trimMargin()

        val expected = """
            |fun hello(): String {
            |    return "world"
            |}
        """.trimMargin()

        val actual = CodeCleaningUtils.cleanGeneratedCode(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testStripMarkdownWithoutLanguageHeader() {
        val input = """
            |```
            |val x = 42
            |```
        """.trimMargin()

        val expected = "val x = 42"
        val actual = CodeCleaningUtils.cleanGeneratedCode(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testCleanRawCodeWithoutMarkdown() {
        val input = "val greeting = \"hello world\""
        val actual = CodeCleaningUtils.cleanGeneratedCode(input)
        assertEquals(input, actual)
    }

    @Test
    fun testEmptyInput() {
        assertEquals("", CodeCleaningUtils.cleanGeneratedCode(""))
        assertEquals("", CodeCleaningUtils.cleanGeneratedCode("   "))
    }

    @Test
    fun testApplyIndentationSingleLine() {
        val code = "val result = 42"
        val indent = "    "
        val actual = CodeCleaningUtils.applyIndentation(code, indent)
        assertEquals("    val result = 42", actual)
    }

    @Test
    fun testApplyIndentationAlreadyIndentedSingleLine() {
        val code = "    val result = 42"
        val indent = "    "
        val actual = CodeCleaningUtils.applyIndentation(code, indent)
        assertEquals("    val result = 42", actual)
    }

    @Test
    fun testApplyIndentationMultiLineRelative() {
        val code = """
            |if (condition) {
            |    doSomething()
            |}
        """.trimMargin()

        val indent = "        " // 8 spaces
        val actual = CodeCleaningUtils.applyIndentation(code, indent)

        val expected = """
            |        if (condition) {
            |            doSomething()
            |        }
        """.trimMargin()

        assertEquals(expected, actual)
    }
}
