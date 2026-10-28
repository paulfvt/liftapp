package com.paulfvt.liftapp.progress.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

const val DEFAULT_WEEKLY_GOAL = 3
val WEEKLY_GOAL_RANGE = 1..7

// Objectif choisi au premier lancement : il vaut pour toutes les semaines, d'où une date de départ très ancienne.
val GOAL_SINCE_ALWAYS: LocalDate = LocalDate.EPOCH

// Objectif de séances par semaine, valable à partir du lundi effectiveFrom. Il est historisé : un changement ajoute
// une ligne, et les semaines passées gardent l'objectif de l'époque pour le calcul de la streak.
data class WeeklyGoal(val goal: Int, val effectiveFrom: LocalDate)

// Lundi de la semaine d'un jour : les semaines vont du lundi au dimanche.
fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

// Une séance compte pour la semaine de sa date de début, dans le fuseau du téléphone.
fun weekOf(instant: Instant, zone: ZoneId): LocalDate = weekStart(instant.atZone(zone).toLocalDate())

// Objectif d'une semaine : le dernier fixé au plus tard ce lundi-là. Avant tout choix, 3 séances par défaut.
fun goalForWeek(goals: List<WeeklyGoal>, monday: LocalDate): Int =
    goals.filter { !it.effectiveFrom.isAfter(monday) }.maxByOrNull { it.effectiveFrom }?.goal ?: DEFAULT_WEEKLY_GOAL

data class WeekProgress(val monday: LocalDate, val sessions: Int, val goal: Int) {
    val isReached: Boolean get() = sessions >= goal
}

// Semaine dont l'objectif est atteint, avec le début de la séance qui l'a atteint (date des badges de streak).
data class ReachedWeek(val monday: LocalDate, val reachedAt: Instant)

fun reachedWeeks(sessionStarts: List<Instant>, goals: List<WeeklyGoal>, zone: ZoneId): List<ReachedWeek> =
    sessionStarts.sorted()
        .groupBy { weekOf(it, zone) }
        .mapNotNull { (monday, starts) ->
            starts.getOrNull(goalForWeek(goals, monday) - 1)?.let { ReachedWeek(monday, it) }
        }
        .sortedBy { it.monday }

// Suites de semaines consécutives dont l'objectif est atteint, dans l'ordre.
fun streakRuns(reached: List<ReachedWeek>): List<List<ReachedWeek>> {
    val runs = mutableListOf<MutableList<ReachedWeek>>()
    reached.sortedBy { it.monday }.forEach { week ->
        val run = runs.lastOrNull()
        val continuesRun = run != null && run.last().monday.plusWeeks(1) == week.monday
        if (continuesRun) run.add(week) else runs.add(mutableListOf(week))
    }
    return runs
}

// weeks : semaines d'affilée dont l'objectif est atteint, jusqu'à aujourd'hui. bestWeeks : la plus longue suite.
data class WeeklyStreak(val currentWeek: WeekProgress, val weeks: Int, val bestWeeks: Int)

// La semaine en cours ne casse jamais la streak : on compte à partir de la semaine dernière, puis on ajoute la
// semaine en cours seulement si son objectif est déjà atteint. Les jours de repos ne sont jamais pénalisés.
fun weeklyStreak(sessionStarts: List<Instant>, goals: List<WeeklyGoal>, now: Instant, zone: ZoneId): WeeklyStreak {
    val thisMonday = weekOf(now, zone)
    val reached = reachedWeeks(sessionStarts, goals, zone)
    val reachedMondays = reached.map { it.monday }.toSet()
    val current = WeekProgress(
        monday = thisMonday,
        sessions = sessionStarts.count { weekOf(it, zone) == thisMonday },
        goal = goalForWeek(goals, thisMonday)
    )
    var weeks = 0
    var monday = thisMonday.minusWeeks(1)
    while (monday in reachedMondays) {
        weeks++
        monday = monday.minusWeeks(1)
    }
    if (current.isReached) weeks++
    val best = streakRuns(reached.filter { !it.monday.isAfter(thisMonday) }).maxOfOrNull { it.size } ?: 0
    return WeeklyStreak(current, weeks, maxOf(best, weeks))
}
