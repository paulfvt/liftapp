package com.paulfvt.liftapp.workout.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

// Alarme de fin de repos : réveille l'app pour l'alerte (vibration, son), même écran verrouillé et en veille.
// Exacte si l'utilisateur a autorisé « Alarmes et rappels » (pas accordé d'office depuis Android 14) ;
// sinon inexacte, et Android peut alors la retarder.
@Singleton
class RestAlarmScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    // Même PendingIntent à chaque fois : programmer une nouvelle fin remplace l'alarme précédente.
    fun schedule(endsAt: Instant) {
        val trigger = endsAt.toEpochMilli()
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent())
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent())
            }
        } catch (e: SecurityException) {
            // Permission retirée entre la vérification et l'appel : on se rabat sur l'alarme inexacte.
            Log.w(TAG, "Alarme exacte refusée", e)
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent())
        }
    }

    fun cancel() = alarms.cancel(pendingIntent())

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, RestEndReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private companion object {
        const val TAG = "RestAlarmScheduler"
    }
}
