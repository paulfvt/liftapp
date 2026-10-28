package com.paulfvt.liftapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.paulfvt.liftapp.core.database.entity.ExerciseEntity
import com.paulfvt.liftapp.core.database.entity.ExerciseMuscleEntity
import com.paulfvt.liftapp.core.database.entity.ExerciseWithMuscles
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    // Flow : la liste est réémise automatiquement à chaque modification de la table.
    // Le tri alphabétique se fait en Kotlin, car SQLite ne trie pas correctement les accents français.
    @Transaction
    @Query("SELECT * FROM exercise")
    fun observeAll(): Flow<List<ExerciseWithMuscles>>

    @Transaction
    @Query("SELECT * FROM exercise WHERE id = :id")
    fun observeById(id: Long): Flow<ExerciseWithMuscles?>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Transaction
    @Query("SELECT * FROM exercise WHERE id IN (:ids)")
    suspend fun getWithMusclesByIds(ids: List<Long>): List<ExerciseWithMuscles>

    @Query("SELECT id FROM exercise WHERE catalogKey IN (:catalogKeys)")
    suspend fun idsForCatalogKeys(catalogKeys: List<String>): List<Long>

    @Query("SELECT * FROM exercise WHERE catalogKey IS NOT NULL")
    suspend fun getCatalogExercises(): List<ExerciseEntity>

    @Insert
    suspend fun insert(exercise: ExerciseEntity): Long

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Insert
    suspend fun insertMuscles(muscles: List<ExerciseMuscleEntity>)

    @Query("DELETE FROM exercise_muscle WHERE exerciseId = :exerciseId")
    suspend fun deleteMuscles(exerciseId: Long)

    @Query("UPDATE exercise SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String?)

    @Query("UPDATE exercise SET isHidden = :isHidden WHERE id = :id")
    suspend fun updateHidden(id: Long, isHidden: Boolean)
}
