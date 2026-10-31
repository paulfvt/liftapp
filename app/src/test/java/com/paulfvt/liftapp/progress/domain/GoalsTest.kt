package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.domain.WeightUnit
import com.paulfvt.liftapp.core.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalsTest {
    private val goal = TieredGoal(
        id = 1,
        exerciseId = 1,
        bronzeKg = 95.0,
        silverKg = 100.0,
        goldKg = 105.0,
        createdAt = at("2026-10-10")
    )

    @Test
    fun `bronze and silver are proposed at a third and two thirds of the way, to the half kilo`() {
        assertEquals(93.5 to 96.5, suggestedTiers(90.0, 100.0))
        assertEquals(33.5 to 66.5, suggestedTiers(0.0, 100.0))
    }

    @Test
    fun `a goal aims above the current estimated one rep max with increasing tiers`() {
        assertNull(GoalDraft("95", "100", "105").problem(90.0))
        assertNull(GoalDraft("92,5", "97,5", "102,5").problem(90.0))
        assertEquals(GoalError.NOT_ABOVE_CURRENT, GoalDraft("90", "100", "105").problem(90.0))
        assertEquals(GoalError.NOT_INCREASING, GoalDraft("95", "95", "105").problem(90.0))
        assertEquals(GoalError.NOT_INCREASING, GoalDraft("95", "110", "105").problem(90.0))
        assertEquals(GoalError.INVALID_NUMBER, GoalDraft("95", "", "105").problem(90.0))
        assertEquals(GoalError.INVALID_NUMBER, GoalDraft("95", "100", "2000").problem(90.0))
    }

    @Test
    fun `each tier is dated by the first session reaching it`() {
        // 1RM estimés : 99 le 12 (bronze), 92,5 le 15, puis 90 × (1 + 5/30) = 105 le 19 (argent et or).
        val sets = listOf(
            performed("2026-10-12", 90.0, 3),
            performed("2026-10-15", 92.5, 1),
            performed("2026-10-19", 90.0, 5)
        )
        val reached = newlyReachedTiers(goal, sets)
        assertEquals(at("2026-10-12"), reached[GoalTier.BRONZE])
        assertEquals(at("2026-10-19"), reached[GoalTier.SILVER])
        assertEquals(at("2026-10-19"), reached[GoalTier.GOLD])
    }

    @Test
    fun `a reached tier keeps its date and warm-ups never count`() {
        val alreadyBronze = goal.copy(reachedAt = mapOf(GoalTier.BRONZE to at("2026-10-11")))
        val sets = listOf(performed("2026-10-12", 90.0, 3), performed("2026-10-13", 120.0, 5, SetType.WARMUP))
        val reached = newlyReachedTiers(alreadyBronze, sets)
        assertTrue(reached.isEmpty())
        assertEquals(GoalTier.SILVER, alreadyBronze.nextTier)
    }

    @Test
    fun `a tier reached by an edited old session is dated at the goal creation`() {
        val reached = newlyReachedTiers(goal, listOf(performed("2026-10-01", 100.0, 1)))
        assertEquals(at("2026-10-10"), reached[GoalTier.BRONZE])
    }

    @Test
    fun `the bar goes from the one rep max at creation to gold`() {
        val sets = listOf(performed("2026-10-05", 90.0, 1), performed("2026-10-20", 97.5, 1))
        val status = goalStatus(goal, sets)

        assertEquals(90.0, status.startKg, 0.0)
        assertEquals(97.5, status.bestKg, 0.0)
        assertEquals(0.5f, status.progress, 1e-6f)
        assertEquals(1f, status.fractionOf(110.0), 0f)
        assertFalse(goal.isCompleted)
    }

    @Test
    fun `tiers typed in pounds are checked and stored in kilos`() {
        val draft = GoalDraft("220", "230", "240")
        val tiers = draft.parsed(WeightUnit.LB)!!
        assertEquals(99.79, tiers.bronzeKg, 0.01)
        assertEquals(108.86, tiers.goldKg, 0.01)
        // 1RM actuel de 100 kg (220,5 lb) : un bronze à 220 lb est en dessous.
        assertEquals(GoalError.NOT_ABOVE_CURRENT, draft.problem(100.0, WeightUnit.LB))
    }
}
