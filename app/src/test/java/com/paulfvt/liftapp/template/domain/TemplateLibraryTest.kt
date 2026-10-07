package com.paulfvt.liftapp.template.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateLibraryTest {
    private fun template(id: Long, folderId: Long?) = TemplateSummary(id, folderId, "T$id", emptyList())

    @Test
    fun `templates are grouped by folder, keeping the given order`() {
        val folders = listOf(TemplateFolder(2, "Hiver"), TemplateFolder(1, "Été"))
        val templates = listOf(template(10, 1), template(11, null), template(12, 2), template(13, 1))

        val library = buildLibrary(folders, templates)

        assertEquals(listOf(11L), library.unfiled.map { it.id })
        assertEquals(listOf(2L, 1L), library.folders.map { it.folder.id })
        assertEquals(listOf(12L), library.folders[0].templates.map { it.id })
        assertEquals(listOf(10L, 13L), library.folders[1].templates.map { it.id })
    }

    @Test
    fun `an empty folder is still shown`() {
        val library = buildLibrary(listOf(TemplateFolder(1, "Vide")), emptyList())
        assertTrue(library.folders.single().templates.isEmpty())
    }

    @Test
    fun `a template whose folder no longer exists is shown outside folders`() {
        val library = buildLibrary(emptyList(), listOf(template(10, 99)))
        assertEquals(listOf(10L), library.unfiled.map { it.id })
    }

    private val library = buildLibrary(
        folders = listOf(TemplateFolder(1, "PPL")),
        templates = listOf(template(10, null), template(11, null), template(20, 1), template(21, 1), template(22, 1))
    )

    @Test
    fun `a template is moved inside its folder`() {
        val moved = library.moveTemplate(fromId = 22, toId = 20)
        assertEquals(listOf(22L, 20L, 21L), moved.folders.single().templates.map { it.id })
        assertEquals(library.unfiled, moved.unfiled)
    }

    @Test
    fun `a template is moved among unfiled templates`() {
        assertEquals(listOf(11L, 10L), library.moveTemplate(fromId = 10, toId = 11).unfiled.map { it.id })
    }

    @Test
    fun `a template cannot be dragged into another section`() {
        assertEquals(library, library.moveTemplate(fromId = 10, toId = 21))
    }

    @Test
    fun `the section of a template is found`() {
        assertEquals(listOf(20L, 21L, 22L), library.sectionContaining(21)?.map { it.id })
        assertEquals(listOf(10L, 11L), library.sectionContaining(10)?.map { it.id })
        assertNull(library.sectionContaining(99))
    }

    @Test
    fun `names are validated without surrounding spaces`() {
        assertNull(validateName("  Push A  "))
        assertEquals(NameError.EMPTY, validateName("   "))
        assertNull(validateName("a".repeat(TEMPLATE_NAME_MAX_LENGTH)))
        assertEquals(NameError.TOO_LONG, validateName("a".repeat(TEMPLATE_NAME_MAX_LENGTH + 1)))
    }

    @Test
    fun `a copy name is shortened to the limit`() {
        assertEquals("Copie de Push A", copyName("Copie de Push A"))
        val long = copyName("Copie de " + "a".repeat(TEMPLATE_NAME_MAX_LENGTH))
        assertEquals(TEMPLATE_NAME_MAX_LENGTH, long.length)
        assertNull(validateName(long))
    }
}
