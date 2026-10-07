package com.paulfvt.liftapp.exercise.data

import androidx.room.withTransaction
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.exercise.domain.ExerciseDraft
import java.text.Collator
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Unique point d'accès aux exercices pour les écrans : ils ne parlent jamais directement à Room.
@Singleton
class ExerciseRepository @Inject constructor(private val database: LiftDatabase) {
    private val dao = database.exerciseDao()

    // Tri alphabétique français : « Écarté » se range avec les E, pas après le Z.
    fun observeExercises(): Flow<List<Exercise>> = dao.observeAll().map { rows ->
        val collator = Collator.getInstance(Locale.FRENCH)
        rows.map { it.toDomain() }.sortedWith(compareBy(collator) { it.name })
    }

    fun observeExercise(id: Long): Flow<Exercise?> = dao.observeById(id).map { it?.toDomain() }

    // Exercices dans l'ordre des identifiants demandés (SQLite ne garantit pas l'ordre d'un « IN »).
    suspend fun getExercises(ids: List<Long>): List<Exercise> {
        val byId = dao.getWithMusclesByIds(ids).associate { it.exercise.id to it.toDomain() }
        return ids.mapNotNull { byId[it] }
    }

    suspend fun createCustom(draft: ExerciseDraft): Long = database.withTransaction {
        val id = dao.insert(draft.toEntity(existing = null))
        dao.insertMuscles(draft.toMuscleEntities(id))
        id
    }

    // Seuls les exercices personnalisés sont modifiables. Ceux du catalogue peuvent seulement être masqués.
    suspend fun updateCustom(id: Long, draft: ExerciseDraft) = database.withTransaction {
        val existing = dao.getById(id)
        require(existing != null && existing.isCustom) {
            "Exercice $id introuvable ou non personnalisé"
        }
        dao.update(draft.toEntity(existing))
        dao.deleteMuscles(id)
        dao.insertMuscles(draft.toMuscleEntities(id))
    }

    // Une note vide est enregistrée comme absente.
    suspend fun updateNote(id: Long, note: String) {
        dao.updateNote(id, note.trim().ifEmpty { null })
    }

    // Masquer plutôt que supprimer : l'historique et les templates qui utilisent l'exercice restent intacts.
    suspend fun setHidden(id: Long, isHidden: Boolean) = dao.updateHidden(id, isHidden)
}
