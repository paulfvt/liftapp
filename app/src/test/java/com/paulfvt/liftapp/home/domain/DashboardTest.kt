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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    fun `the week number follows the ISO numbering, a week spanning two months has one number`() {
        assertEquals(44, weekNumber(LocalDate.parse("2026-10-26")))
        assertEquals(44, weekNumber(LocalDate.parse("2026-11-01")))
        assertEquals(53, weekNumber(LocalDate.parse("2026-12-31")))
        assertEquals(1, weekNumber(LocalDate.parse("2027-01-04")))
    }

    @Test
    fun `a session counts for the local day it started, other weeks are ignored`() {
        val paris = ZoneId.of("Europe/Paris")
        val monday = LocalDate.parse("2026-10-26")
        val starts = listOf(
            // Dimanche 25 à 23 h 30 à Paris : semaine précédente.
            Instant.parse("2026-10-25T22:30:00Z"),
            Instant.parse("2026-10-26T17:00:00Z"),
            // Deux séances le même jour : un seul jour entraîné.
            Instant.parse("2026-10-28T06:00:00Z"),
            Instant.parse("2026-10-28T18:00:00Z"),
            // Samedi 31 octobre à 23 h 30 UTC, déjà dimanche 1er novembre à Paris : même semaine.
            Instant.parse("2026-10-31T23:30:00Z"),
            Instant.parse("2026-11-02T17:00:00Z")
        )

        assertEquals(
            setOf(LocalDate.parse("2026-10-26"), LocalDate.parse("2026-10-28"), LocalDate.parse("2026-11-01")),
            trainedDays(starts, monday, paris)
        )
    }

    @Test
    fun `the duration shown is the one of the last finished session with this template`() {
        fun session(templateId: Long, start: String, minutes: Long?) = TemplateSession(
            templateId,
            Instant.parse(start),
            minutes?.let { Instant.parse(start).plusSeconds(it * 60) }
        )
        val sessions = listOf(
            session(2, "2026-10-20T17:00:00Z", 58),
            session(2, "2026-10-27T17:00:00Z", 65),
            session(3, "2026-10-29T17:00:00Z", 40)
        )

        assertEquals(java.time.Duration.ofMinutes(65), lastDuration(2, sessions))
        assertNull(lastDuration(4, sessions))
        assertNull(lastDuration(5, listOf(session(5, "2026-10-29T17:00:00Z", null))))
    }

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
