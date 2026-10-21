package com.paulfvt.liftapp.workout.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Vibration de fin de repos. Elle est jouée par l'app plutôt que par un canal de notification : un canal garde la
// vibration choisie à sa création, alors que l'app doit pouvoir la couper à tout moment. Rien en mode silencieux.
// Le son, lui, passe par une notification (WorkoutNotifier.showRestEnd) : depuis Android 15, un son joué par une app
// en arrière-plan est coupé, ce que l'essai sur l'émulateur a confirmé écran verrouillé.
@Singleton
class RestAlerter @Inject constructor(@ApplicationContext private val context: Context) {
    fun vibrate() {
        if (context.getSystemService(AudioManager::class.java).ringerMode == AudioManager.RINGER_MODE_SILENT) return
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

    private companion object {
        // Deux vibrations de 400 ms séparées par 200 ms.
        val VIBRATION_PATTERN = longArrayOf(0, 400, 200, 400)
        const val NO_REPEAT = -1
    }
}
