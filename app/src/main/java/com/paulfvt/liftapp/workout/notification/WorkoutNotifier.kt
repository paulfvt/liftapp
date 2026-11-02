package com.paulfvt.liftapp.workout.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.paulfvt.liftapp.MainActivity
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.domain.Units
import com.paulfvt.liftapp.workout.domain.REST_ADD_SECONDS
import com.paulfvt.liftapp.workout.domain.WorkoutNotice
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

// Notification de séance, visible écran verrouillé pendant toute la séance. Le compte à rebours est dessiné par
// Android à partir de l'heure de fin (setWhen + chronomètre) : il reste juste sans que l'app ait à tourner.
// Sur Android 16, elle demande à devenir une Live Update (puce dans la barre d'état, en tête de l'écran verrouillé) ;
// le système décide, sinon elle reste une notification classique.
@Singleton
class WorkoutNotifier @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    // Un seul canal, « Séance en cours », sans son ni vibration : la notification change à chaque série. La fin du
    // repos n'a plus de canal : son et vibration sont joués par l'app (RestAlertService), car Samsung coupait le son
    // de la notification. Les anciens canaux « Fin du repos » sont supprimés.
    fun createChannels() {
        val workout = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_workout),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_workout_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val system = context.getSystemService(NotificationManager::class.java)
        LEGACY_CHANNEL_IDS.forEach(system::deleteNotificationChannel)
        system.createNotificationChannel(workout)
    }

    // Met RestAlertService au premier plan sur la notification de la séance, déjà affichée : elle ne change pas. Si
    // elle ne l'est pas (notifications refusées), une notification minimale la remplace, qu'Android masquera alors.
    fun startForeground(service: Service, restEndsAt: Instant?) {
        val notification = manager.activeNotifications.firstOrNull { it.id == NOTIFICATION_ID }?.notification
            ?: NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle(context.getString(R.string.notification_rest))
                .setSilent(true)
                .setContentIntent(openWorkoutIntent())
                .apply {
                    if (restEndsAt != null) {
                        setWhen(restEndsAt.toEpochMilli()).setUsesChronometer(true).setChronometerCountDown(true)
                    }
                }
                .build()
        // « shortService » n'existe que depuis Android 14 ; avant, le service n'a pas de type à déclarer.
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
        } else {
            0
        }
        ServiceCompat.startForeground(service, NOTIFICATION_ID, notification, type)
    }

    // Android 13+ : rien ne s'affiche sans la permission POST_NOTIFICATIONS.
    fun canNotify(): Boolean = manager.areNotificationsEnabled() && hasPermission()

    fun show(notice: WorkoutNotice, now: Instant, units: Units) = post(NOTIFICATION_ID, build(notice, now, units))

    fun cancel() = manager.cancel(NOTIFICATION_ID)

    private fun hasPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    // La vérification de permission est écrite ici en entier (et non via hasPermission) : c'est ce que l'analyse
    // Android Lint sait reconnaître avant un appel à notify.
    private fun post(id: Int, notification: Notification) {
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.notify(id, notification)
    }

    private fun build(notice: WorkoutNotice, now: Instant, units: Units): Notification {
        val rest = notice.rest?.takeIf { it.isRunning(now) }
        val next = notice.next
        val title = when {
            rest != null -> context.getString(R.string.notification_rest)
            next != null -> next.exercise.name
            else -> notice.workoutName
        }
        val text = when {
            next == null -> context.getString(R.string.notification_all_done)
            rest != null -> context.getString(
                R.string.notification_next,
                next.exercise.name,
                describeNextSet(context, next, units.weight)
            )
            else -> describeNextSet(context, next, units.weight)
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openWorkoutIntent())
            .setRequestPromotedOngoing(true)
            .setShowWhen(true)
            .setUsesChronometer(true)
        if (rest != null) {
            // Compte à rebours jusqu'à la fin du repos.
            builder.setWhen(rest.endsAt.toEpochMilli()).setChronometerCountDown(true)
        } else {
            // Hors repos : durée de la séance, comme le bandeau EN DIRECT.
            builder.setWhen(notice.startedAt.toEpochMilli()).setChronometerCountDown(false)
        }
        if (next != null && next.canComplete) {
            builder.addAction(
                R.drawable.ic_check,
                context.getString(R.string.notification_action_complete),
                actionIntent(WorkoutActionReceiver.ACTION_COMPLETE_SET, REQUEST_COMPLETE) {
                    putExtra(WorkoutActionReceiver.EXTRA_SET_ID, next.setId)
                }
            )
        }
        if (rest != null) {
            builder.addAction(
                R.drawable.ic_add,
                context.getString(R.string.rest_add, REST_ADD_SECONDS),
                actionIntent(WorkoutActionReceiver.ACTION_ADD_REST, REQUEST_ADD_REST)
            )
        }
        return builder.build()
    }

    // Ouvre l'app sur la séance. SINGLE_TOP : si l'app est déjà ouverte, elle est ramenée au premier plan
    // et reçoit l'intention dans onNewIntent au lieu d'être recréée.
    private fun openWorkoutIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN,
        Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_WORKOUT)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun actionIntent(action: String, requestCode: Int, extras: Intent.() -> Unit = {}): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, WorkoutActionReceiver::class.java).setAction(action).apply(extras),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private companion object {
        const val CHANNEL_ID = "workout"
        val LEGACY_CHANNEL_IDS = listOf("rest_end", "rest_end_chime")
        const val NOTIFICATION_ID = 1
        const val REQUEST_OPEN = 0
        const val REQUEST_COMPLETE = 1
        const val REQUEST_ADD_REST = 2
    }
}
