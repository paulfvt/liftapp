package com.paulfvt.liftapp.template.data

import androidx.room.withTransaction
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.database.entity.TemplateExerciseEntity
import com.paulfvt.liftapp.core.database.entity.TemplateFolderEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutTemplateEntity
import com.paulfvt.liftapp.core.domain.insertedAfter
import com.paulfvt.liftapp.exercise.data.toDomain
import com.paulfvt.liftapp.template.domain.DEFAULT_REST_SECONDS
import com.paulfvt.liftapp.template.domain.TemplateDraft
import com.paulfvt.liftapp.template.domain.TemplateExerciseDraft
import com.paulfvt.liftapp.template.domain.TemplateFolder
import com.paulfvt.liftapp.template.domain.TemplateLibrary
import com.paulfvt.liftapp.template.domain.TemplateSummary
import com.paulfvt.liftapp.template.domain.buildLibrary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

// Unique point d'accès aux templates et à leurs dossiers. Les noms reçus sont déjà validés par l'écran.
@Singleton
class TemplateRepository @Inject constructor(private val database: LiftDatabase) {
    private val dao = database.templateDao()
    private val exerciseDao = database.exerciseDao()

    fun observeLibrary(): Flow<TemplateLibrary> =
        combine(dao.observeFolders(), dao.observeTemplates(), dao.observeExerciseNames()) { folders, templates, names ->
            val namesByTemplate = names.groupBy({ it.templateId }, { it.exerciseName })
            buildLibrary(
                folders = folders.map { TemplateFolder(it.id, it.name) },
                templates = templates.map {
                    TemplateSummary(it.id, it.folderId, it.name, namesByTemplate[it.id].orEmpty())
                }
            )
        }

    suspend fun getDraft(templateId: Long): TemplateDraft? = database.withTransaction {
        val template = dao.getTemplate(templateId) ?: return@withTransaction null
        val rows = dao.getTemplateExercises(templateId)
        val exercises = exerciseDao.getWithMusclesByIds(rows.map { it.exerciseId })
            .associate { it.exercise.id to it.toDomain() }
        TemplateDraft(
            name = template.name,
            exercises = rows.mapNotNull { row ->
                exercises[row.exerciseId]?.let {
                    TemplateExerciseDraft(
                        exercise = it,
                        setCount = row.setCount,
                        targetReps = row.targetReps,
                        restSeconds = row.restSeconds ?: DEFAULT_REST_SECONDS
                    )
                }
            }
        )
    }

    // Un nouveau template se place à la fin de son dossier (ou des templates hors dossier).
    suspend fun createTemplate(folderId: Long?, draft: TemplateDraft): Long = database.withTransaction {
        val id = dao.insertTemplate(
            WorkoutTemplateEntity(
                folderId = folderId,
                name = draft.name.trim(),
                sortOrder = dao.nextTemplateSortOrder(folderId)
            )
        )
        dao.insertTemplateExercises(draft.toExerciseEntities(id))
        id
    }

    // Les exercices sont réécrits en entier : plus simple et sûr que de calculer les différences.
    suspend fun updateTemplate(templateId: Long, draft: TemplateDraft) = database.withTransaction {
        dao.renameTemplate(templateId, draft.name.trim())
        dao.deleteTemplateExercises(templateId)
        dao.insertTemplateExercises(draft.toExerciseEntities(templateId))
    }

    suspend fun renameTemplate(templateId: Long, name: String) = dao.renameTemplate(templateId, name.trim())

    // La copie se place juste après l'original, dans le même dossier.
    suspend fun duplicateTemplate(templateId: Long, copyName: String): Long = database.withTransaction {
        val original = requireNotNull(dao.getTemplate(templateId)) { "Template $templateId introuvable" }
        val copyId = dao.insertTemplate(original.copy(id = 0, name = copyName))
        dao.insertTemplateExercises(
            dao.getTemplateExercises(templateId).map { it.copy(id = 0, templateId = copyId) }
        )
        val order = dao.getTemplatesIn(original.folderId).map { it.id }.filter { it != copyId }
        writeTemplateOrder(order.insertedAfter(templateId, copyId))
        copyId
    }

    suspend fun moveTemplateToFolder(templateId: Long, folderId: Long?) = database.withTransaction {
        dao.moveTemplate(templateId, folderId, dao.nextTemplateSortOrder(folderId))
    }

    // Ordre d'un dossier après un glisser-déposer : la position dans la liste devient le sortOrder.
    suspend fun reorderTemplates(orderedIds: List<Long>) = database.withTransaction { writeTemplateOrder(orderedIds) }

    suspend fun deleteTemplate(templateId: Long) = dao.deleteTemplate(templateId)

    suspend fun createFolder(name: String): Long = database.withTransaction {
        dao.insertFolder(TemplateFolderEntity(name = name.trim(), sortOrder = dao.nextFolderSortOrder()))
    }

    suspend fun renameFolder(folderId: Long, name: String) = dao.renameFolder(folderId, name.trim())

    suspend fun reorderFolders(orderedIds: List<Long>) = database.withTransaction {
        orderedIds.forEachIndexed { index, id -> dao.updateFolderSortOrder(id, index) }
    }

    // Les templates du dossier ne sont pas supprimés : ils rejoignent la fin des templates hors dossier.
    suspend fun deleteFolder(folderId: Long) = database.withTransaction {
        val firstFreeOrder = dao.nextTemplateSortOrder(null)
        dao.getTemplatesIn(folderId).forEachIndexed { index, template ->
            dao.moveTemplate(template.id, null, firstFreeOrder + index)
        }
        dao.deleteFolder(folderId)
    }

    private suspend fun writeTemplateOrder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> dao.updateTemplateSortOrder(id, index) }
    }
}

private fun TemplateDraft.toExerciseEntities(templateId: Long) = exercises.mapIndexed { index, item ->
    TemplateExerciseEntity(
        templateId = templateId,
        exerciseId = item.exercise.id,
        sortOrder = index,
        setCount = item.setCount,
        targetReps = item.targetReps,
        restSeconds = item.restSeconds
    )
}
