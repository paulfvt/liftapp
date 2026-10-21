package com.paulfvt.liftapp.profile.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileTest {
    private val today = LocalDate.of(2026, 10, 21)

    @Test
    fun `age only counts a year on the birthday`() {
        assertEquals(27, ageOn(LocalDate.of(1998, 10, 22), today))
        assertEquals(28, ageOn(LocalDate.of(1998, 10, 21), today))
    }

    @Test
    fun `someone born on February 29 gets a year older on March 1 in common years`() {
        val birth = LocalDate.of(2000, 2, 29)
        assertEquals(26, ageOn(birth, LocalDate.of(2027, 2, 28)))
        assertEquals(27, ageOn(birth, LocalDate.of(2027, 3, 1)))
    }

    @Test
    fun `an empty draft is valid and gives an empty profile`() {
        val draft = ProfileDraft()
        assertTrue(draft.errors(today).isEmpty())
        assertEquals(UserProfile(), draft.toProfile())
    }

    @Test
    fun `height accepts a French decimal and rejects typos`() {
        assertTrue(ProfileDraft(height = "180,5").errors(today).isEmpty())
        assertEquals(180.5, ProfileDraft(height = "180,5").toProfile().heightCm!!, 0.0)
        assertEquals(setOf(ProfileError.HEIGHT_INVALID), ProfileDraft(height = "1800").errors(today))
        assertEquals(setOf(ProfileError.HEIGHT_INVALID), ProfileDraft(height = "49").errors(today))
        assertEquals(setOf(ProfileError.HEIGHT_INVALID), ProfileDraft(height = ",").errors(today))
    }

    @Test
    fun `a birth date in the future is refused, today is accepted`() {
        assertEquals(
            setOf(ProfileError.BIRTH_DATE_IN_FUTURE),
            ProfileDraft(birthDate = today.plusDays(1)).errors(today)
        )
        assertTrue(ProfileDraft(birthDate = today).errors(today).isEmpty())
    }

    @Test
    fun `first name is trimmed and blank means not set`() {
        assertEquals("Paul", ProfileDraft(firstName = "  Paul ").toProfile().firstName)
        assertNull(ProfileDraft(firstName = "   ").toProfile().firstName)
        val tooLong = ProfileDraft(firstName = "a".repeat(FIRST_NAME_MAX_LENGTH + 1))
        assertEquals(setOf(ProfileError.FIRST_NAME_TOO_LONG), tooLong.errors(today))
    }

    @Test
    fun `a profile survives the round trip through the draft`() {
        val profile = UserProfile("Paul", 181.0, LocalDate.of(1998, 5, 3), Sex.MALE)
        assertEquals(profile, profile.toDraft().toProfile())
    }
}
