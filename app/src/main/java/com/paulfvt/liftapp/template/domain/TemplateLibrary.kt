package com.paulfvt.liftapp.template.domain

const val TEMPLATE_NAME_MAX_LENGTH = 60

data class TemplateFolder(val id: Long, val name: String)

// Résumé d'un template pour la liste de l'onglet Séances.
data class TemplateSummary(val id: Long, val folderId: Long?, val name: String, val exerciseNames: List<String>)

data class FolderSection(val folder: TemplateFolder, val templates: List<TemplateSummary>)

// Contenu de l'onglet Séances : les templates hors dossier, puis chaque dossier avec ses templates.
data class TemplateLibrary(val unfiled: List<TemplateSummary>, val folders: List<FolderSection>)

// Les deux listes arrivent déjà triées. Un template dont le dossier n'existe plus est rangé hors dossier,
// pour ne jamais disparaître de l'écran.
fun buildLibrary(folders: List<TemplateFolder>, templates: List<TemplateSummary>): TemplateLibrary {
    val folderIds = folders.map { it.id }.toSet()
    val (filed, unfiled) = templates.partition { it.folderId in folderIds }
    val byFolder = filed.groupBy { it.folderId }
    return TemplateLibrary(
        unfiled = unfiled,
        folders = folders.map { FolderSection(it, byFolder[it.id].orEmpty()) }
    )
}

enum class NameError {
    EMPTY,
    TOO_LONG
}

// Règle commune aux noms de templates et de dossiers. Les espaces autour du nom ne comptent pas.
fun validateName(name: String): NameError? {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> NameError.EMPTY
        trimmed.length > TEMPLATE_NAME_MAX_LENGTH -> NameError.TOO_LONG
        else -> null
    }
}

// Nom d'une copie (« Copie de Push A »), raccourci s'il dépasse la limite.
fun copyName(formattedCopyName: String): String = formattedCopyName.trim().take(TEMPLATE_NAME_MAX_LENGTH).trim()
