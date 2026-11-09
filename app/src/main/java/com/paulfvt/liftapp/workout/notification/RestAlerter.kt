package com.paulfvt.liftapp.workout.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.workout.domain.RestBeat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

// Vibration et sons de fin de repos, joués par l'app elle-même pendant que RestAlertService la tient au premier plan
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
    // rendormir pendant le décompte et les sons partiraient en retard. Non compté : un seul appel à release le libère.
    private val wakeLock = context.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LiftApp:restEnd")
        .apply { setReferenceCounted(false) }

    private val soundAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    // Une impulsion par temps fort, et non plus une seule vibration pour tout le décompte : Android annule la vibration
    // en cours quand on éteint l'écran (bouton latéral), et la vibration longue de fin était perdue avec elle (vu sur
    // le S25+ le 9 novembre 2026). Les impulsions suivantes, lancées écran éteint, ne sont pas annulées.
    fun pulse(durationMs: Long) {
        val vibrator = vibrator
        if (!vibrator.hasVibrator()) return
        val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }

    // Repos passé ou prolongé pendant le décompte : la vibration en cours s'arrête.
    fun cancel() = vibrator.cancel()

    // Petit ding du décompte et ding ding de fin (res/raw, synthétisés pour l'app), chargés en mémoire dès l'alarme :
    // ils partent ensuite sans délai, pile à la seconde. Null s'ils ne se chargent pas à temps.
    suspend fun loadSounds(): RestSounds? {
        val pool = SoundPool.Builder().setMaxStreams(MAX_STREAMS).setAudioAttributes(soundAttributes).build()
        var tickId = 0
        var endId = 0
        val loaded = withTimeoutOrNull(LOAD_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val loadedCount = AtomicInteger()
                pool.setOnLoadCompleteListener { _, _, status ->
                    val result = when {
                        status != 0 -> false
                        loadedCount.incrementAndGet() == SOUND_COUNT -> true
                        else -> null
                    }
                    if (result != null && continuation.isActive) continuation.resume(result)
                }
                tickId = pool.load(context, R.raw.rest_tick, 1)
                endId = pool.load(context, R.raw.rest_end_chime, 1)
            }
        }
        if (loaded != true) {
            Log.w(TAG, "Sons de fin de repos non chargés")
            pool.release()
            return null
        }
        return RestSounds(pool, tickId, endId)
    }

    inner class RestSounds(private val pool: SoundPool, private val tickId: Int, private val endId: Int) {
        private var focus: AudioFocusRequest? = null
        private var playingUntil = 0L

        // La musique en cours baisse du premier ding jusqu'à la fin du son, puis reprend.
        fun play(beat: RestBeat) {
            if (focus == null) {
                focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(soundAttributes)
                    .build()
                    .also { audioManager.requestAudioFocus(it) }
            }
            pool.play(if (beat.isEnd) endId else tickId, 1f, 1f, 1, 0, 1f)
            val length = if (beat.isEnd) END_SOUND_MS else TICK_SOUND_MS
            playingUntil = SystemClock.uptimeMillis() + length
        }

        // Attend la fin du dernier son lancé avant de tout libérer, car libérer coupe le son en cours. Alerte
        // interrompue (service arrêté) : tout est libéré aussitôt.
        suspend fun close() {
            try {
                delay((playingUntil - SystemClock.uptimeMillis()).coerceAtLeast(0))
            } finally {
                focus?.let { audioManager.abandonAudioFocusRequest(it) }
                pool.release()
            }
        }
    }

    // Au plus WAKE_LOCK_TIMEOUT_MS : même si l'alerte plantait, le téléphone pourrait se rendormir.
    fun acquireWakeLock() = wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)

    fun releaseWakeLock() {
        if (wakeLock.isHeld) wakeLock.release()
    }

    private companion object {
        const val TAG = "RestAlerter"
        const val MAX_STREAMS = 2
        const val SOUND_COUNT = 2

        // Reste sous les 2 s de préparation entre l'alarme et le premier ding (REST_ALARM_ADVANCE).
        const val LOAD_TIMEOUT_MS = 1_500L
        const val TICK_SOUND_MS = 200L
        const val END_SOUND_MS = 1_000L
        const val WAKE_LOCK_TIMEOUT_MS = 15_000L
    }
}
