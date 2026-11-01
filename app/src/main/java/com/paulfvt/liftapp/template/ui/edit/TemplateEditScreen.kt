package com.paulfvt.liftapp.template.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.DragHandle
import com.paulfvt.liftapp.core.designsystem.component.LiftOutlinedButton
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.domain.workSetNumbers
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.ui.SetTypeButton
import com.paulfvt.liftapp.core.ui.durationLabel
import com.paulfvt.liftapp.core.ui.setName
import com.paulfvt.liftapp.exercise.ui.ExerciseThumbnail
import com.paulfvt.liftapp.template.domain.MAX_SET_COUNT
import com.paulfvt.liftapp.template.domain.MIN_SET_COUNT
import com.paulfvt.liftapp.template.domain.REST_CHOICES_SECONDS
import com.paulfvt.liftapp.template.domain.TemplateExerciseDraft
import com.paulfvt.liftapp.template.domain.TemplateSetDraft
import com.paulfvt.liftapp.template.ui.ChoiceDialog
import com.paulfvt.liftapp.template.ui.messageRes
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditScreen(
    onClose: () -> Unit,
    onAddExercises: (excludedIds: List<Long>) -> Unit,
    viewModel: TemplateEditViewModel = hiltViewModel()
) {
    LaunchedEffect(viewModel.isSaved) {
        if (viewModel.isSaved) onClose()
    }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    // Exercice dont on choisit le temps de repos (identifiant), null si la boîte de dialogue est fermée.
    var restDialogFor by rememberSaveable { mutableStateOf<Long?>(null) }

    fun close() {
        if (viewModel.hasChanges) showDiscardDialog = true else onClose()
    }
    // Le bouton retour du téléphone passe aussi par la confirmation quand il y a des modifications.
    BackHandler(enabled = viewModel.hasChanges) { showDiscardDialog = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ScreenTitle(
                        stringResource(
                            if (viewModel.isNew) R.string.template_create_title else R.string.template_edit_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::close) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    LiftTextButton(
                        text = stringResource(R.string.action_save),
                        onClick = viewModel::save,
                        enabled = viewModel.isLoaded
                    )
                }
            )
        }
    ) { padding ->
        if (!viewModel.isLoaded) return@Scaffold
        val draft = viewModel.draft
        val haptics = LocalHapticFeedback.current
        val listState = rememberLazyListState()
        val reorderState = rememberReorderableLazyListState(listState) { from, to ->
            // Seules les cartes d'exercice sont déplaçables ; leur clé est l'identifiant de l'exercice.
            viewModel.onMoveExercise(fromId = from.key as Long, toId = to.key as Long)
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
        LazyColumn(
            state = listState,
            // imePadding : le clavier ne cache pas le champ « Reps » en cours de saisie.
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "name") {
                val error = viewModel.nameError
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.template_name)) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(stringResource(it.messageRes)) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (draft.exercises.isEmpty()) {
                item(key = "hint") {
                    Text(
                        stringResource(R.string.template_no_exercises_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            itemsIndexed(draft.exercises, key = { _, item -> item.exercise.id }) { index, item ->
                val exerciseId = item.exercise.id
                ReorderableItem(reorderState, key = exerciseId) { isDragging ->
                    TemplateExerciseCard(
                        item = item,
                        isDragging = isDragging,
                        canMoveUp = index > 0,
                        canMoveDown = index < draft.exercises.lastIndex,
                        dragHandle = {
                            DragHandle(
                                Modifier.draggableHandle(
                                    onDragStarted = {
                                        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                    },
                                    onDragStopped = { haptics.performHapticFeedback(HapticFeedbackType.GestureEnd) }
                                )
                            )
                        },
                        onRemove = { viewModel.onRemoveExercise(exerciseId) },
                        onMoveBy = { offset -> viewModel.onMoveExerciseBy(exerciseId, offset) },
                        onAddSet = { viewModel.onAddSet(exerciseId) },
                        onRemoveSet = { setIndex -> viewModel.onRemoveSet(exerciseId, setIndex) },
                        onSetTypeChange = { setIndex, type -> viewModel.onSetTypeChange(exerciseId, setIndex, type) },
                        onTargetRepsChange = { setIndex, input ->
                            viewModel.onTargetRepsChange(exerciseId, setIndex, input)
                        },
                        onRestClick = { restDialogFor = exerciseId }
                    )
                }
            }
            item(key = "add") {
                LiftOutlinedButton(
                    text = stringResource(R.string.template_add_exercises),
                    onClick = { onAddExercises(draft.exercises.map { it.exercise.id }) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = R.drawable.ic_add
                )
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.template_discard_title)) },
            text = { Text(stringResource(R.string.template_discard_text)) },
            confirmButton = {
                LiftTextButton(
                    text = stringResource(R.string.template_discard_confirm),
                    onClick = {
                        showDiscardDialog = false
                        onClose()
                    },
                    color = MaterialTheme.colorScheme.error
                )
            },
            dismissButton = {
                LiftTextButton(stringResource(R.string.template_discard_keep), onClick = { showDiscardDialog = false })
            }
        )
    }
    restDialogFor?.let { exerciseId ->
        val current = viewModel.draft.exercises.firstOrNull { it.exercise.id == exerciseId }?.restSeconds
        ChoiceDialog(
            title = stringResource(R.string.template_rest_title),
            options = REST_CHOICES_SECONDS,
            selected = current,
            label = { durationLabel(it) },
            onSelect = { seconds ->
                viewModel.onRestChange(exerciseId, seconds)
                restDialogFor = null
            },
            onDismiss = { restDialogFor = null }
        )
    }
}

@Composable
private fun TemplateExerciseCard(
    item: TemplateExerciseDraft,
    isDragging: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    dragHandle: @Composable () -> Unit,
    onRemove: () -> Unit,
    onMoveBy: (offset: Int) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (setIndex: Int) -> Unit,
    onSetTypeChange: (setIndex: Int, SetType) -> Unit,
    onTargetRepsChange: (setIndex: Int, input: String) -> Unit,
    onRestClick: () -> Unit
) {
    val elevation by animateDpAsState(if (isDragging) 8.dp else 1.dp, label = "elevation")
    val moveUpLabel = stringResource(R.string.action_move_up)
    val moveDownLabel = stringResource(R.string.action_move_down)
    ElevatedCard(
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
            modifier = Modifier.padding(start = 12.dp, top = 12.dp)
        ) {
            ExerciseThumbnail(item.exercise)
            Text(
                item.exercise.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            IconButton(onClick = onRemove) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    stringResource(R.string.template_remove_exercise, item.exercise.name)
                )
            }
            dragHandle()
        }
        Row(modifier = Modifier.padding(start = 12.dp, top = 8.dp)) {
            ColumnTitle(stringResource(R.string.template_set_header), Modifier.width(48.dp))
            ColumnTitle(stringResource(R.string.template_target_reps), Modifier.padding(start = 8.dp))
        }
        val workNumbers = workSetNumbers(item.sets.map { it.type })
        item.sets.forEachIndexed { index, set ->
            TemplateSetRow(
                set = set,
                workNumber = workNumbers[index],
                canRemove = item.sets.size > MIN_SET_COUNT,
                onTypeChange = { onSetTypeChange(index, it) },
                onTargetRepsChange = { onTargetRepsChange(index, it) },
                onRemove = { onRemoveSet(index) }
            )
        }
        val rest = durationLabel(item.restSeconds)
        val restDescription = stringResource(R.string.template_rest, rest)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, bottom = 8.dp)
        ) {
            LiftTextButton(
                text = stringResource(R.string.template_set_add),
                onClick = onAddSet,
                enabled = item.sets.size < MAX_SET_COUNT,
                icon = R.drawable.ic_add
            )
            Spacer(Modifier.weight(1f))
            // L'icône minuteur suffit à l'écran ; le lecteur d'écran entend « Repos 2 min ».
            AssistChip(
                onClick = onRestClick,
                label = { Text(rest) },
                modifier = Modifier.semantics { contentDescription = restDescription },
                leadingIcon = {
                    Icon(
                        painterResource(R.drawable.ic_timer),
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                }
            )
        }
    }
}

@Composable
private fun ColumnTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

// Une série prévue : pastille de type (numéro, « É » ou « D »), reps cibles, bouton pour la retirer.
@Composable
private fun TemplateSetRow(
    set: TemplateSetDraft,
    workNumber: Int?,
    canRemove: Boolean,
    onTypeChange: (SetType) -> Unit,
    onTargetRepsChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    val name = setName(set.type, workNumber)
    val repsDescription = stringResource(R.string.template_set_target_reps, name)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, bottom = 4.dp)
    ) {
        SetTypeButton(type = set.type, workNumber = workNumber, onTypeChange = onTypeChange)
        OutlinedTextField(
            value = set.targetReps?.toString().orEmpty(),
            onValueChange = onTargetRepsChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.padding(start = 8.dp).width(96.dp).semantics { contentDescription = repsDescription }
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onRemove,
            enabled = canRemove,
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
        ) {
            Icon(painterResource(R.drawable.ic_remove), stringResource(R.string.template_set_remove, name))
        }
    }
}
