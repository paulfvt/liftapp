package com.paulfvt.liftapp.workout.notification

import com.paulfvt.liftapp.workout.data.WorkoutRepository
import com.paulfvt.liftapp.workout.data.WorkoutSettingsRepository
import com.paulfvt.liftapp.workout.domain.isRestAlertDue
import com.paulfvt.liftapp.workout.domain.restAlertBeats
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

// Déroulé de l'alerte de fin de repos : un petit ding et une impulsion à 3, 2 et 1 s de la fin, puis le ding ding et
// une vibration longue à la fin, sauf si le repos a été passé ou prolongé entre-temps. Chaque temps fort relit la base,
// seule source de vérité.
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
        val sounds = if (current.restSound) alerter.loadSounds() else null
        try {
            // Calculés une fois les sons prêts : un ding déjà passé est sauté plutôt que joué en retard.
            for (beat in restAlertBeats(Duration.between(clock.instant(), endsAt))) {
                delay(Duration.between(clock.instant(), endsAt.minus(beat.beforeEnd)).toMillis().coerceAtLeast(0))
                // Repos passé ou prolongé pendant le décompte : plus rien, la vibration en cours s'arrête.
                if (workouts.observeInProgress().first()?.rest?.endsAt != endsAt) {
                    alerter.cancel()
                    break
                }
                if (current.restVibration) alerter.pulse(beat.vibrationMs)
                sounds?.play(beat)
            }
        } finally {
            sounds?.close()
        }
        // La notification et le widget quittent le compte à rebours.
        sync.refresh()
    }
}
