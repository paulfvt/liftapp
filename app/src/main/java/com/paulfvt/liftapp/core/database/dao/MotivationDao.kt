package com.paulfvt.liftapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paulfvt.liftapp.core.database.entity.BadgeEntity
import com.paulfvt.liftapp.core.database.entity.GoalEntity
import com.paulfvt.liftapp.core.database.entity.WeeklyGoalEntity
import kotlinx.coroutines.flow.Flow

// Objectif hebdomadaire, objectifs à paliers et badges (phase 8). Les tables existent depuis la version 1 du schéma.
@Dao
interface MotivationDao {
    @Query("SELECT * FROM weekly_goal ORDER BY effectiveFrom")
    fun observeWeeklyGoals(): Flow<List<WeeklyGoalEntity>>

    @Query("SELECT * FROM weekly_goal ORDER BY effectiveFrom")
    suspend fun getWeeklyGoals(): List<WeeklyGoalEntity>

    // effectiveFrom est unique : un second changement dans la même semaine remplace le premier.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeeklyGoal(goal: WeeklyGoalEntity)

    @Query("SELECT * FROM goal ORDER BY createdAt DESC")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goal")
    suspend fun getGoals(): List<GoalEntity>

    @Query("SELECT * FROM goal WHERE id = :id")
    suspend fun getGoal(id: Long): GoalEntity?

    @Insert
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("DELETE FROM goal WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("SELECT * FROM badge")
    fun observeBadges(): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badge")
    suspend fun getBadges(): List<BadgeEntity>

    // Un badge débloqué reste acquis avec sa date d'origine : IGNORE ne réécrit jamais une ligne existante.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadges(badges: List<BadgeEntity>)
}
