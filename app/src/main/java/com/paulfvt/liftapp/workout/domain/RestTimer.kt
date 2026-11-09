package com.paulfvt.liftapp.workout.domain

import com.paulfvt.liftapp.template.domain.DEFAULT_REST_SECONDS
import java.time.Duration
import java.time.Instant

const val REST_ADD_SECONDS = 30
const val REST_REMOVE_SECONDS = 15
private const val MILLIS_PER_SECOND = 1_000L

// Fin de repos (décisions du 1er et du 9 novembre 2026) : à 3, 2 et 1 s de la fin, un petit ding et une brève
// impulsion ; à la fin, le ding ding d'1 s et une vibration longue.
private const val REST_ALERT_LEAD_SECONDS = 3L
private const val PULSE_MS = 200L
private const val FINAL_VIBRATION_MS = 1_000L

// L'alarme sonne 2 s avant le premier ding : le temps de lancer le service, de lire la base et de charger les sons
// (plus d'1 s sur l'émulateur), pour que le premier ding parte à l'heure.
val REST_ALARM_ADVANCE: Duration = Duration.ofSeconds(REST_ALERT_LEAD_SECONDS + 2)

// Fenêtre où l'alerte de fin de repos est due : dès l'alarme (avec 1 s de marge pour une alarme en avance) et jusqu'à
// 1 min après la fin. Une alarme retardée de plusieurs minutes (sans permission d'alarme exacte) ne doit pas sonner au
// milieu de la série suivante.
private val ALERT_EARLIEST: Duration = REST_ALARM_ADVANCE.plusSeconds(1)
private val ALERT_LATEST: Duration = Duration.ofMinutes(1).negated()

fun isRestAlertDue(untilEnd: Duration): Boolean = untilEnd <= ALERT_EARLIEST && untilEnd >= ALERT_LATEST

// Un temps fort de l'alerte, compté avant la fin du repos : un ding du décompte, ou la fin elle-même (beforeEnd nul).
data class RestBeat(val beforeEnd: Duration) {
    val isEnd: Boolean get() = beforeEnd.isZero
    val vibrationMs: Long get() = if (isEnd) FINAL_VIBRATION_MS else PULSE_MS
}

// Temps forts encore à venir, untilEnd étant le temps restant avant la fin du repos. Une alarme en retard saute les
// dings déjà passés ; la fin part tout de suite si elle est passée.
fun restAlertBeats(untilEnd: Duration): List<RestBeat> {
    val ticks = (REST_ALERT_LEAD_SECONDS downTo 1L).map { RestBeat(Duration.ofSeconds(it)) }
    return ticks.filter { it.beforeEnd <= untilEnd } + RestBeat(Duration.ZERO)
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
