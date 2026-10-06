package com.paulfvt.liftapp.exercise.data

import com.paulfvt.liftapp.core.database.entity.ExerciseMuscleEntity
import com.paulfvt.liftapp.core.database.entity.ExerciseWithMuscles
import com.paulfvt.liftapp.core.model.Equipment
import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.MuscleRole
import com.paulfvt.liftapp.core.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseMappersTest {
    private val dto = CatalogExerciseDto(
        key = "Barbell_Squat",
        name = "Squat barre",
        equipment = Equipment.BARBELL,
        category = ExerciseCategory.FREE_WEIGHT,
        trackingType = TrackingType.WEIGHT_REPS,
        primaryMuscles = listOf(Muscle.QUADRICEPS),
        secondaryMuscles = listOf(Muscle.GLUTES, Muscle.QUADRICEPS),
        instructions = listOf("Placez la barre.", "Descendez."),
        imageCount = 2
    )

    @Test
    fun `new catalog exercise starts visible and without note`() {
        val entity = dto.toEntity(existing = null)
        assertEquals(0L, entity.id)
        assertFalse(entity.isHidden)
        assertFalse(entity.isCustom)
        assertNull(entity.note)
        assertEquals("Placez la barre.\nDescendez.", entity.instructions)
    }

    @Test
    fun `catalog update keeps the user's note and hidden choice`() {
        val existing = dto.toEntity(existing = null).copy(id = 42, note = "Siège cran 4", isHidden = true)
        val updated = dto.copy(name = "Squat à la barre").toEntity(existing)
        assertEquals(42L, updated.id)
        assertEquals("Siège cran 4", updated.note)
        assertEquals(true, updated.isHidden)
        assertEquals("Squat à la barre", updated.name)
    }

    @Test
    fun `a muscle listed as both primary and secondary keeps only the primary role`() {
        val muscles = dto.toMuscleEntities(exerciseId = 7)
        assertEquals(
            listOf(
                ExerciseMuscleEntity(7, Muscle.QUADRICEPS, MuscleRole.PRIMARY),
                ExerciseMuscleEntity(7, Muscle.GLUTES, MuscleRole.SECONDARY)
            ),
            muscles
        )
    }

    @Test
    fun `domain model splits muscles by role and instructions by line`() {
        val row = ExerciseWithMuscles(dto.toEntity(existing = null).copy(id = 7), dto.toMuscleEntities(7))
        val exercise = row.toDomain()
        assertEquals(listOf(Muscle.QUADRICEPS), exercise.primaryMuscles)
        assertEquals(listOf(Muscle.GLUTES), exercise.secondaryMuscles)
        assertEquals(listOf("Placez la barre.", "Descendez."), exercise.instructions)
    }

    @Test
    fun `custom exercise without instructions has an empty list`() {
        val row = ExerciseWithMuscles(dto.toEntity(existing = null).copy(instructions = null), emptyList())
        assertEquals(emptyList<String>(), row.toDomain().instructions)
    }
}
