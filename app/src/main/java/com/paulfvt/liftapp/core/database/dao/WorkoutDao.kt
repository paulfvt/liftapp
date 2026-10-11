package com.paulfvt.liftapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.paulfvt.liftapp.core.database.entity.WorkoutDetails
import com.paulfvt.liftapp.core.database.entity.WorkoutEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutExerciseEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutSetEntity
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.WorkoutStatus
import java.time.Instant
import kotlinx.coroutines.flow.Flow

// Chaque modification de la séance en cours est écrite aussitôt : c'est ce qui permet de la reprendre
// telle quelle après un redémarrage. D'où des mises à jour champ par champ, petites et indépendantes.
@Dao
interface WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workout WHERE status = :status ORDER BY startedAt DESC LIMIT 1")
    fun observeByStatus(status: WorkoutStatus): Flow<WorkoutDetails?>

    @Transaction
    @Query("SELECT * FROM workout WHERE id = :id")
    suspend fun getDetails(id: Long): WorkoutDetails?

    @Query("SELECT * FROM workout WHERE id = :id")
    suspend fun getWorkout(id: Long): WorkoutEntity?

    @Query("SELECT id FROM workout WHERE status = :status LIMIT 1")
    suspend fun firstIdWithStatus(status: WorkoutStatus): Long?

    // Dernière séance terminée contenant l'exercice, limitée à un template si templateId n'est pas nul.
    @Query(
        """
        SELECT we.id FROM workout_exercise we INNER JOIN workout w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND w.status = :status
          AND (:templateId IS NULL OR w.templateId = :templateId)
        ORDER BY w.startedAt DESC, we.sortOrder LIMIT 1
        """
    )
    suspend fun lastWorkoutExerciseId(exerciseId: Long, templateId: Long?, status: WorkoutStatus): Long?

    @Query("SELECT * FROM workout_set WHERE id = :id")
    suspend fun getSet(id: Long): WorkoutSetEntity?

    @Query("SELECT * FROM workout_set WHERE workoutExerciseId = :workoutExerciseId ORDER BY sortOrder")
    suspend fun getSets(workoutExerciseId: Long): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_exercise WHERE workoutId = :workoutId ORDER BY sortOrder")
    suspend fun getExercises(workoutId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercise WHERE id = :id")
    suspend fun getExercise(id: Long): WorkoutExerciseEntity?

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Insert
    suspend fun insertExercise(exercise: WorkoutExerciseEntity): Long

    @Insert
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Query("UPDATE workout SET name = :name WHERE id = :id")
    suspend fun updateName(id: Long, name: String)

    @Query("UPDATE workout SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String?)

    @Query("UPDATE workout SET status = :status, endedAt = :endedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: WorkoutStatus, endedAt: Instant?)

    @Query("UPDATE workout_exercise SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateExerciseOrder(id: Long, sortOrder: Int)

    @Query("UPDATE workout_exercise SET exerciseId = :exerciseId WHERE id = :id")
    suspend fun updateExerciseExercise(id: Long, exerciseId: Long)

    @Query("DELETE FROM workout_exercise WHERE id = :id")
    suspend fun deleteExercise(id: Long)

    @Query("UPDATE workout_set SET type = :type WHERE id = :id")
    suspend fun updateSetType(id: Long, type: SetType)

    @Query("UPDATE workout_set SET weightKg = :weightKg WHERE id = :id")
    suspend fun updateSetWeight(id: Long, weightKg: Double?)

    @Query("UPDATE workout_set SET reps = :reps WHERE id = :id")
    suspend fun updateSetReps(id: Long, reps: Int?)

    @Query("UPDATE workout_set SET durationSec = :durationSec WHERE id = :id")
    suspend fun updateSetDuration(id: Long, durationSec: Int?)

    @Query("UPDATE workout_set SET distanceM = :distanceM WHERE id = :id")
    suspend fun updateSetDistance(id: Long, distanceM: Double?)

    @Query("UPDATE workout_set SET rpe = :rpe WHERE id = :id")
    suspend fun updateSetRpe(id: Long, rpe: Double?)

    @Query("UPDATE workout_set SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :id")
    suspend fun updateSetCompleted(id: Long, isCompleted: Boolean, completedAt: Instant?)

    @Query("UPDATE workout_set SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSetOrder(id: Long, sortOrder: Int)

    @Query("DELETE FROM workout_set WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("DELETE FROM workout_set WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun deleteSetsOf(workoutExerciseId: Long)

    // Fin de séance : les séries non validées sont retirées, puis les exercices restés sans série.
    @Query(
        """
        DELETE FROM workout_set WHERE isCompleted = 0
          AND workoutExerciseId IN (SELECT id FROM workout_exercise WHERE workoutId = :workoutId)
        """
    )
    suspend fun deleteIncompleteSets(workoutId: Long)

    @Query(
        """
        DELETE FROM workout_exercise WHERE workoutId = :workoutId
          AND id NOT IN (SELECT workoutExerciseId FROM workout_set)
        """
    )
    suspend fun deleteExercisesWithoutSets(workoutId: Long)
}
