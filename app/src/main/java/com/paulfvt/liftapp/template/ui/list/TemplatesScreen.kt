package com.paulfvt.liftapp.template.ui.list

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.DragHandle
import com.paulfvt.liftapp.core.designsystem.component.MenuEntry
import com.paulfvt.liftapp.core.designsystem.component.OverflowMenu
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.template.domain.TemplateFolder
import com.paulfvt.liftapp.template.domain.TemplateLibrary
import com.paulfvt.liftapp.template.domain.TemplateSummary
import com.paulfvt.liftapp.template.domain.moveTemplate
import com.paulfvt.liftapp.template.domain.sectionContaining
import com.paulfvt.liftapp.template.ui.ChoiceDialog
import com.paulfvt.liftapp.template.ui.ConfirmDeleteDialog
import com.paulfvt.liftapp.template.ui.NameDialog
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// Onglet Séances : templates hors dossier, puis dossiers repliables avec leurs templates.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    onOpenTemplate: (Long) -> Unit,
    onCreateTemplate: (folderId: Long?) -> Unit,
    onOpenCatalog: () -> Unit,
    viewModel: TemplatesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle(stringResource(R.string.nav_templates)) },
                actions = {
                    OverflowMenu(
                        listOf(
                            MenuEntry(R.string.templates_new_folder, R.drawable.ic_create_new_folder) {
                                viewModel.openDialog(TemplatesDialog.CreateFolder)
                            },
                            MenuEntry(
                                R.string.templates_open_catalog,
                                R.drawable.ic_fitness_center,
                                onClick = onOpenCatalog
                            )
                        )
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onCreateTemplate(null) },
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.templates_new_template)) }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Unit
            uiState.isEmpty -> EmptyTemplates(Modifier.padding(padding))
            else -> TemplateList(
                library = uiState.library,
                viewModel = viewModel,
                onOpenTemplate = onOpenTemplate,
                onCreateTemplate = onCreateTemplate,
                modifier = Modifier.padding(padding)
            )
        }
    }
    TemplatesDialogs(viewModel = viewModel, folders = uiState.library.folders.map { it.folder })
}

