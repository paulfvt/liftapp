package com.paulfvt.liftapp.progress.data

import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.database.entity.WeeklyGoalEntity
import com.paulfvt.liftapp.core.model.WorkoutStatus
import com.paulfvt.liftapp.progress.domain.GOAL_SINCE_ALWAYS
import com.paulfvt.liftapp.progress.domain.WEEKLY_GOAL_RANGE
import com.paulfvt.liftapp.progress.domain.WeeklyGoal
import com.paulfvt.liftapp.progress.domain.WeeklyStreak
import com.paulfvt.liftapp.progress.domain.weekOf
import com.paulfvt.liftapp.progress.domain.weeklyStreak
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// Objectif hebdomadaire (historisé dans weekly_goal) et streak calculée à partir des séances terminées.
@Singleton
class WeeklyGoalRepository @Inject constructor(database: LiftDatabase, private val clock: Clock) {
    private val dao = database.motivationDao()
    private val workoutDao = database.workoutDao()

    fun observeGoals(): Flow<List<WeeklyGoal>> = dao.observeWeeklyGoals().map { rows -> rows.map { it.toDomain() } }

    fun observeStreak(): Flow<WeeklyStreak> =
        combine(workoutDao.observeStartTimes(WorkoutStatus.FINISHED), observeGoals()) { starts, goals ->
            weeklyStreak(starts, goals, clock.instant(), ZoneId.systemDefault())
        }

    suspend fun getGoals(): List<WeeklyGoal> = dao.getWeeklyGoals().map { it.toDomain() }

    // Objectif choisi au premier lancement : il vaut pour toutes les semaines.
    suspend fun setInitialGoal(goal: Int) {
        val entity = WeeklyGoalEntity(goal = goal.coerceIn(WEEKLY_GOAL_RANGE), effectiveFrom = GOAL_SINCE_ALWAYS)
        dao.upsertWeeklyGoal(entity)
    }

    // Un changement vaut dès la semaine en cours ; les semaines passées gardent leur objectif.
    suspend fun changeGoal(goal: Int) {
        val monday = weekOf(clock.instant(), ZoneId.systemDefault())
        dao.upsertWeeklyGoal(WeeklyGoalEntity(goal = goal.coerceIn(WEEKLY_GOAL_RANGE), effectiveFrom = monday))
    }
}

private fun WeeklyGoalEntity.toDomain() = WeeklyGoal(goal, effectiveFrom)
