package com.paulfvt.liftapp.workout.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.paulfvt.liftapp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

// Vibration et son de fin de repos, joués par l'app elle-même pendant que RestAlertService la tient au premier plan
// (une app en arrière-plan n'a plus le droit de jouer un son, et Samsung coupait aussi sa vibration).
// Les deux passent en vibreur et en silencieux (décision du 1er novembre 2026) : la vibration a l'usage « alarme »,
// le son l'usage « multimédia », réglé par le volume de la musique et joué dans les écouteurs s'ils sont branchés
// (décision du 2 novembre 2026 : le volume du réveil ne convenait pas).
@Singleton
class RestAlerter @Inject constructor(@ApplicationContext private val context: Context) {
    private val vibrator: Vibrator
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    private val audioManager = context.getSystemService(AudioManager::class.java)

    // Garde le processeur éveillé, écran éteint, entre l'alarme et la fin du son : sans lui, le téléphone peut se
    // rendormir pendant le décompte et le son partirait en retard. Non compté : un seul appel à release le libère.
    private val wakeLock = context.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LiftApp:restEnd")
        .apply { setReferenceCounted(false) }

    private val chimeAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    // timings : pause, vibration, pause… (restEndVibration).
    fun vibrate(timings: LongArray) {
        val vibrator = vibrator
        if (!vibrator.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(timings, NO_REPEAT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }

    // Repos passé ou prolongé pendant le décompte : la vibration s'arrête.
    fun cancel() = vibrator.cancel()

    // Le ding ding d'1 s (res/raw/rest_end_chime.wav, synthétisé pour l'app), chargé pendant le décompte : le
    // chargement prend près d'une seconde, le son part ainsi pile à la fin. Null s'il est illisible.
    fun prepareChime(): Chime? {
        val player = MediaPlayer()
        return try {
            context.resources.openRawResourceFd(R.raw.rest_end_chime).use { player.setDataSource(it) }
            player.setAudioAttributes(chimeAttributes)
            player.prepare()
            Chime(player)
        } catch (e: IOException) {
            Log.w(TAG, "Son de fin de repos illisible", e)
            player.release()
            null
        }
    }

    inner class Chime(private val player: MediaPlayer) {
        // La musique en cours baisse le temps du son puis reprend. Rend la main une fois le son terminé.
        suspend fun play() {
            val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(chimeAttributes)
                .build()
            audioManager.requestAudioFocus(focus)
            try {
                withTimeoutOrNull(CHIME_TIMEOUT_MS) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        player.setOnCompletionListener { continuation.resume(Unit) }
                        player.start()
                    }
                }
            } finally {
                audioManager.abandonAudioFocusRequest(focus)
            }
        }

        fun release() = player.release()
    }

    // Au plus WAKE_LOCK_TIMEOUT_MS : même si l'alerte plantait, le téléphone pourrait se rendormir.
    fun acquireWakeLock() = wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)

    fun releaseWakeLock() {
        if (wakeLock.isHeld) wakeLock.release()
    }

    private companion object {
        const val TAG = "RestAlerter"
        const val NO_REPEAT = -1
        const val CHIME_TIMEOUT_MS = 3_000L
        const val WAKE_LOCK_TIMEOUT_MS = 15_000L
    }
}
