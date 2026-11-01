package com.paulfvt.liftapp.home.domain

import com.paulfvt.liftapp.progress.domain.ExerciseHistory
import com.paulfvt.liftapp.workout.domain.setTonnageKg
import com.paulfvt.liftapp.workout.domain.tonnageChange
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Séance terminée : son template (null pour une séance libre), son début et sa fin.
data class FinishedSession(val templateId: Long?, val startedAt: Instant, val endedAt: Instant?)

// Totaux d'une période : tonnage (séries validées hors échauffements), durée cumulée et nombre de séances.
data class WeekTotals(val tonnageKg: Double, val duration: Duration, val sessions: Int)

// Tuiles « Cette semaine » de l'accueil. tonnageChange : +0,06 pour « +6 % » par rapport à la semaine précédente,
// null quand l'une des deux périodes n'a pas de tonnage.
data class WeekStats(val current: WeekTotals, val tonnageChange: Double?, val previousWeekNumber: Int)

// Semaine en cours jusqu'à maintenant, comparée à la semaine précédente prise au même moment : du lundi jusqu'au même
// jour et à la même heure (décision du 1er novembre 2026). Comparée à une semaine entière, la semaine en cours
// paraîtrait toujours en baisse avant le dimanche. Une séance compte pour sa date de début, dans le fuseau du téléphone.
fun weekStats(
    sessions: List<FinishedSession>,
    histories: List<ExerciseHistory>,
    monday: LocalDate,
    now: Instant,
    zone: ZoneId
): WeekStats {
    val current = totals(sessions, histories, from = monday.atStartOfDay(zone).toInstant(), to = now)
    val previousMonday = monday.minusWeeks(1)
    val previous = totals(
        sessions,
        histories,
        from = previousMonday.atStartOfDay(zone).toInstant(),
        // Même heure locale une semaine plus tôt, même si l'heure d'été a changé entre-temps.
        to = now.atZone(zone).minusWeeks(1).toInstant()
    )
    return WeekStats(current, tonnageChange(current.tonnageKg, previous.tonnageKg), weekNumber(previousMonday))
}

private fun totals(
    sessions: List<FinishedSession>,
    histories: List<ExerciseHistory>,
    from: Instant,
    to: Instant
): WeekTotals {
    fun isInside(instant: Instant) = !instant.isBefore(from) && !instant.isAfter(to)
    val kept = sessions.filter { isInside(it.startedAt) }
    val tonnage = histories.sumOf { (exercise, sets) ->
        sets.filter { isInside(it.performedAt) }.sumOf { setTonnageKg(exercise.trackingType, it.type, it.values) }
    }
    val duration = kept.mapNotNull { session -> session.endedAt?.let { Duration.between(session.startedAt, it) } }
        .fold(Duration.ZERO, Duration::plus)
    return WeekTotals(tonnage, duration, kept.size)
}
