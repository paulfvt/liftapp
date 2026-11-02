package com.paulfvt.liftapp.workout.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Service de premier plan de quelques secondes, lancé par l'alarme 3 s avant la fin du repos (décision du
// 2 novembre 2026). Tant qu'il tourne, Android traite l'app comme active : elle peut vibrer et jouer le son même
// quittée ou écran éteint. Type « shortService » : fait pour une tâche courte qui ne doit pas être interrompue, sans
// permission à demander ; Android l'arrête de toute façon au bout de 3 minutes. Il s'attache à la notification de la
// séance, déjà affichée : rien de nouveau n'apparaît.
@AndroidEntryPoint
class RestAlertService : Service() {
    @Inject lateinit var alert: RestEndAlert

    @Inject lateinit var notifier: WorkoutNotifier

    @Inject lateinit var alerter: RestAlerter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Toujours en premier : Android arrête l'app si un service lancé « au premier plan » tarde à s'y mettre.
        val endsAt = intent?.getLongExtra(EXTRA_ENDS_AT, 0L)?.takeIf { it > 0 }?.let(Instant::ofEpochMilli)
        notifier.startForeground(this, endsAt)
        // Une alerte en cours suffit : une deuxième alarme dans les mêmes secondes ne relance rien.
        if (job?.isActive == true) return START_NOT_STICKY
        job = scope.launch {
            try {
                if (endsAt != null) alert.run(endsAt)
            } finally {
                stop()
            }
        }
        return START_NOT_STICKY
    }

    // Délai de 3 minutes dépassé (ne devrait pas arriver, l'alerte dure 4 s) : Android exige un arrêt immédiat.
    override fun onTimeout(startId: Int) = stop()

    override fun onTimeout(startId: Int, fgsType: Int) = stop()

    override fun onDestroy() {
        scope.cancel()
        alerter.releaseWakeLock()
        super.onDestroy()
    }

    // DETACH : la notification de la séance reste affichée, elle redevient une notification ordinaire.
    private fun stop() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    companion object {
        private const val EXTRA_ENDS_AT = "ends_at"

        // Lève ForegroundServiceStartNotAllowedException (une IllegalStateException) si Android refuse le service,
        // par exemple pour une alarme devenue inexacte.
        fun start(context: Context, endsAt: Instant) {
            val intent = Intent(context, RestAlertService::class.java).putExtra(EXTRA_ENDS_AT, endsAt.toEpochMilli())
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
