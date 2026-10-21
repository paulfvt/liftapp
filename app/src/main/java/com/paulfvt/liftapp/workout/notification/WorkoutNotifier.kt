package com.paulfvt.liftapp.workout.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.paulfvt.liftapp.MainActivity
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.workout.domain.NextSetNotice
import com.paulfvt.liftapp.workout.domain.REST_ADD_SECONDS
import com.paulfvt.liftapp.workout.domain.SetValues
import com.paulfvt.liftapp.workout.domain.WorkoutNotice
import com.paulfvt.liftapp.workout.domain.distanceUnitFor
import com.paulfvt.liftapp.workout.domain.formatDecimal
import com.paulfvt.liftapp.workout.domain.formatDistance
import com.paulfvt.liftapp.workout.domain.formatDuration
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

    // Canal sans son ni vibration : la notification change à chaque série, seule la fin du repos doit alerter,
    // et cette alerte est jouée à part (RestAlerter) pour respecter les réglages son et vibration de l'app.
    fun createChannel() {
        val channel = NotificationChannel(
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
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // Android 13+ : rien ne s'affiche sans la permission POST_NOTIFICATIONS.
    fun canNotify(): Boolean = manager.areNotificationsEnabled() && hasPermission()

    fun show(notice: WorkoutNotice, now: Instant) {
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.notify(NOTIFICATION_ID, build(notice, now))
    }

    fun cancel() = manager.cancel(NOTIFICATION_ID)

    private fun hasPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    private fun build(notice: WorkoutNotice, now: Instant): Notification {
        val rest = notice.rest?.takeIf { it.isRunning(now) }
        val next = notice.next
        val title = when {
            rest != null -> context.getString(R.string.notification_rest)
            next != null -> next.exercise.name
            else -> notice.workoutName
        }
        val text = when {
            next == null -> context.getString(R.string.notification_all_done)
            rest != null -> context.getString(R.string.notification_next, next.exercise.name, describe(next))
            else -> describe(next)
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

    // « Série 3 sur 4 · 80 kg × 8 », « Échauffement · 40 kg × 12 », « Série 1 sur 3 · 1:30 ».
    private fun describe(next: NextSetNotice): String {
        val position = if (next.type == SetType.WORK) {
            context.getString(R.string.notification_set_position, next.position, next.count)
        } else {
            context.getString(if (next.type == SetType.WARMUP) R.string.set_type_warmup else R.string.set_type_drop)
        }
        val values = describeValues(next.values, next)
        return if (values.isEmpty()) position else "$position · $values"
    }

    private fun describeValues(values: SetValues, next: NextSetNotice): String = when (next.exercise.trackingType) {
        TrackingType.WEIGHT_REPS, TrackingType.REPS -> {
            val reps = values.reps
            val weight = values.weightKg
            when {
                reps == null -> ""
                weight == null -> context.resources.getQuantityString(R.plurals.workout_previous_reps, reps, reps)
                next.exercise.trackingType == TrackingType.REPS && weight > 0 ->
                    context.getString(R.string.notification_weight_reps, "+" + formatDecimal(weight), reps)
                else -> context.getString(R.string.notification_weight_reps, formatDecimal(weight), reps)
            }
        }
        TrackingType.DURATION -> formatDuration(values.durationSec)
        TrackingType.DISTANCE -> listOf(
            formatDistance(values.distanceM, distanceUnitFor(next.exercise.category)),
            formatDuration(values.durationSec)
        ).filter { it.isNotEmpty() }.joinToString(" · ")
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
        const val NOTIFICATION_ID = 1
        const val REQUEST_OPEN = 0
        const val REQUEST_COMPLETE = 1
        const val REQUEST_ADD_REST = 2
    }
}
