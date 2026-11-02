package com.paulfvt.liftapp.workout.domain

import com.paulfvt.liftapp.template.domain.DEFAULT_REST_SECONDS
import java.time.Duration
import java.time.Instant

const val REST_ADD_SECONDS = 30
const val REST_REMOVE_SECONDS = 15
private const val MILLIS_PER_SECOND = 1_000L

// Fin de repos (décision du 1er novembre 2026) : la vibration commence 3 s avant la fin, une brève impulsion par
// seconde, puis une vibration longue pendant le son de fin d'1 s.
const val REST_ALERT_LEAD_SECONDS = 3L
private const val PULSE_MS = 200L
private const val FINAL_VIBRATION_MS = 1_000L

// Fenêtre où l'alerte de fin de repos est due, l'alarme sonnant 3 s avant la fin : pendant le décompte (avec 1 s de
// marge pour une alarme en avance) et jusqu'à 1 min après la fin. Une alarme retardée de plusieurs minutes (sans
// permission d'alarme exacte) ne doit pas sonner au milieu de la série suivante.
private val ALERT_EARLIEST: Duration = Duration.ofSeconds(REST_ALERT_LEAD_SECONDS + 1)
private val ALERT_LATEST: Duration = Duration.ofMinutes(1).negated()

fun isRestAlertDue(untilEnd: Duration): Boolean = untilEnd <= ALERT_EARLIEST && untilEnd >= ALERT_LATEST

// Minutages de la vibration à partir de maintenant, à la manière d'Android : pause, vibration, pause, vibration…
// untilEndMs : temps restant avant la fin du repos. Une alarme en retard ne garde que les impulsions encore à venir ;
// la vibration finale part tout de suite si la fin est déjà passée.
fun restEndVibration(untilEndMs: Long): LongArray {
    val timings = mutableListOf<Long>()
    var cursor = 0L
    (REST_ALERT_LEAD_SECONDS downTo 1L).map {
        untilEndMs - it * MILLIS_PER_SECOND
    }.filter { it >= 0 }.forEach { start ->
        timings += start - cursor
        timings += PULSE_MS
        cursor = start + PULSE_MS
    }
    timings += maxOf(untilEndMs, cursor) - cursor
    timings += FINAL_VIBRATION_MS
    return timings.toLongArray()
}

// Repos en cours. Seules les deux bornes sont enregistrées : le temps restant se recalcule toujours à partir de
// l'heure de fin, il ne dérive donc pas, même si l'app est arrêtée pendant le repos.
data class RestPeriod(val startedAt: Instant, val endsAt: Instant) {
    val totalSeconds: Int get() = Duration.between(startedAt, endsAt).seconds.toInt()

    fun isRunning(now: Instant): Boolean = now.isBefore(endsAt)

    // Arrondi au-dessus : « 1:00 » reste affiché jusqu'à la dernière fraction de seconde, puis « 0:59 ».
    fun remainingSeconds(now: Instant): Int {
        val millis = Duration.between(now, endsAt).toMillis()
        return if (millis <= 0) 0 else ((millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    }
}

// Repos lancé en validant une série : celui de son exercice, 2 min par défaut. Pas de repos après la dernière
// série de la séance (plus rien à attendre), ni si l'exercice n'a pas de repos (0 s).
fun Workout.restAfterCompleting(setId: Long, now: Instant): RestPeriod? {
    val exercise = exercises.firstOrNull { item -> item.sets.any { it.id == setId } } ?: return null
    val remainsToDo = exercises.any { item -> item.sets.any { !it.isCompleted && it.id != setId } }
    val seconds = exercise.restSeconds ?: DEFAULT_REST_SECONDS
    if (!remainsToDo || seconds <= 0) return null
    return RestPeriod(now, now.plusSeconds(seconds.toLong()))
}

// « +30 s » et « −15 s ». Un repos déjà terminé repart de maintenant (+30 s relance 30 s de repos) ;
// un repos ramené à 0 s ou moins est terminé (null).
fun RestPeriod?.adjusted(deltaSeconds: Int, now: Instant): RestPeriod? {
    val running = this?.takeIf { it.isRunning(now) }
    val endsAt = (running?.endsAt ?: now).plusSeconds(deltaSeconds.toLong())
    if (!endsAt.isAfter(now)) return null
    return RestPeriod(running?.startedAt ?: now, endsAt)
}

// Série à faire ensuite, affichée par la notification : la première non validée, dans l'ordre de la séance.
// position et count numérotent la série dans son exercice (« Série 3 sur 4 »).
data class NextSet(val exercise: WorkoutExercise, val set: WorkoutSet, val position: Int, val count: Int)

fun Workout.nextSet(): NextSet? {
    for (item in exercises) {
        val index = item.sets.indexOfFirst { !it.isCompleted }
        if (index != -1) return NextSet(item, item.sets[index], position = index + 1, count = item.sets.size)
    }
    return null
}
