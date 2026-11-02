package com.paulfvt.liftapp.workout.notification

import com.paulfvt.liftapp.workout.data.WorkoutRepository
import com.paulfvt.liftapp.workout.data.WorkoutSettingsRepository
import com.paulfvt.liftapp.workout.domain.isRestAlertDue
import com.paulfvt.liftapp.workout.domain.restEndVibration
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

// Déroulé de l'alerte de fin de repos : vibration pendant les 3 dernières secondes, puis le son à la fin, sauf si le
// repos a été passé ou prolongé entre-temps. Chaque étape relit la base, seule source de vérité.
@Singleton
class RestEndAlert @Inject constructor(
    private val workouts: WorkoutRepository,
    private val settings: WorkoutSettingsRepository,
    private val alerter: RestAlerter,
    private val sync: WorkoutSessionSync,
    private val clock: Clock
) {
    // Fin du repos en cours si son alerte est due maintenant, avec le son ou la vibration activés ; null sinon.
    suspend fun dueRestEnd(): Instant? {
        val endsAt = workouts.observeInProgress().first()?.rest?.endsAt ?: return null
        val current = settings.current()
        if (!current.restSound && !current.restVibration) return null
        return endsAt.takeIf { isRestAlertDue(Duration.between(clock.instant(), it)) }
    }

    suspend fun run(endsAt: Instant) {
        val current = settings.current()
        val untilEnd = Duration.between(clock.instant(), endsAt)
        if (current.restVibration) alerter.vibrate(restEndVibration(untilEnd.toMillis()))
        val chime = if (current.restSound) alerter.prepareChime() else null
        try {
            delay(Duration.between(clock.instant(), endsAt).toMillis().coerceAtLeast(0))
            // Repos passé ou prolongé pendant le décompte : pas de son, la vibration s'arrête.
            if (workouts.observeInProgress().first()?.rest?.endsAt == endsAt) chime?.play() else alerter.cancel()
        } finally {
            chime?.release()
        }
        // La notification et le widget quittent le compte à rebours.
        sync.refresh()
    }
}
