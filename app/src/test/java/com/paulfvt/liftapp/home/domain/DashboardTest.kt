package com.paulfvt.liftapp.home.domain

import com.paulfvt.liftapp.progress.domain.ExerciseHistory
import com.paulfvt.liftapp.progress.domain.ExerciseMetric
import com.paulfvt.liftapp.progress.domain.at
import com.paulfvt.liftapp.progress.domain.performed
import com.paulfvt.liftapp.progress.domain.testExercise
import com.paulfvt.liftapp.template.domain.FolderSection
import com.paulfvt.liftapp.template.domain.TemplateFolder
import com.paulfvt.liftapp.template.domain.TemplateLibrary
import com.paulfvt.liftapp.template.domain.TemplateSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardTest {
    private fun template(id: Long, folderId: Long? = null) = TemplateSummary(id, folderId, "T$id", emptyList())

    // Hors dossier : 1 (Full body). Dossier PPL : 2 (Push), 3 (Pull), 4 (Jambes). Dossier Solo : 5.
    private val library = TemplateLibrary(
        unfiled = listOf(template(1)),
        folders = listOf(
            FolderSection(TemplateFolder(10, "PPL"), listOf(template(2, 10), template(3, 10), template(4, 10))),
            FolderSection(TemplateFolder(11, "Solo"), listOf(template(5, 11)))
        )
    )

    @Test
    fun `the next session follows the last one used in its folder`() {
        assertEquals(3L, nextTemplate(library, listOf(2, 1))?.id)
        assertEquals(4L, nextTemplate(library, listOf(3))?.id)
    }

    @Test
    fun `after the last template of a folder comes its first`() {
        assertEquals(2L, nextTemplate(library, listOf(4, 3))?.id)
    }

    @Test
    fun `a template outside any folder is proposed again`() {
        assertEquals(1L, nextTemplate(library, listOf(1, 4))?.id)
    }

    @Test
    fun `a folder with a single template proposes it again`() {
        assertEquals(5L, nextTemplate(library, listOf(5))?.id)
    }

    @Test
    fun `a deleted template is skipped for the one used before it`() {
        assertEquals(4L, nextTemplate(library, listOf(99, 3))?.id)
    }

    @Test
    fun `without history the first template of the list is proposed, without template nothing`() {
        assertEquals(1L, nextTemplate(library, emptyList())?.id)
        assertNull(nextTemplate(TemplateLibrary(emptyList(), emptyList()), listOf(1)))
    }

    @Test
    fun `the latest record is the most recent one broken`() {
        val squat = testExercise(1, name = "Squat")
        val bench = testExercise(2, name = "Développé couché")
        val histories = listOf(
            ExerciseHistory(squat, listOf(performed("2026-10-01", 100.0, 5), performed("2026-10-08", 105.0, 5))),
            ExerciseHistory(bench, listOf(performed("2026-10-02", 80.0, 5), performed("2026-10-15", 82.5, 5)))
        )
        val latest = latestRecord(histories)!!

        assertEquals("Développé couché", latest.exercise.name)
        assertEquals(at("2026-10-15"), latest.recordBreak.achievedAt)
        // Charge max, 1RM et volume battus le même jour : le 1RM estimé, mesure principale, est montré.
        assertEquals(ExerciseMetric.ESTIMATED_1RM, latest.recordBreak.metric)
    }

    @Test
    fun `a first session sets no latest record`() {
        val squat = testExercise(1)
        assertNull(latestRecord(listOf(ExerciseHistory(squat, listOf(performed("2026-10-01", 100.0, 5))))))
    }
}
