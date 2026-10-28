package com.paulfvt.liftapp.progress.data

import androidx.room.withTransaction
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.database.entity.BadgeEntity
import com.paulfvt.liftapp.core.model.WorkoutStatus
import com.paulfvt.liftapp.exercise.data.ExerciseRepository
import com.paulfvt.liftapp.progress.domain.Badge
import com.paulfvt.liftapp.progress.domain.CLUB_CATALOG_KEYS
import com.paulfvt.liftapp.progress.domain.ExerciseHistory
import com.paulfvt.liftapp.progress.domain.WeeklyGoal
import com.paulfvt.liftapp.progress.domain.earnedBadges
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Badges débloqués. Ils restent acquis avec leur date, même si l'historique est modifié ensuite.
@Singleton
class BadgeRepository @Inject constructor(
    private val database: LiftDatabase,
    private val exercises: ExerciseRepository
) {
    private val dao = database.motivationDao()
    private val workoutDao = database.workoutDao()
    private val exerciseDao = database.exerciseDao()

    fun observeUnlocked(): Flow<Map<Badge, Instant>> = dao.observeBadges().map { rows ->
        rows.mapNotNull { row ->
            Badge.entries.firstOrNull { it.name == row.code }?.let { it to row.unlockedAt }
        }.toMap()
    }

    // Débloque ce que l'historique mérite et n'avait pas encore, à la date où il l'a mérité. Renvoie les nouveaux.
    suspend fun refresh(): List<Badge> = database.withTransaction {
        val starts = workoutDao.startTimes(WorkoutStatus.FINISHED)
        val weeklyGoals = dao.getWeeklyGoals().map { WeeklyGoal(it.goal, it.effectiveFrom) }
        val clubExercises = exercises.getExercises(exerciseDao.idsForCatalogKeys(CLUB_CATALOG_KEYS))
        val setsByExercise = if (clubExercises.isEmpty()) {
            emptyMap()
        } else {
            workoutDao.performedSetsOf(clubExercises.map { it.id }, WorkoutStatus.FINISHED)
                .groupBy({ it.exerciseId }, { it.set.toPerformedSet() })
        }
        val histories = clubExercises.map { ExerciseHistory(it, setsByExercise[it.id].orEmpty()) }
        val earned = earnedBadges(starts, weeklyGoals, histories, ZoneId.systemDefault())
        val unlocked = dao.getBadges().map { it.code }.toSet()
        val fresh = earned.filterKeys { it.name !in unlocked }
        dao.insertBadges(fresh.map { (badge, at) -> BadgeEntity(badge.name, at) })
        fresh.keys.sorted()
    }
}
