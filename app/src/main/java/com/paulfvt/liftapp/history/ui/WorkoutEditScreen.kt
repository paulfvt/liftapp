package com.paulfvt.liftapp.history.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftCard
import com.paulfvt.liftapp.core.designsystem.component.LiftOutlinedButton
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.MenuEntry
import com.paulfvt.liftapp.core.designsystem.component.OverflowMenu
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.domain.workSetNumbers
import com.paulfvt.liftapp.exercise.ui.ExerciseThumbnail
import com.paulfvt.liftapp.history.domain.EditProblem
import com.paulfvt.liftapp.template.ui.ChoiceDialog
import com.paulfvt.liftapp.workout.domain.RPE_CHOICES
import com.paulfvt.liftapp.workout.domain.Workout
import com.paulfvt.liftapp.workout.domain.WorkoutExercise
import com.paulfvt.liftapp.workout.domain.findSet
import com.paulfvt.liftapp.workout.domain.formatDecimal
import com.paulfvt.liftapp.workout.ui.FollowOpenField
import com.paulfvt.liftapp.workout.ui.SetActions
import com.paulfvt.liftapp.workout.ui.SetHeaderRow
import com.paulfvt.liftapp.workout.ui.SetKeypad
import com.paulfvt.liftapp.workout.ui.SetKeypadState
import com.paulfvt.liftapp.workout.ui.SetRowMode
import com.paulfvt.liftapp.workout.ui.WorkoutSetRow
import com.paulfvt.liftapp.workout.ui.moveNext

// Modification d'une séance passée : mêmes lignes de série que pendant la séance, sans case de validation.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditScreen(
    onClose: () -> Unit,
    onAddExercises: (excludedIds: List<Long>) -> Unit,
    viewModel: WorkoutEditViewModel = hiltViewModel()
) {
    LaunchedEffect(viewModel.isSaved) {
        if (viewModel.isSaved) onClose()
    }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val close = { if (viewModel.hasChanges) showDiscardDialog = true else onClose() }
    // Le bouton retour du téléphone passe aussi par la confirmation quand il y a des modifications.
    BackHandler(enabled = viewModel.hasChanges) { showDiscardDialog = true }
    val keypad = remember { SetKeypadState() }
    // Déclaré après : il passe en premier, le retour ferme d'abord le pavé.
    BackHandler(enabled = keypad.isOpen, onBack = keypad::close)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle(stringResource(R.string.history_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = close) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    LiftTextButton(
                        text = stringResource(R.string.action_save),
                        onClick = viewModel::save,
                        enabled = viewModel.draft != null
                    )
                }
            )
        },
        bottomBar = {
            val draft = viewModel.draft
            if (keypad.isOpen && draft != null) {
                Surface(tonalElevation = 3.dp) {
                    SetKeypad(
                        state = keypad,
                        // Séance terminée : toutes ses séries sont déjà validées, « Suivant » ne fait que passer.
                        onNext = { keypad.moveNext(draft.exercises) },
                        onRpe = {
                            keypad.openField?.let { draft.findSet(it.setId) }?.let { viewModel.openRpe(it.id, it.rpe) }
                        },
                        modifier = Modifier.navigationBarsPadding()
                    )
                }
            }
        }
    ) { padding ->
        val draft = viewModel.draft ?: return@Scaffold
        EditContent(draft, viewModel, onAddExercises, keypad, Modifier.padding(padding).consumeWindowInsets(padding))
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.template_discard_title)) },
            text = { Text(stringResource(R.string.history_edit_discard_text)) },
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
    viewModel.rpeChoice?.let { choice ->
        val none = stringResource(R.string.workout_rpe_none)
        ChoiceDialog(
            title = stringResource(R.string.workout_rpe_title),
            options = listOf<Double?>(null) + RPE_CHOICES,
            selected = choice.current,
            label = { rpe -> rpe?.let(::formatDecimal) ?: none },
            onSelect = { viewModel.onRpeSelected(choice.setId, it) },
            onDismiss = viewModel::dismissRpe
        )
    }
}

