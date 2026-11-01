package com.paulfvt.liftapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.paulfvt.liftapp.core.database.entity.TemplateExerciseEntity
import com.paulfvt.liftapp.core.database.entity.TemplateFolderEntity
import com.paulfvt.liftapp.core.database.entity.TemplateSetEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutTemplateEntity
import kotlinx.coroutines.flow.Flow

// Nom d'un exercice d'un template, pour le résumé affiché dans la liste.
data class TemplateExerciseNameRow(val templateId: Long, val exerciseName: String)

data class TemplateSetCountRow(val templateId: Long, val setCount: Int)

@Dao
interface TemplateDao {
    @Query("SELECT * FROM template_folder ORDER BY sortOrder, id")
    fun observeFolders(): Flow<List<TemplateFolderEntity>>

    @Query("SELECT * FROM workout_template ORDER BY sortOrder, id")
    fun observeTemplates(): Flow<List<WorkoutTemplateEntity>>

    @Query(
        """
        SELECT te.templateId AS templateId, e.name AS exerciseName
        FROM template_exercise te INNER JOIN exercise e ON e.id = te.exerciseId
        ORDER BY te.templateId, te.sortOrder
        """
    )
    fun observeExerciseNames(): Flow<List<TemplateExerciseNameRow>>

    @Query(
        """
        SELECT te.templateId AS templateId, COUNT(ts.id) AS setCount
        FROM template_exercise te INNER JOIN template_set ts ON ts.templateExerciseId = te.id
        GROUP BY te.templateId
        """
    )
    fun observeSetCounts(): Flow<List<TemplateSetCountRow>>

    @Query("SELECT * FROM workout_template WHERE id = :id")
    suspend fun getTemplate(id: Long): WorkoutTemplateEntity?

    // « IS » plutôt que « = » : en SQL, « folderId = NULL » n'est jamais vrai.
    @Query("SELECT * FROM workout_template WHERE folderId IS :folderId ORDER BY sortOrder, id")
    suspend fun getTemplatesIn(folderId: Long?): List<WorkoutTemplateEntity>

    @Query("SELECT * FROM template_exercise WHERE templateId = :templateId ORDER BY sortOrder")
    suspend fun getTemplateExercises(templateId: Long): List<TemplateExerciseEntity>

    @Query("SELECT COALESCE(MAX(sortOrder) + 1, 0) FROM workout_template WHERE folderId IS :folderId")
    suspend fun nextTemplateSortOrder(folderId: Long?): Int

    @Query("SELECT COALESCE(MAX(sortOrder) + 1, 0) FROM template_folder")
    suspend fun nextFolderSortOrder(): Int

    @Insert
    suspend fun insertTemplate(template: WorkoutTemplateEntity): Long

    @Query("UPDATE workout_template SET name = :name WHERE id = :id")
    suspend fun renameTemplate(id: Long, name: String)

    @Query("UPDATE workout_template SET folderId = :folderId, sortOrder = :sortOrder WHERE id = :id")
    suspend fun moveTemplate(id: Long, folderId: Long?, sortOrder: Int)

    @Query("UPDATE workout_template SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateTemplateSortOrder(id: Long, sortOrder: Int)

    // Les exercices du template partent avec lui (CASCADE) ; les séances passées gardent leur nom (SET_NULL).
    @Query("DELETE FROM workout_template WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Query(
        """
        SELECT ts.* FROM template_set ts INNER JOIN template_exercise te ON te.id = ts.templateExerciseId
        WHERE te.templateId = :templateId ORDER BY ts.templateExerciseId, ts.sortOrder
        """
    )
    suspend fun getTemplateSets(templateId: Long): List<TemplateSetEntity>

    // Renvoie les identifiants créés, dans l'ordre de la liste : ils servent à rattacher les séries.
    @Insert
    suspend fun insertTemplateExercises(exercises: List<TemplateExerciseEntity>): List<Long>

    @Insert
    suspend fun insertTemplateSets(sets: List<TemplateSetEntity>)

    // Les séries partent avec leurs exercices (CASCADE).
    @Query("DELETE FROM template_exercise WHERE templateId = :templateId")
    suspend fun deleteTemplateExercises(templateId: Long)

    @Insert
    suspend fun insertFolder(folder: TemplateFolderEntity): Long

    @Query("UPDATE template_folder SET name = :name WHERE id = :id")
    suspend fun renameFolder(id: Long, name: String)

    @Query("UPDATE template_folder SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateFolderSortOrder(id: Long, sortOrder: Int)

    @Query("DELETE FROM template_folder WHERE id = :id")
    suspend fun deleteFolder(id: Long)
}
