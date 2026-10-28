package com.paulfvt.liftapp.progress.data

import androidx.room.withTransaction
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.database.entity.PerformedSetRow
import com.paulfvt.liftapp.core.database.entity.PersonalRecordEntity
import com.paulfvt.liftapp.core.model.WorkoutStatus
import com.paulfvt.liftapp.exercise.data.ExerciseRepository
import com.paulfvt.liftapp.progress.domain.PerformedExercise
import com.paulfvt.liftapp.progress.domain.PerformedSet
import com.paulfvt.liftapp.progress.domain.PersonalRecord
import com.paulfvt.liftapp.progress.domain.computeRecords
import com.paulfvt.liftapp.progress.domain.headlineRecordType
import com.paulfvt.liftapp.workout.domain.SetValues
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// Records et données des courbes. Les records sont un cache : recompute() les réécrit à partir des séries des
// séances terminées, seule source de vérité.
@Singleton
class RecordRepository @Inject constructor(
    private val database: LiftDatabase,
    private val exercises: ExerciseRepository
) {
    private val workoutDao = database.workoutDao()
    private val recordDao = database.recordDao()
    private val exerciseDao = database.exerciseDao()

    // Appelé dans la transaction qui termine, modifie ou supprime une séance : les records changent en même temps
    // que les séries, jamais à moitié.
    suspend fun recompute(exerciseIds: Collection<Long>) = database.withTransaction {
        exerciseIds.distinct().forEach { exerciseId ->
            val trackingType = exerciseDao.getById(exerciseId)?.trackingType ?: return@forEach
            val sets = workoutDao.performedSets(exerciseId, WorkoutStatus.FINISHED).map { it.toPerformedSet() }
            recordDao.deleteFor(exerciseId)
            recordDao.insertAll(computeRecords(sets, trackingType).map { it.toEntity(exerciseId) })
        }
    }

    fun observeRecords(exerciseId: Long): Flow<List<PersonalRecord>> =
        recordDao.observeFor(exerciseId).map { rows -> rows.map { it.toDomain() } }

    fun observePerformedSets(exerciseId: Long): Flow<List<PerformedSet>> =
        workoutDao.observePerformedSets(exerciseId, WorkoutStatus.FINISHED).map { rows ->
            rows.map { it.toPerformedSet() }
        }

    // Exercices déjà faits, le plus récent d'abord, avec leur record principal (1RM, reps, durée ou distance).
    fun observePerformedExercises(): Flow<List<PerformedExercise>> =
        combine(workoutDao.observePerformedExercises(WorkoutStatus.FINISHED), recordDao.observeAll()) { rows, records ->
            rows to records.groupBy { it.exerciseId }
        }.map { (rows, recordsByExercise) ->
            val byId = exercises.getExercises(rows.map { it.exerciseId }).associateBy { it.id }
            rows.mapNotNull { row ->
                val exercise = byId[row.exerciseId] ?: return@mapNotNull null
                val headlineType = headlineRecordType(exercise.trackingType)
                PerformedExercise(
                    exercise = exercise,
                    lastPerformedAt = row.lastPerformedAt,
                    workoutCount = row.workoutCount,
                    headline = recordsByExercise[exercise.id]?.firstOrNull { it.recordType == headlineType }?.toDomain()
                )
            }
        }
}

internal fun PerformedSetRow.toPerformedSet() =
    PerformedSet(setId, workoutId, performedAt, type, SetValues(weightKg, reps, durationSec, distanceM))

private fun PersonalRecord.toEntity(exerciseId: Long) = PersonalRecordEntity(
    exerciseId = exerciseId,
    recordType = type,
    value = value,
    weightKg = weightKg,
    setId = setId,
    achievedAt = achievedAt
)

private fun PersonalRecordEntity.toDomain() = PersonalRecord(recordType, value, weightKg, setId, achievedAt)