@Composable
private fun EditContent(
    draft: Workout,
    viewModel: WorkoutEditViewModel,
    onAddExercises: (excludedIds: List<Long>) -> Unit,
    keypad: SetKeypadState,
    modifier: Modifier
) {
    val listState = rememberLazyListState()
    // Avant les exercices : le message d'erreur s'il y en a un, puis le nom et la note.
    FollowOpenField(keypad, draft.exercises, listState, firstIndex = if (viewModel.problem != null) 2 else 1)
    val setActions = remember(viewModel) {
        SetActions(
            onTypeChange = viewModel::onSetTypeChange,
            onWeightChange = viewModel::onWeightChange,
            onRepsChange = viewModel::onRepsChange,
            onDurationChange = viewModel::onDurationChange,
            onDistanceChange = viewModel::onDistanceChange,
            // Pas de case de validation : toutes les séries d'une séance terminée sont validées.
            onToggleCompleted = {},
            onRpeClick = { set -> viewModel.openRpe(set.id, set.rpe) },
            onRemove = viewModel::onRemoveSet
        )
    }
    LazyColumn(
        state = listState,
        // imePadding : le clavier d'Android ne cache pas le nom ou la note en cours de saisie.
        modifier = modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        viewModel.problem?.let { problem ->
            item(key = "problem") {
                Text(
                    stringResource(problem.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    // Annoncé par le lecteur d'écran dès qu'il apparaît.
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }
        item(key = "fields") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.history_edit_name)) },
                    singleLine = true,
                    isError = viewModel.problem == EditProblem.BLANK_NAME,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().closesKeypad(keypad)
                )
                OutlinedTextField(
                    value = draft.note.orEmpty(),
                    onValueChange = viewModel::onNoteChange,
                    label = { Text(stringResource(R.string.history_edit_note)) },
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().closesKeypad(keypad)
                )
            }
        }
        items(draft.exercises, key = { it.id }) { item ->
            EditExerciseCard(
                item = item,
                setActions = setActions,
                keypad = keypad,
                onAddSet = { viewModel.onAddSet(item.id) },
                onRemove = { viewModel.onRemoveExercise(item.id) }
            )
        }
        item(key = "add") {
            LiftOutlinedButton(
                text = stringResource(R.string.workout_add_exercises),
                onClick = { onAddExercises(draft.exercises.map { it.exercise.id }) },
                modifier = Modifier.fillMaxWidth(),
                icon = R.drawable.ic_add
            )
        }
    }
}

@Composable
private fun EditExerciseCard(
    item: WorkoutExercise,
    setActions: SetActions,
    keypad: SetKeypadState,
    onAddSet: () -> Unit,
    onRemove: () -> Unit
) {
    LiftCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, top = 12.dp)) {
            ExerciseThumbnail(item.exercise, zoomable = true)
            Text(
                item.exercise.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            OverflowMenu(
                listOf(
                    MenuEntry(
                        R.string.workout_remove_exercise,
                        R.drawable.ic_delete,
                        isDestructive = true,
                        onClick = onRemove
                    )
                )
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)
        ) {
            SetHeaderRow(item.exercise, SetRowMode.EDIT)
            val workNumbers = workSetNumbers(item.sets.map { it.type })
            item.sets.forEachIndexed { index, set ->
                // key : chaque ligne garde son propre texte saisi si une série au-dessus est retirée.
                key(set.id) {
                    WorkoutSetRow(
                        set = set,
                        workNumber = workNumbers[index],
                        previous = null,
                        exercise = item.exercise,
                        canRemove = item.sets.size > 1,
                        actions = setActions,
                        keypad = keypad,
                        mode = SetRowMode.EDIT
                    )
                }
            }
        }
        LiftTextButton(
            text = stringResource(R.string.template_set_add),
            onClick = onAddSet,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            icon = R.drawable.ic_add
        )
    }
}

// Le nom ou la note prend la main : le pavé se ferme pour laisser place au clavier d'Android.
private fun Modifier.closesKeypad(keypad: SetKeypadState): Modifier = onFocusChanged {
    if (it.isFocused) keypad.close()
}

private fun EditProblem.messageRes(): Int = when (this) {
    EditProblem.BLANK_NAME -> R.string.history_edit_blank_name
    EditProblem.NO_EXERCISE -> R.string.history_edit_no_exercise
    EditProblem.MISSING_VALUES -> R.string.history_edit_missing_values
}
