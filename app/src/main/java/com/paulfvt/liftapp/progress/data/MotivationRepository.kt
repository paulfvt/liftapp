package com.paulfvt.liftapp.progress.data

import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.model.WorkoutStatus
import com.paulfvt.liftapp.exercise.data.ExerciseRepository
import com.paulfvt.liftapp.progress.domain.ExerciseHistory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Historique de tous les exercices (séries validées des séances terminées), base de la carte du corps, des souvenirs
// et du récap du mois. Les calculs eux-mêmes sont dans le domaine.
@Singleton
class MotivationRepository @Inject constructor(database: LiftDatabase, private val exercises: ExerciseRepository) {
    private val workoutDao = database.workoutDao()

    fun observeHistories(): Flow<List<ExerciseHistory>> =
        workoutDao.observeAllPerformedSets(WorkoutStatus.FINISHED).map { rows ->
            val setsByExercise = rows.groupBy({ it.exerciseId }, { it.set.toPerformedSet() })
            val byId = exercises.getExercises(setsByExercise.keys.toList()).associateBy { it.id }
            setsByExercise.mapNotNull { (exerciseId, sets) -> byId[exerciseId]?.let { ExerciseHistory(it, sets) } }
        }
}
