package com.paulfvt.liftapp.profile.domain

import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.min

const val PIN_MIN_LENGTH = 4
const val PIN_MAX_LENGTH = 8

fun isValidPin(pin: String): Boolean = pin.length in PIN_MIN_LENGTH..PIN_MAX_LENGTH && pin.all(Char::isDigit)

// Empreinte du PIN : PBKDF2 avec un sel aléatoire, jamais le PIN en clair. Lente exprès (120 000 tours) : essayer
// tous les codes sur une copie des données prendrait des heures au lieu de quelques secondes.
const val PIN_HASH_ITERATIONS = 120_000
private const val PIN_HASH_BITS = 256

fun pinHash(pin: String, salt: ByteArray, iterations: Int = PIN_HASH_ITERATIONS): ByteArray {
    val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, PIN_HASH_BITS)
    return try {
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    } finally {
        spec.clearPassword()
    }
}

// Comparaison en temps constant : la durée ne révèle pas combien d'octets sont justes.
fun pinMatches(pin: String, salt: ByteArray, iterations: Int, expected: ByteArray): Boolean =
    MessageDigest.isEqual(pinHash(pin, salt, iterations), expected)

// Après 5 erreurs, attente de 30 s qui double à chaque nouvelle erreur, 15 min au plus.
const val FREE_PIN_ATTEMPTS = 5
private const val FIRST_LOCKOUT_SECONDS = 30L
private const val MAX_LOCKOUT_SECONDS = 15 * 60L
private const val MAX_DOUBLINGS = 10

fun lockoutDuration(failures: Int): Duration {
    if (failures < FREE_PIN_ATTEMPTS) return Duration.ZERO
    val doublings = min(failures - FREE_PIN_ATTEMPTS, MAX_DOUBLINGS)
    return Duration.ofSeconds(min(FIRST_LOCKOUT_SECONDS shl doublings, MAX_LOCKOUT_SECONDS))
}

// Fin de l'attente imposée, ou null si un nouvel essai est permis tout de suite.
fun lockoutEndsAt(failures: Int, lastFailureAt: Instant?, now: Instant): Instant? {
    val end = lastFailureAt?.plus(lockoutDuration(failures)) ?: return null
    return end.takeIf { failures >= FREE_PIN_ATTEMPTS && it.isAfter(now) }
}
