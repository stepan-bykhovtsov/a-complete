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
    fun testExtractMentionWithTrailingPunctuation() {
        val textWithComma = "// TODO: Add text from @file:screenshot.png, removing comments"
        val mentionsComma = FileMentionResolver.extractMentionPaths(textWithComma)
        assertEquals(1, mentionsComma.size)
        assertEquals("screenshot.png", mentionsComma["@file:screenshot.png"])

        val textWithPeriod = "// Please review @file:spec.pdf."
        val mentionsPeriod = FileMentionResolver.extractMentionPaths(textWithPeriod)
        assertEquals("spec.pdf", mentionsPeriod["@file:spec.pdf"])

        val textWithParen = "// See (@file:diagram.png) for details"
        val mentionsParen = FileMentionResolver.extractMentionPaths(textWithParen)
        assertEquals("diagram.png", mentionsParen["@file:diagram.png"])

        val textWithSemi = "// load @file:data.json; process next"
        val mentionsSemi = FileMentionResolver.extractMentionPaths(textWithSemi)
        assertEquals("data.json", mentionsSemi["@file:data.json"])

        val textWithComplex = "// Compare (@file:a.png) and (@file:b.png), then merge"
        val mentionsComplex = FileMentionResolver.extractMentionPaths(textWithComplex)
        assertEquals(2, mentionsComplex.size)
        assertEquals("a.png", mentionsComplex["@file:a.png"])
        assertEquals("b.png", mentionsComplex["@file:b.png"])
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
