package com.paulfvt.liftapp.workout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.paulfvt.liftapp.core.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Reçoit l'alarme 3 s avant la fin du repos et confie l'alerte à RestAlertService : une alarme exacte fait partie des
// cas où Android permet de lancer un service de premier plan depuis l'arrière-plan.
@AndroidEntryPoint
class RestEndReceiver : BroadcastReceiver() {
    @Inject lateinit var alert: RestEndAlert

    @Inject lateinit var alerter: RestAlerter

    @Inject lateinit var sync: WorkoutSessionSync

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        // goAsync : le récepteur reste en vie le temps de lire la base.
        val pending = goAsync()
        scope.launch {
            try {
                val endsAt = alert.dueRestEnd()
                if (endsAt == null) {
                    sync.refresh()
                    return@launch
                }
                // Le réveil est pris ici, avant que l'alarme ne relâche le sien, et rendu par le service à la fin.
                alerter.acquireWakeLock()
                try {
                    RestAlertService.start(context, endsAt)
                } catch (e: IllegalStateException) {
                    // Service refusé (alarme devenue inexacte) : l'alerte est jouée quand même, mais Android peut
                    // couper le son d'une app en arrière-plan.
                    Log.w(TAG, "Service d'alerte refusé", e)
                    try {
                        alert.run(endsAt)
                    } finally {
                        alerter.releaseWakeLock()
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "RestEndReceiver"
    }
}
