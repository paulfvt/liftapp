package com.paulfvt.liftapp.workout.notification

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Vibration de fin de repos. Elle est jouée par l'app plutôt que par un canal de notification : un canal garde la
// vibration choisie à sa création, alors que l'app doit pouvoir la couper à tout moment. Usage « alarme » : elle se
// fait sentir même téléphone en silencieux (décision du 1er novembre 2026), comme le son de fin.
// Le son, lui, passe par une notification (WorkoutNotifier.showRestEnd) : depuis Android 15, un son joué par une app
// en arrière-plan est coupé, ce que l'essai sur l'émulateur a confirmé écran verrouillé.
@Singleton
class RestAlerter @Inject constructor(@ApplicationContext private val context: Context) {
    private val vibrator: Vibrator
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    // timings : pause, vibration, pause… (restEndVibration).
    fun vibrate(timings: LongArray) {
        val vibrator = vibrator
        if (!vibrator.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(timings, NO_REPEAT)
        // Usage « alarme » : Android peut ignorer une vibration d'app en arrière-plan qui n'indique pas d'usage.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }

    // Repos passé ou prolongé pendant le décompte : la vibration s'arrête.
    fun cancel() = vibrator.cancel()

    private companion object {
        const val NO_REPEAT = -1
    }
}
