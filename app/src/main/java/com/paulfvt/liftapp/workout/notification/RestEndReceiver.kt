package com.paulfvt.liftapp.workout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.paulfvt.liftapp.core.di.ApplicationScope
import com.paulfvt.liftapp.workout.data.WorkoutRepository
import com.paulfvt.liftapp.workout.data.WorkoutSettingsRepository
import com.paulfvt.liftapp.workout.domain.RestPeriod
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Reçoit l'alarme de fin de repos, alerte, puis remet la notification sur la séance.
@AndroidEntryPoint
class RestEndReceiver : BroadcastReceiver() {
    @Inject lateinit var workouts: WorkoutRepository

    @Inject lateinit var settings: WorkoutSettingsRepository

    @Inject lateinit var alerter: RestAlerter

    @Inject lateinit var sync: WorkoutSessionSync

    @Inject lateinit var clock: Clock

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        // goAsync : le récepteur reste en vie le temps de lire la base (quelques secondes au plus).
        val pending = goAsync()
        scope.launch {
            try {
                val rest = workouts.observeInProgress().first()?.rest
                if (rest != null && shouldAlert(rest, clock.instant())) alerter.alert(settings.current())
                sync.refresh()
            } finally {
                pending.finish()
            }
        }
    }

    // Repos bien terminé, et pas depuis trop longtemps : une alarme retardée de plusieurs minutes (sans permission
    // d'alarme exacte) ne doit pas vibrer au milieu de la série suivante.
    private fun shouldAlert(rest: RestPeriod, now: Instant): Boolean =
        !rest.isRunning(now) && Duration.between(rest.endsAt, now) <= MAX_ALERT_DELAY

    private companion object {
        val MAX_ALERT_DELAY: Duration = Duration.ofMinutes(1)
    }
}
