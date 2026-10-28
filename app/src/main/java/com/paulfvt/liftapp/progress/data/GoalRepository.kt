package com.paulfvt.liftapp.progress.data

import androidx.room.withTransaction
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.database.entity.GoalEntity
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.GoalMetric
import com.paulfvt.liftapp.core.model.WorkoutStatus
import com.paulfvt.liftapp.exercise.data.ExerciseRepository
import com.paulfvt.liftapp.progress.domain.GoalTier
import com.paulfvt.liftapp.progress.domain.GoalTiers
import com.paulfvt.liftapp.progress.domain.TieredGoal
import com.paulfvt.liftapp.progress.domain.newlyReachedTiers
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Palier tout juste atteint, avec son exercice pour le résumé de fin de séance.
data class ReachedTier(val exercise: Exercise, val goal: TieredGoal, val tier: GoalTier)

// Objectifs à paliers sur le 1RM estimé. Les paliers atteints sont enregistrés une fois pour toutes.
@Singleton
class GoalRepository @Inject constructor(
    private val database: LiftDatabase,
    private val exercises: ExerciseRepository,
    private val clock: Clock
) {
    private val dao = database.motivationDao()
    private val workoutDao = database.workoutDao()

    // Les plus récents d'abord.
    fun observeGoals(): Flow<List<TieredGoal>> = dao.observeGoals().map { rows -> rows.map { it.toDomain() } }

    suspend fun create(exerciseId: Long, tiers: GoalTiers): Long = dao.insertGoal(
        GoalEntity(
            exerciseId = exerciseId,
            metric = GoalMetric.ESTIMATED_1RM,
            bronzeValue = tiers.bronzeKg,
            silverValue = tiers.silverKg,
            goldValue = tiers.goldKg,
            createdAt = clock.instant(),
            bronzeAchievedAt = null,
            silverAchievedAt = null,
            goldAchievedAt = null
        )
    )

    suspend fun delete(goalId: Long) = dao.deleteGoal(goalId)

    // Vérifie les objectifs pas encore terminés après une fin de séance, une modification de l'historique ou au
    // lancement de l'app. Renvoie les paliers tout juste atteints.
    suspend fun refreshTiers(): List<ReachedTier> = database.withTransaction {
        val open = dao.getGoals().map { it.toDomain() }.filter { !it.isCompleted }
        val byId = exercises.getExercises(open.map { it.exerciseId }.distinct()).associateBy { it.id }
        open.flatMap { goal ->
            val sets = workoutDao.performedSets(goal.exerciseId, WorkoutStatus.FINISHED).map { it.toPerformedSet() }
            val reached = newlyReachedTiers(goal, sets)
            if (reached.isEmpty()) return@flatMap emptyList()
            val updated = goal.copy(reachedAt = goal.reachedAt + reached)
            dao.updateGoal(updated.toEntity())
            val exercise = byId[goal.exerciseId] ?: return@flatMap emptyList()
            reached.keys.sorted().map { ReachedTier(exercise, updated, it) }
        }
    }
}

private fun GoalEntity.toDomain() = TieredGoal(
    id = id,
    exerciseId = exerciseId,
    bronzeKg = bronzeValue,
    silverKg = silverValue,
    goldKg = goldValue,
    createdAt = createdAt,
    reachedAt = listOfNotNull<Pair<GoalTier, Instant>>(
        bronzeAchievedAt?.let { GoalTier.BRONZE to it },
        silverAchievedAt?.let { GoalTier.SILVER to it },
        goldAchievedAt?.let { GoalTier.GOLD to it }
    ).toMap()
)

private fun TieredGoal.toEntity() = GoalEntity(
    id = id,
    exerciseId = exerciseId,
    metric = GoalMetric.ESTIMATED_1RM,
    bronzeValue = bronzeKg,
    silverValue = silverKg,
    goldValue = goldKg,
    createdAt = createdAt,
    bronzeAchievedAt = reachedAt[GoalTier.BRONZE],
    silverAchievedAt = reachedAt[GoalTier.SILVER],
    goldAchievedAt = reachedAt[GoalTier.GOLD]
)
