package com.paulfvt.liftapp.workout.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
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

    // Deux canaux. « Séance en cours », sans son ni vibration : la notification change à chaque série.
    // « Fin du repos » : le ding ding d'1 s (res/raw/rest_end_chime.wav, synthétisé pour l'app) joué avec l'usage
    // « alarme », donc entendu même téléphone en vibreur ou en silencieux (décision du 1er novembre 2026), sans
    // vibration (jouée par RestAlerter, pour pouvoir la couper depuis l'app). Le son d'un canal ne peut plus changer
    // après sa création : l'ancien canal, au son de notification du téléphone, est supprimé.
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
        val restEnd = NotificationChannel(
            REST_END_CHANNEL_ID,
            context.getString(R.string.notification_channel_rest_end),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_rest_end_description)
            setSound(
                "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.rest_end_chime}".toUri(),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val system = context.getSystemService(NotificationManager::class.java)
        system.deleteNotificationChannel(LEGACY_REST_END_CHANNEL_ID)
        system.createNotificationChannels(listOf(workout, restEnd))
    }

    // Son de fin de repos : notification brève qui disparaît d'elle-même après quelques secondes. Catégorie « alarme
    // ou minuteur » : depuis Android 15, une notification d'une autre catégorie perd l'usage « alarme » de son canal
    // et son son est coupé en vibreur (vérifié sur l'émulateur Android 16).
    fun showRestEnd(notice: WorkoutNotice, units: Units) {
        val next = notice.next
        val notification = NotificationCompat.Builder(context, REST_END_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.notification_rest_end))
            .setContentText(
                next?.let {
                    context.getString(
                        R.string.notification_next,
                        it.exercise.name,
                        describeNextSet(context, it, units.weight)
                    )
                }
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openWorkoutIntent())
            .setAutoCancel(true)
            .setTimeoutAfter(REST_END_TIMEOUT_MS)
            .build()
        post(REST_END_NOTIFICATION_ID, notification)
    }

    // Android 13+ : rien ne s'affiche sans la permission POST_NOTIFICATIONS.
    fun canNotify(): Boolean = manager.areNotificationsEnabled() && hasPermission()

    fun show(notice: WorkoutNotice, now: Instant, units: Units) = post(NOTIFICATION_ID, build(notice, now, units))

    fun cancel() {
        manager.cancel(NOTIFICATION_ID)
        manager.cancel(REST_END_NOTIFICATION_ID)
    }

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
        const val REST_END_CHANNEL_ID = "rest_end_chime"
        const val LEGACY_REST_END_CHANNEL_ID = "rest_end"
        const val NOTIFICATION_ID = 1
        const val REST_END_NOTIFICATION_ID = 2
        const val REST_END_TIMEOUT_MS = 10_000L
        const val REQUEST_OPEN = 0
        const val REQUEST_COMPLETE = 1
        const val REQUEST_ADD_REST = 2
    }
}
