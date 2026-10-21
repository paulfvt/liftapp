package com.paulfvt.liftapp.workout.notification

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.paulfvt.liftapp.workout.data.WorkoutSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Alerte de fin de repos. Elle est jouée par l'app plutôt que par le canal de notification : un canal garde le son et
// la vibration choisis à sa création, alors que l'app doit pouvoir couper l'un ou l'autre à tout moment.
// Le mode du téléphone est respecté : pas de son en vibreur, en silencieux ni en « Ne pas déranger », pas de
// vibration en silencieux.
@Singleton
class RestAlerter @Inject constructor(@ApplicationContext private val context: Context) {
    fun alert(settings: WorkoutSettings) {
        val ringerMode = context.getSystemService(AudioManager::class.java).ringerMode
        if (settings.restVibration && ringerMode != AudioManager.RINGER_MODE_SILENT) vibrate()
        val filter = context.getSystemService(NotificationManager::class.java).currentInterruptionFilter
        val doNotDisturb = filter != NotificationManager.INTERRUPTION_FILTER_ALL
        if (settings.restSound && ringerMode == AudioManager.RINGER_MODE_NORMAL && !doNotDisturb) playSound()
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        if (!vibrator.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, NO_REPEAT)
        // Usage « alarme » : Android peut ignorer une vibration d'app en arrière-plan qui n'indique pas d'usage.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }

    // Son de notification choisi par l'utilisateur dans les réglages du téléphone.
    private fun playSound() {
        val ringtone = RingtoneManager.getRingtone(context, Settings.System.DEFAULT_NOTIFICATION_URI) ?: return
        ringtone.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        ringtone.play()
    }

    private companion object {
        // Deux vibrations de 400 ms séparées par 200 ms.
        val VIBRATION_PATTERN = longArrayOf(0, 400, 200, 400)
        const val NO_REPEAT = -1
    }
}