@Composable
private fun TemplateList(
    library: TemplateLibrary,
    viewModel: TemplatesViewModel,
    onOpenTemplate: (Long) -> Unit,
    onCreateTemplate: (folderId: Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    // Copie locale : le glisser-déposer la modifie à chaque pas, la base n'est écrite qu'au lâcher.
    var localLibrary by remember(library) { mutableStateOf(library) }
    // Template en cours de déplacement : seuls ceux de sa section (même dossier) peuvent échanger leur place.
    var dragged by remember { mutableStateOf<TemplateSummary?>(null) }
    // Dossiers repliés, gardés après une rotation. Un dossier est déplié par défaut.
    var collapsed by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        // Clés des templates = leur identifiant (Long) ; les en-têtes de dossier ne sont pas déplaçables.
        localLibrary = localLibrary.moveTemplate(fromId = from.key as Long, toId = to.key as Long)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }
    val hasFolders = localLibrary.folders.isNotEmpty()

    fun LazyListScope.templateItems(templates: List<TemplateSummary>) {
        itemsIndexed(templates, key = { _, template -> template.id }) { index, template ->
            ReorderableItem(
                reorderState,
                key = template.id,
                enabled = dragged.let { it == null || it.folderId == template.folderId }
            ) { isDragging ->
                TemplateCard(
                    template = template,
                    isDragging = isDragging,
                    canMoveUp = index > 0,
                    canMoveDown = index < templates.lastIndex,
                    canChangeFolder = hasFolders || template.folderId != null,
                    dragHandle = {
                        DragHandle(
                            Modifier.draggableHandle(
                                onDragStarted = {
                                    dragged = template
                                    haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                },
                                onDragStopped = {
                                    localLibrary.sectionContaining(template.id)?.let { section ->
                                        viewModel.reorderTemplates(section.map { it.id })
                                    }
                                    dragged = null
                                    haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                }
                            )
                        )
                    },
                    onClick = { onOpenTemplate(template.id) },
                    onRename = { viewModel.openDialog(TemplatesDialog.RenameTemplate(template)) },
                    onDuplicate = { copyName -> viewModel.duplicateTemplate(template.id, copyName) },
                    onChangeFolder = { viewModel.openDialog(TemplatesDialog.MoveTemplate(template)) },
                    onDelete = { viewModel.openDialog(TemplatesDialog.DeleteTemplate(template)) },
                    onMoveBy = { offset -> viewModel.moveTemplateBy(template.id, offset) }
                )
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        // Marge basse pour que le dernier template ne soit pas caché sous le bouton flottant.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        templateItems(localLibrary.unfiled)
        localLibrary.folders.forEachIndexed { index, section ->
            val folder = section.folder
            val expanded = folder.id !in collapsed
            item(key = "folder-${folder.id}") {
                FolderHeader(
                    folder = folder,
                    templateCount = section.templates.size,
                    expanded = expanded,
                    onToggle = { collapsed = if (expanded) collapsed + folder.id else collapsed - folder.id },
                    menuEntries = listOfNotNull(
                        MenuEntry(R.string.folder_new_template, R.drawable.ic_add) { onCreateTemplate(folder.id) },
                        MenuEntry(R.string.action_rename, R.drawable.ic_edit) {
                            viewModel.openDialog(TemplatesDialog.RenameFolder(folder))
                        },
                        MenuEntry(R.string.action_move_up) { viewModel.moveFolderBy(folder.id, -1) }
                            .takeIf { index > 0 },
                        MenuEntry(R.string.action_move_down) { viewModel.moveFolderBy(folder.id, 1) }
                            .takeIf { index < localLibrary.folders.lastIndex },
                        MenuEntry(R.string.action_delete, R.drawable.ic_delete, isDestructive = true) {
                            viewModel.openDialog(TemplatesDialog.DeleteFolder(folder))
                        }
                    ),
                    modifier = Modifier.animateItem()
                )
            }
            if (expanded) {
                if (section.templates.isEmpty()) {
                    item(key = "folder-empty-${folder.id}") {
                        Text(
                            stringResource(R.string.folder_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.animateItem().padding(start = 52.dp, bottom = 8.dp)
                        )
                    }
                }
                templateItems(section.templates)
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: TemplateSummary,
    isDragging: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canChangeFolder: Boolean,
    dragHandle: @Composable () -> Unit,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: (copyName: String) -> Unit,
    onChangeFolder: () -> Unit,
    onDelete: () -> Unit,
    onMoveBy: (offset: Int) -> Unit
) {
    val elevation by animateDpAsState(if (isDragging) 8.dp else 1.dp, label = "elevation")
    val copyName = stringResource(R.string.template_copy_name, template.name)
    val moveUpLabel = stringResource(R.string.action_move_up)
    val moveDownLabel = stringResource(R.string.action_move_down)
    val noExercise = stringResource(R.string.template_no_exercise)
    ElevatedCard(
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
        // Le glisser-déposer est inaccessible au lecteur d'écran : il propose « Monter » et « Descendre » à la place.
        modifier = Modifier.fillMaxWidth().semantics {
            customActions = listOfNotNull(
                CustomAccessibilityAction(moveUpLabel) {
                    onMoveBy(-1)
                    true
                }.takeIf { canMoveUp },
                CustomAccessibilityAction(moveDownLabel) {
                    onMoveBy(1)
                    true
                }.takeIf { canMoveDown }
            )
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    template.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    template.exerciseNames.joinToString(", ").ifEmpty { noExercise },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OverflowMenu(
                listOfNotNull(
                    MenuEntry(R.string.action_rename, R.drawable.ic_edit, onClick = onRename),
                    MenuEntry(R.string.template_duplicate, R.drawable.ic_content_copy) { onDuplicate(copyName) },
                    MenuEntry(R.string.template_move, R.drawable.ic_folder, onClick = onChangeFolder)
                        .takeIf { canChangeFolder },
                    MenuEntry(R.string.action_delete, R.drawable.ic_delete, isDestructive = true, onClick = onDelete)
                )
            )
            dragHandle()
        }
    }
}

@Composable
private fun FolderHeader(
    folder: TemplateFolder,
    templateCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    menuEntries: List<MenuEntry>,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    val state = stringResource(if (expanded) R.string.folder_expanded else R.string.folder_collapsed)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onToggle)
            .semantics { stateDescription = state }
            .padding(start = 12.dp)
    ) {
        Icon(
            painterResource(R.drawable.ic_folder),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                folder.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                pluralStringResource(R.plurals.folder_template_count, templateCount, templateCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            painterResource(R.drawable.ic_expand_more),
            contentDescription = null,
            modifier = Modifier.rotate(rotation)
        )
        OverflowMenu(menuEntries)
    }
}

@Composable
private fun EmptyTemplates(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painterResource(R.drawable.ic_fitness_center),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            stringResource(R.string.templates_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(R.string.templates_empty_text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TemplatesDialogs(viewModel: TemplatesViewModel, folders: List<TemplateFolder>) {
    when (val dialog = viewModel.dialog) {
        null -> Unit
        TemplatesDialog.CreateFolder -> NameDialog(
            title = stringResource(R.string.templates_new_folder),
            initialName = "",
            confirmLabel = stringResource(R.string.action_create),
            onConfirm = viewModel::createFolder,
            onDismiss = viewModel::dismissDialog
        )
        is TemplatesDialog.RenameFolder -> NameDialog(
            title = stringResource(R.string.folder_rename_title),
            initialName = dialog.folder.name,
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = { viewModel.renameFolder(dialog.folder.id, it) },
            onDismiss = viewModel::dismissDialog
        )
        is TemplatesDialog.DeleteFolder -> ConfirmDeleteDialog(
            title = stringResource(R.string.folder_delete_title, dialog.folder.name),
            text = stringResource(R.string.folder_delete_text),
            onConfirm = { viewModel.deleteFolder(dialog.folder.id) },
            onDismiss = viewModel::dismissDialog
        )
        is TemplatesDialog.RenameTemplate -> NameDialog(
            title = stringResource(R.string.template_rename_title),
            initialName = dialog.template.name,
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = { viewModel.renameTemplate(dialog.template.id, it) },
            onDismiss = viewModel::dismissDialog
        )
        is TemplatesDialog.MoveTemplate -> {
            val noFolder = stringResource(R.string.template_move_no_folder)
            val names = folders.associate { it.id to it.name }
            ChoiceDialog(
                title = stringResource(R.string.template_move_title),
                // null = « Sans dossier ».
                options = listOf<Long?>(null) + folders.map { it.id },
                selected = dialog.template.folderId,
                label = { folderId -> folderId?.let { names[it] } ?: noFolder },
                onSelect = { viewModel.moveTemplateToFolder(dialog.template, it) },
                onDismiss = viewModel::dismissDialog
            )
        }
        is TemplatesDialog.DeleteTemplate -> ConfirmDeleteDialog(
            title = stringResource(R.string.template_delete_title, dialog.template.name),
            text = stringResource(R.string.template_delete_text),
            onConfirm = { viewModel.deleteTemplate(dialog.template.id) },
            onDismiss = viewModel::dismissDialog
        )
    }
}
