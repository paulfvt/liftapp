package com.paulfvt.liftapp.home.domain

import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.progress.domain.ExerciseHistory
import com.paulfvt.liftapp.progress.domain.RecordBreak
import com.paulfvt.liftapp.progress.domain.celebratedMetrics
import com.paulfvt.liftapp.progress.domain.recordBreaks
import com.paulfvt.liftapp.template.domain.TemplateLibrary
import com.paulfvt.liftapp.template.domain.TemplateSummary

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
