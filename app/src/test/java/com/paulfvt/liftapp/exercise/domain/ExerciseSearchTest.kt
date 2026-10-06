package com.paulfvt.liftapp.exercise.domain

import com.paulfvt.liftapp.core.model.Equipment
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.MuscleGroup
import com.paulfvt.liftapp.core.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSearchTest {
    private fun exercise(
        name: String = "Squat barre",
        equipment: Equipment = Equipment.BARBELL,
        primary: List<Muscle> = listOf(Muscle.QUADRICEPS),
        secondary: List<Muscle> = emptyList(),
        isHidden: Boolean = false
    ) = Exercise(
        id = 1,
        catalogKey = null,
        name = name,
        equipment = equipment,
        category = ExerciseCategory.FREE_WEIGHT,
        trackingType = TrackingType.WEIGHT_REPS,
        primaryMuscles = primary,
        secondaryMuscles = secondary,
        instructions = emptyList(),
        imageCount = 0,
        isCustom = false,
        isHidden = isHidden,
        note = null
    )

    @Test
    fun `normalize removes accents, case and punctuation`() {
        assertEquals("developpe couche", normalizeForSearch("Développé couché"))
        assertEquals("ecarte a la poulie haute", normalizeForSearch("  Écarté à la poulie (haute) "))
        assertEquals("soeur", normalizeForSearch("Sœur"))
    }

    @Test
    fun `search ignores accents and word order`() {
        val benchPress = exercise(name = "Développé couché")
        assertTrue(benchPress.matches(ExerciseQuery(text = "developpe")))
        assertTrue(benchPress.matches(ExerciseQuery(text = "COUCHÉ dév")))
        assertFalse(benchPress.matches(ExerciseQuery(text = "incliné")))
    }

    @Test
    fun `empty query matches every visible exercise`() {
        assertTrue(exercise().matches(ExerciseQuery()))
        assertTrue(exercise().matches(ExerciseQuery(text = "   ")))
    }

    @Test
    fun `hidden exercises only appear when requested`() {
        val hidden = exercise(isHidden = true)
        assertFalse(hidden.matches(ExerciseQuery()))
        assertTrue(hidden.matches(ExerciseQuery(showHidden = true)))
    }

    @Test
    fun `legs group filter matches any leg muscle as primary`() {
        val legs = ExerciseQuery(muscle = MuscleFilter.Group(MuscleGroup.LEGS))
        assertTrue(exercise(primary = listOf(Muscle.QUADRICEPS)).matches(legs))
        assertTrue(exercise(primary = listOf(Muscle.HAMSTRINGS)).matches(legs))
        assertFalse(exercise(primary = listOf(Muscle.CHEST)).matches(legs))
    }

    @Test
    fun `muscle filter ignores secondary muscles`() {
        val bench = exercise(primary = listOf(Muscle.CHEST), secondary = listOf(Muscle.TRICEPS))
        assertFalse(bench.matches(ExerciseQuery(muscle = MuscleFilter.Single(Muscle.TRICEPS))))
        assertTrue(bench.matches(ExerciseQuery(muscle = MuscleFilter.Single(Muscle.CHEST))))
    }

    @Test
    fun `filters combine with search text`() {
        val query = ExerciseQuery(
            text = "squat",
            muscle = MuscleFilter.Group(MuscleGroup.LEGS),
            equipment = Equipment.BARBELL
        )
        assertTrue(exercise(name = "Squat barre").matches(query))
        assertFalse(exercise(name = "Squat goblet", equipment = Equipment.KETTLEBELL).matches(query))
    }
}
