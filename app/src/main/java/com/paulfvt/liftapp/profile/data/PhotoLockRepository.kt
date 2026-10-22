package com.paulfvt.liftapp.profile.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.paulfvt.liftapp.profile.domain.PIN_HASH_ITERATIONS
import com.paulfvt.liftapp.profile.domain.lockoutEndsAt
import com.paulfvt.liftapp.profile.domain.pinHash
import com.paulfvt.liftapp.profile.domain.pinMatches
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val SALT_BYTES = 16

data class PhotoLockSettings(
    val isEnabled: Boolean = false,
    val useBiometric: Boolean = false,
    // Erreurs de code depuis le dernier bon code, gardées même si l'app redémarre.
    val failures: Int = 0,
    val lastFailureAt: Instant? = null
)

sealed interface PinCheck {
    data object Correct : PinCheck

    // lockedUntil : attente imposée après trop d'erreurs, null si on peut réessayer tout de suite.
    data class Wrong(val lockedUntil: Instant?) : PinCheck

    data class LockedOut(val until: Instant) : PinCheck
}

// Réglages du verrouillage des photos, dans le DataStore : empreinte et sel du PIN (jamais le PIN lui-même),
// choix du visage ou de l'empreinte, compteur d'erreurs.
@Singleton
class PhotoLockRepository @Inject constructor(
    private val preferences: DataStore<Preferences>,
    private val clock: Clock
) {
    val settings: Flow<PhotoLockSettings> = preferences.data.map { prefs ->
        PhotoLockSettings(
            isEnabled = prefs[HASH] != null,
            useBiometric = prefs[BIOMETRIC] ?: false,
            failures = prefs[FAILURES] ?: 0,
            lastFailureAt = prefs[LAST_FAILURE]?.let(Instant::ofEpochMilli)
        )
    }.distinctUntilChanged()

    // Active le verrouillage, ou change le code s'il l'était déjà. Le calcul de l'empreinte est lent exprès :
    // il se fait hors du fil principal.
    suspend fun setPin(pin: String) {
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val hash = withContext(Dispatchers.Default) { pinHash(pin, salt, PIN_HASH_ITERATIONS) }
        preferences.edit { prefs ->
            prefs[HASH] = hash.encode()
            prefs[SALT] = salt.encode()
            prefs[ITERATIONS] = PIN_HASH_ITERATIONS
            prefs.remove(FAILURES)
            prefs.remove(LAST_FAILURE)
        }
    }

    suspend fun check(pin: String): PinCheck {
        val prefs = preferences.data.first()
        val hash = prefs[HASH]?.decode() ?: return PinCheck.Correct
        val salt = prefs[SALT]?.decode() ?: return PinCheck.Correct
        val iterations = prefs[ITERATIONS] ?: PIN_HASH_ITERATIONS
        val failures = prefs[FAILURES] ?: 0
        val now = clock.instant()
        lockoutEndsAt(failures, prefs[LAST_FAILURE]?.let(Instant::ofEpochMilli), now)?.let {
            return PinCheck.LockedOut(it)
        }

        val correct = withContext(Dispatchers.Default) { pinMatches(pin, salt, iterations, hash) }
        if (correct) {
            clearFailures()
            return PinCheck.Correct
        }
        preferences.edit { edited ->
            edited[FAILURES] = failures + 1
            edited[LAST_FAILURE] = now.toEpochMilli()
        }
        return PinCheck.Wrong(lockoutEndsAt(failures + 1, now, now))
    }

    // Déverrouillage par le visage, l'empreinte ou le verrouillage du téléphone : les erreurs de code sont oubliées.
    suspend fun clearFailures() {
        preferences.edit { prefs ->
            prefs.remove(FAILURES)
            prefs.remove(LAST_FAILURE)
        }
    }

    suspend fun setBiometric(enabled: Boolean) {
        preferences.edit { it[BIOMETRIC] = enabled }
    }

    suspend fun disable() {
        preferences.edit { prefs ->
            prefs.remove(HASH)
            prefs.remove(SALT)
            prefs.remove(ITERATIONS)
            prefs.remove(BIOMETRIC)
            prefs.remove(FAILURES)
            prefs.remove(LAST_FAILURE)
        }
    }

    private fun ByteArray.encode(): String = Base64.getEncoder().encodeToString(this)

    private fun String.decode(): ByteArray = Base64.getDecoder().decode(this)

    private companion object {
        val HASH = stringPreferencesKey("photo_lock_hash")
        val SALT = stringPreferencesKey("photo_lock_salt")
        val ITERATIONS = intPreferencesKey("photo_lock_iterations")
        val BIOMETRIC = booleanPreferencesKey("photo_lock_biometric")
        val FAILURES = intPreferencesKey("photo_lock_failures")
        val LAST_FAILURE = longPreferencesKey("photo_lock_last_failure")
    }
}
