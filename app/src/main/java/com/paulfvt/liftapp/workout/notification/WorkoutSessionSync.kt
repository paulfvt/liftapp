package com.paulfvt.liftapp.workout.notification

import com.paulfvt.liftapp.core.di.ApplicationScope
import com.paulfvt.liftapp.core.domain.Units
import com.paulfvt.liftapp.settings.data.AppSettingsRepository
import com.paulfvt.liftapp.widget.WidgetUpdater
import com.paulfvt.liftapp.workout.data.WorkoutRepository
import com.paulfvt.liftapp.workout.domain.WorkoutNotice
import com.paulfvt.liftapp.workout.domain.toNotice
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Fait suivre la séance en cours à la notification et à l'alarme de fin de repos. La base reste la seule source :
// chaque changement (série validée dans l'app ou depuis la notification, +30 s, séance terminée) reconstruit les deux.
@Singleton
class WorkoutSessionSync @Inject constructor(
    private val workouts: WorkoutRepository,
    private val notifier: WorkoutNotifier,
    private val alarms: RestAlarmScheduler,
    private val widgets: WidgetUpdater,
    private val appSettings: AppSettingsRepository,
    private val clock: Clock,
    @param:ApplicationScope private val scope: CoroutineScope
) {
    private val mutex = Mutex()
    private var restEndJob: Job? = null

    // Lancé au démarrage de l'app. Après un redémarrage du téléphone, notification et alarme reviennent donc dès que
    // l'app est rouverte (la séance, elle, est reprise telle quelle).
    fun start() {
        notifier.createChannels()
        scope.launch {
            // Les unités font partie de ce qu'affiche la notification : un changement dans les réglages la reconstruit.
            combine(
                workouts.observeInProgress().map { it?.toNotice() }.distinctUntilChanged(),
                appSettings.settings.map { it.units }.distinctUntilChanged()
            ) { notice, units -> notice to units }.collect { (notice, units) -> apply(notice, units) }
        }
    }

    // Relit la base et reconstruit tout : après une action de la notification ou du widget, la fin d'un repos ou une
    // permission qui vient d'être accordée. Le widget est redessiné même si la séance n'a pas changé : à la fin d'un
    // repos, son compte à rebours doit laisser place à la durée de la séance.
    suspend fun refresh() {
        apply(workouts.observeInProgress().first()?.toNotice(), appSettings.current().units)
        widgets.update()
    }

    // Ce qui manque pour que l'alerte de fin de repos arrive à l'heure écran verrouillé, dans l'ordre où le demander.
    fun missingPermissions(): List<AlertPermission> = buildList {
        if (!notifier.canNotify()) add(AlertPermission.NOTIFICATIONS)
        if (!alarms.canScheduleExact()) add(AlertPermission.EXACT_ALARMS)
    }

    private suspend fun apply(notice: WorkoutNotice?, units: Units) = mutex.withLock {
        restEndJob?.cancel()
        restEndJob = null
        if (notice == null) {
            notifier.cancel()
            alarms.cancel()
            return@withLock
        }
        val now = clock.instant()
        notifier.show(notice, now, units)
        val rest = notice.rest?.takeIf { it.isRunning(now) }
        if (rest == null) {
            alarms.cancel()
            return@withLock
        }
        alarms.schedule(rest.endsAt)
        // Tant que l'app tourne, la notification quitte le compte à rebours dès la fin du repos, sans attendre
        // l'alarme (elle afficherait sinon un temps négatif si l'alarme est retardée).
        restEndJob = scope.launch {
            delay(Duration.between(now, rest.endsAt).toMillis())
            mutex.withLock { notifier.show(notice, clock.instant(), units) }
            widgets.update()
        }
    }
}

enum class AlertPermission {
    // Notifications refusées (Android 13+) ou coupées dans les réglages : rien n'est visible écran verrouillé.
    NOTIFICATIONS,

    // « Alarmes et rappels » non accordé : l'alerte de fin de repos peut être retardée.
    EXACT_ALARMS
}
