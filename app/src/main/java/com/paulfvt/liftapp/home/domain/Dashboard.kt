package com.paulfvt.liftapp.home.domain

import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.progress.domain.ExerciseHistory
import com.paulfvt.liftapp.progress.domain.RecordBreak
import com.paulfvt.liftapp.progress.domain.celebratedMetrics
import com.paulfvt.liftapp.progress.domain.recordBreaks
import com.paulfvt.liftapp.template.domain.TemplateLibrary
import com.paulfvt.liftapp.template.domain.TemplateSummary
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

// Prochaine séance proposée (décision du 6 octobre 2026) : le template qui suit le dernier utilisé dans son dossier
// (après le dernier du dossier, on repart du premier), ou le dernier utilisé lui-même s'il est hors dossier.
// recentTemplateIds : templates des séances terminées, du plus récemment utilisé au plus ancien. Un template supprimé
// depuis est ignoré : on remonte au précédent. Sans historique, le premier template de la liste ; sans template, rien.
fun nextTemplate(library: TemplateLibrary, recentTemplateIds: List<Long>): TemplateSummary? {
    val all = library.unfiled + library.folders.flatMap { it.templates }
    val lastUsed = recentTemplateIds.firstNotNullOfOrNull { id -> all.firstOrNull { it.id == id } }
        ?: return all.firstOrNull()
    val folder = library.folders.firstOrNull { section -> section.templates.any { it.id == lastUsed.id } }
        ?: return lastUsed
    val index = folder.templates.indexOfFirst { it.id == lastUsed.id }
    return folder.templates[(index + 1) % folder.templates.size]
}

// Numéro affiché sur l'accueil (« Semaine 44 ») : numérotation ISO, semaine du lundi au dimanche.
fun weekNumber(date: LocalDate): Int = date.get(WeekFields.ISO.weekOfWeekBasedYear())

private const val LAST_DAY_OFFSET = 6L

// Jours de la semaine commençant au lundi donné où une séance terminée a commencé, dans le fuseau du téléphone :
// une séance compte pour sa date de début (décision du 6 octobre 2026).
fun trainedDays(sessionStarts: List<Instant>, monday: LocalDate, zone: ZoneId): Set<LocalDate> {
    val sunday = monday.plusDays(LAST_DAY_OFFSET)
    return sessionStarts.map { it.atZone(zone).toLocalDate() }
        .filterTo(mutableSetOf()) { !it.isBefore(monday) && !it.isAfter(sunday) }
}

// Séance terminée d'un template : son début et sa fin.
data class TemplateSession(val templateId: Long, val startedAt: Instant, val endedAt: Instant?)

// Durée affichée sous la prochaine séance (décision du 1er novembre 2026) : celle de la dernière séance terminée avec
// ce template, null s'il n'a jamais été fait.
fun lastDuration(templateId: Long, sessions: List<TemplateSession>): Duration? = sessions
    .filter { it.templateId == templateId && it.endedAt != null }
    .maxByOrNull { it.startedAt }
    ?.let { Duration.between(it.startedAt, it.endedAt) }

data class LatestRecord(val exercise: Exercise, val recordBreak: RecordBreak)

// Dernier record battu, tous exercices confondus (jamais à la première séance sur un exercice). Si une séance en a
// battu plusieurs, la mesure principale de l'exercice passe d'abord (1RM estimé avant la charge max).
fun latestRecord(histories: List<ExerciseHistory>): LatestRecord? = histories
    .flatMap { (exercise, sets) ->
        recordBreaks(sets, exercise.trackingType).map { LatestRecord(exercise, it) }
    }
    .sortedWith(
        compareByDescending<LatestRecord> { it.recordBreak.achievedAt }
            .thenBy { celebratedMetrics(it.exercise.trackingType).indexOf(it.recordBreak.metric) }
            .thenBy { it.exercise.name }
    )
    .firstOrNull()
