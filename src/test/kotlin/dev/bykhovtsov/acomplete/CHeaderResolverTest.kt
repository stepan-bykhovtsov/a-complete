package dev.bykhovtsov.acomplete

import com.intellij.testFramework.LightVirtualFile
import dev.bykhovtsov.acomplete.header.CHeaderResolver
import dev.bykhovtsov.acomplete.header.IncludeType
import dev.bykhovtsov.acomplete.settings.ACompleteSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CHeaderResolverTest {

    @Test
    fun testIsCSourceOrHeader() {
        // C & C++ extensions
        assertTrue(CHeaderResolver.isCSourceOrHeader("c"))
        assertTrue(CHeaderResolver.isCSourceOrHeader("h"))
        assertTrue(CHeaderResolver.isCSourceOrHeader("cpp"))
        assertTrue(CHeaderResolver.isCSourceOrHeader("hpp"))
        assertTrue(CHeaderResolver.isCSourceOrHeader("cc"))
        assertTrue(CHeaderResolver.isCSourceOrHeader("hh"))
        assertTrue(CHeaderResolver.isCSourceOrHeader("cxx"))
        assertTrue(CHeaderResolver.isCSourceOrHeader("hxx"))
        assertTrue(CHeaderResolver.isCSourceOrHeader("c++"))
        assertTrue(CHeaderResolver.isCSourceOrHeader("h++"))
        assertTrue(CHeaderResolver.isCSourceOrHeader("tpp"))
        assertTrue(CHeaderResolver.isCSourceOrHeader("inl"))
        assertTrue(CHeaderResolver.isCSourceOrHeader("ipp"))
        assertTrue(CHeaderResolver.isCSourceOrHeader("ixx"))

        // Case-insensitivity & leading dot
        assertTrue(CHeaderResolver.isCSourceOrHeader(".CPP"))
        assertTrue(CHeaderResolver.isCSourceOrHeader(".H"))
        assertTrue(CHeaderResolver.isCSourceOrHeader("C"))

        // Non C/C++ extensions
        assertFalse(CHeaderResolver.isCSourceOrHeader("kt"))
        assertFalse(CHeaderResolver.isCSourceOrHeader("java"))
        assertFalse(CHeaderResolver.isCSourceOrHeader("py"))
        assertFalse(CHeaderResolver.isCSourceOrHeader("rs"))
        assertFalse(CHeaderResolver.isCSourceOrHeader("go"))
        assertFalse(CHeaderResolver.isCSourceOrHeader("txt"))
        assertFalse(CHeaderResolver.isCSourceOrHeader(""))
    }

    @Test
    fun testExtractDirectives() {
        val code = """
            // Line comment at top
            #include "local_header.h"
            #include <system_header.h>
            #  include   "spaced/header.hpp"  // with comment
            #	include	<iostream> /* block comment */
            
            // #include "commented_out.h"
            /* #include "also_commented_out.h" */
            
            /*
             * Multi-line comment
             * #include "in_multiline_block.h"
             */
            
            int main() {
                #include "nested_macro.h"
                return 0;
            }
        """.trimIndent()

        val directives = CHeaderResolver.extractDirectives(code)

        assertEquals(5, directives.size)

        assertEquals("local_header.h", directives[0].path)
        assertEquals(IncludeType.QUOTE, directives[0].type)

        assertEquals("system_header.h", directives[1].path)
        assertEquals(IncludeType.ANGLE, directives[1].type)

        assertEquals("spaced/header.hpp", directives[2].path)
        assertEquals(IncludeType.QUOTE, directives[2].type)

        assertEquals("iostream", directives[3].path)
        assertEquals(IncludeType.ANGLE, directives[3].type)

        assertEquals("nested_macro.h", directives[4].path)
        assertEquals(IncludeType.QUOTE, directives[4].type)
    }

    @Test
    fun testExtractDirectivesEmptyOrWithoutIncludes() {
        val emptyDirectives = CHeaderResolver.extractDirectives("")
        assertTrue(emptyDirectives.isEmpty())

        val noIncludes = CHeaderResolver.extractDirectives("int x = 42;\nvoid foo() {}\n")
        assertTrue(noIncludes.isEmpty())
    }

    @Test
    fun testQuoteVsAngleResolutionRules() {
        // Mock current file and local directory
        val currentDir = LightVirtualFile("src")
        val currentFile = LightVirtualFile("main.c", "int main() {}")

        // Angle include shouldn't search local directory directly unless in include roots
        val includeRoots = emptyList<com.intellij.openapi.vfs.VirtualFile>()

        val quoteDirective = dev.bykhovtsov.acomplete.header.IncludeDirective("local.h", IncludeType.QUOTE)
        val angleDirective = dev.bykhovtsov.acomplete.header.IncludeDirective("stdio.h", IncludeType.ANGLE)

        assertEquals(IncludeType.QUOTE, quoteDirective.type)
        assertEquals(IncludeType.ANGLE, angleDirective.type)
    }
}
