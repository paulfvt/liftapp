package com.paulfvt.liftapp.workout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.paulfvt.liftapp.core.di.ApplicationScope
import com.paulfvt.liftapp.settings.data.AppSettingsRepository
import com.paulfvt.liftapp.workout.data.WorkoutRepository
import com.paulfvt.liftapp.workout.data.WorkoutSettingsRepository
import com.paulfvt.liftapp.workout.domain.REST_ALERT_LEAD_SECONDS
import com.paulfvt.liftapp.workout.domain.restEndVibration
import com.paulfvt.liftapp.workout.domain.toNotice
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Reçoit l'alarme 3 s avant la fin du repos : vibration pendant le décompte, son à la fin, puis remet la notification
// sur la séance.
@AndroidEntryPoint
class RestEndReceiver : BroadcastReceiver() {
    @Inject lateinit var workouts: WorkoutRepository

    @Inject lateinit var settings: WorkoutSettingsRepository

    @Inject lateinit var alerter: RestAlerter

    @Inject lateinit var notifier: WorkoutNotifier

    @Inject lateinit var sync: WorkoutSessionSync

    @Inject lateinit var clock: Clock

    @Inject lateinit var appSettings: AppSettingsRepository

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        // goAsync : le récepteur reste en vie le temps du décompte (3 s) et de la lecture de la base.
        val pending = goAsync()
        scope.launch {
            try {
                alertIfDue()
                sync.refresh()
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun alertIfDue() {
        val rest = workouts.observeInProgress().first()?.rest ?: return
        val untilEnd = Duration.between(clock.instant(), rest.endsAt)
        if (!shouldAlert(untilEnd)) return
        val current = settings.current()
        if (current.restVibration) alerter.vibrate(restEndVibration(untilEnd.toMillis()))
        if (!untilEnd.isNegative) delay(untilEnd.toMillis())
        // Repos passé ou prolongé pendant le décompte : pas de son, la vibration s'arrête.
        val workout = workouts.observeInProgress().first()
        if (workout?.rest?.endsAt != rest.endsAt) {
            alerter.cancel()
            return
        }
        if (current.restSound) notifier.showRestEnd(workout.toNotice(), appSettings.current().units)
    }

    // Décompte des 3 dernières secondes (avec une marge pour une alarme en avance), et fin pas trop ancienne : une
    // alarme retardée de plusieurs minutes (sans permission d'alarme exacte) ne doit pas sonner au milieu de la série
    // suivante.
    private fun shouldAlert(untilEnd: Duration): Boolean =
        untilEnd <= LEAD_WITH_MARGIN && untilEnd >= MAX_ALERT_DELAY.negated()

    private companion object {
        val LEAD_WITH_MARGIN: Duration = Duration.ofSeconds(REST_ALERT_LEAD_SECONDS + 1)
        val MAX_ALERT_DELAY: Duration = Duration.ofMinutes(1)
    }
}
