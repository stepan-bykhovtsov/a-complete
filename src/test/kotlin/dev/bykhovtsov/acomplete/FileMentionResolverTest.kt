package dev.bykhovtsov.acomplete

import dev.bykhovtsov.acomplete.mention.FileMentionResolver
import dev.bykhovtsov.acomplete.mention.MentionedFileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileMentionResolverTest {

    @Test
    fun testExtractSingleMention() {
        val text = "// please refactor using @file:src/main/App.kt"
        val mentions = FileMentionResolver.extractMentionPaths(text)

        assertEquals(1, mentions.size)
        assertEquals("src/main/App.kt", mentions["@file:src/main/App.kt"])
    }

    @Test
    fun testExtractMultipleMentions() {
        val text = "// combine @file:models/User.kt with @file:assets/diagram.png and @file:docs/spec.pdf"
        val mentions = FileMentionResolver.extractMentionPaths(text)

        assertEquals(3, mentions.size)
        assertEquals("models/User.kt", mentions["@file:models/User.kt"])
        assertEquals("assets/diagram.png", mentions["@file:assets/diagram.png"])
        assertEquals("docs/spec.pdf", mentions["@file:docs/spec.pdf"])
    }

    @Test
    fun testExtractQuotedMentionWithSpaces() {
        val text = "// look at @file:\"my folder/my file.txt\" for reference"
        val mentions = FileMentionResolver.extractMentionPaths(text)

        assertEquals(1, mentions.size)
        assertEquals("my folder/my file.txt", mentions["@file:\"my folder/my file.txt\""])
    }

    @Test
    fun testIgnoresNonMentionText() {
        val text = "Contact support@example.com or user@domain.org for info"
        val mentions = FileMentionResolver.extractMentionPaths(text)

        assertTrue(mentions.isEmpty())
    }

    @Test
    fun testDetermineFileType() {
        assertEquals(MentionedFileType.PDF, FileMentionResolver.determineFileType("pdf"))
        assertEquals(MentionedFileType.PDF, FileMentionResolver.determineFileType(".PDF"))

        assertEquals(MentionedFileType.IMAGE, FileMentionResolver.determineFileType("png"))
        assertEquals(MentionedFileType.IMAGE, FileMentionResolver.determineFileType("jpg"))
        assertEquals(MentionedFileType.IMAGE, FileMentionResolver.determineFileType("jpeg"))
        assertEquals(MentionedFileType.IMAGE, FileMentionResolver.determineFileType("gif"))
        assertEquals(MentionedFileType.IMAGE, FileMentionResolver.determineFileType("webp"))
        assertEquals(MentionedFileType.IMAGE, FileMentionResolver.determineFileType("svg"))

        assertEquals(MentionedFileType.TEXT, FileMentionResolver.determineFileType("kt", isBinary = false))
        assertEquals(MentionedFileType.TEXT, FileMentionResolver.determineFileType("java", isBinary = false))
        assertEquals(MentionedFileType.TEXT, FileMentionResolver.determineFileType("py", isBinary = false))
        assertEquals(MentionedFileType.TEXT, FileMentionResolver.determineFileType("c", isBinary = false))
        assertEquals(MentionedFileType.TEXT, FileMentionResolver.determineFileType("h", isBinary = false))
        assertEquals(MentionedFileType.TEXT, FileMentionResolver.determineFileType("json", isBinary = false))
        assertEquals(MentionedFileType.TEXT, FileMentionResolver.determineFileType("md", isBinary = false))
        assertEquals(MentionedFileType.TEXT, FileMentionResolver.determineFileType("txt", isBinary = false))

        assertEquals(MentionedFileType.UNSUPPORTED, FileMentionResolver.determineFileType("exe", isBinary = true))
        assertEquals(MentionedFileType.UNSUPPORTED, FileMentionResolver.determineFileType("zip", isBinary = true))
    }
}
