package dev.bykhovtsov.acomplete

import dev.bykhovtsov.acomplete.mention.MentionedFile
import dev.bykhovtsov.acomplete.mention.MentionedFileType
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
    fun testBuildUserPromptWithIncludedHeaders() {
        val headers = listOf(
            dev.bykhovtsov.acomplete.header.HeaderFileContext(
                includePath = "user.h",
                resolvedPath = "include/user.h",
                content = "typedef struct { int id; } User;",
                includeType = dev.bykhovtsov.acomplete.header.IncludeType.QUOTE
            ),
            dev.bykhovtsov.acomplete.header.HeaderFileContext(
                includePath = "system/types.h",
                resolvedPath = "sys/system/types.h",
                content = "#define MAX_USERS 100",
                includeType = dev.bykhovtsov.acomplete.header.IncludeType.ANGLE
            )
        )

        val context = EditorContext(
            prompt = "// create user with id 1",
            baseIndentation = "    ",
            insertionOffset = 50,
            surroundingCode = "#include \"user.h\"\n#include <system/types.h>\n\nvoid init() {\n    // create user with id 1\n}",
            fileName = "main.c",
            fileExtension = "c",
            includedHeaders = headers
        )

        val userPrompt = PromptBuilder.buildUserPrompt(context)

        assertTrue(userPrompt.contains("Language: c"))
        assertTrue(userPrompt.contains("File: main.c"))
        assertTrue(userPrompt.contains("Included Headers:"))
        assertTrue(userPrompt.contains("Header: #include \"user.h\" (resolved: include/user.h)"))
        assertTrue(userPrompt.contains("typedef struct { int id; } User;"))
        assertTrue(userPrompt.contains("Header: #include <system/types.h> (resolved: sys/system/types.h)"))
        assertTrue(userPrompt.contains("#define MAX_USERS 100"))
    }

    @Test
    fun testSystemInstructionContainsRules() {
        val instruction = PromptBuilder.SYSTEM_INSTRUCTION
        assertTrue(instruction.contains("code completion engine"))
        assertTrue(instruction.contains("Output pure code only"))
        assertTrue(instruction.contains("no ```"))
    }

    @Test
    fun testBuildUserPromptWithTextMention() {
        val mentionedFiles = listOf(
            MentionedFile(
                rawMention = "@file:models/User.kt",
                relativePath = "models/User.kt",
                type = MentionedFileType.TEXT,
                mimeType = "text/plain",
                textContent = "data class User(val id: Int, val name: String)"
            )
        )

        val context = EditorContext(
            prompt = "// create user from @file:models/User.kt",
            baseIndentation = "    ",
            insertionOffset = 45,
            surroundingCode = "fun main() {\n    // create user from @file:models/User.kt\n}",
            fileName = "Main.kt",
            fileExtension = "kt",
            mentionedFiles = mentionedFiles
        )

        val userPrompt = PromptBuilder.buildUserPrompt(context)

        assertTrue(userPrompt.contains("Referenced Files:"))
        assertTrue(userPrompt.contains("Referenced File: models/User.kt"))
        assertTrue(userPrompt.contains("data class User(val id: Int, val name: String)"))
        assertTrue(userPrompt.contains("// create user from @file:models/User.kt"))
    }

    @Test
    fun testBuildUserPromptWithBinaryMentions() {
        val mentionedFiles = listOf(
            MentionedFile(
                rawMention = "@file:assets/mockup.png",
                relativePath = "assets/mockup.png",
                type = MentionedFileType.IMAGE,
                mimeType = "image/png",
                base64DataUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg=="
            ),
            MentionedFile(
                rawMention = "@file:docs/spec.pdf",
                relativePath = "docs/spec.pdf",
                type = MentionedFileType.PDF,
                mimeType = "application/pdf",
                base64DataUrl = "data:application/pdf;base64,JVBERi0xLjQK"
            )
        )

        val context = EditorContext(
            prompt = "// implement UI matching @file:assets/mockup.png per @file:docs/spec.pdf",
            baseIndentation = "    ",
            insertionOffset = 80,
            surroundingCode = "// implement UI matching @file:assets/mockup.png per @file:docs/spec.pdf",
            fileName = "App.kt",
            fileExtension = "kt",
            mentionedFiles = mentionedFiles
        )

        val userPrompt = PromptBuilder.buildUserPrompt(context)

        assertTrue(userPrompt.contains("Referenced Attachments:"))
        assertTrue(userPrompt.contains("- Attached image: assets/mockup.png"))
        assertTrue(userPrompt.contains("- Attached document: docs/spec.pdf"))
    }
}
