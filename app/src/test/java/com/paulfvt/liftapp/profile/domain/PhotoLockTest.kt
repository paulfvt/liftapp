package com.paulfvt.liftapp.profile.domain

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoLockTest {
    // Peu de tours dans les tests : seule la logique compte ici, pas la lenteur voulue en production.
    private val iterations = 1_000
    private val salt = ByteArray(16) { it.toByte() }

    @Test
    fun `a pin has 4 to 8 digits`() {
        assertTrue(isValidPin("1234"))
        assertTrue(isValidPin("12345678"))
        assertFalse(isValidPin("123"))
        assertFalse(isValidPin("123456789"))
        assertFalse(isValidPin("12a4"))
        assertFalse(isValidPin(""))
    }

    @Test
    fun `the same pin and salt always give the same hash, another pin or salt does not`() {
        val hash = pinHash("2580", salt, iterations)
        assertArrayEquals(hash, pinHash("2580", salt, iterations))
        assertFalse(hash.contentEquals(pinHash("2581", salt, iterations)))
        assertFalse(hash.contentEquals(pinHash("2580", ByteArray(16), iterations)))
        assertEquals(32, hash.size)
    }

    @Test
    fun `pin check accepts only the right code`() {
        val hash = pinHash("2580", salt, iterations)
        assertTrue(pinMatches("2580", salt, iterations, hash))
        assertFalse(pinMatches("0852", salt, iterations, hash))
    }

    @Test
    fun `five free attempts, then a wait that doubles up to fifteen minutes`() {
        assertEquals(Duration.ZERO, lockoutDuration(4))
        assertEquals(Duration.ofSeconds(30), lockoutDuration(5))
        assertEquals(Duration.ofSeconds(60), lockoutDuration(6))
        assertEquals(Duration.ofSeconds(120), lockoutDuration(7))
        assertEquals(Duration.ofMinutes(15), lockoutDuration(12))
        assertEquals(Duration.ofMinutes(15), lockoutDuration(1_000))
    }

    @Test
    fun `the wait ends after its duration`() {
        val failedAt = Instant.parse("2026-10-22T10:00:00Z")
        assertNull(lockoutEndsAt(4, failedAt, failedAt))
        assertEquals(failedAt.plusSeconds(30), lockoutEndsAt(5, failedAt, failedAt.plusSeconds(10)))
        assertNull(lockoutEndsAt(5, failedAt, failedAt.plusSeconds(30)))
        assertNull(lockoutEndsAt(5, null, failedAt))
    }
}
