package com.paulfvt.liftapp.exercise.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.NoteDialog
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.designsystem.component.hairlineBorder
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.exercise.ui.ExerciseImageAnimation

@Composable
fun ExerciseDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenProgress: (Long) -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseDetailContent(
        uiState = uiState,
        onBack = onBack,
        onEdit = onEdit,
        onOpenProgress = onOpenProgress,
        onSaveNote = viewModel::saveNote,
        onSetHidden = viewModel::setHidden
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDetailContent(
    uiState: ExerciseDetailUiState,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenProgress: (Long) -> Unit,
    onSaveNote: (String) -> Unit,
    onSetHidden: (Boolean) -> Unit
) {
    val exercise = (uiState as? ExerciseDetailUiState.Loaded)?.exercise
    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle(exercise?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (exercise != null) {
                        // Records et courbes de l'exercice (onglet Progrès).
                        IconButton(onClick = { onOpenProgress(exercise.id) }) {
                            Icon(
                                painterResource(R.drawable.ic_trending_up),
                                stringResource(R.string.exercise_open_progress)
                            )
                        }
                        if (exercise.isCustom) {
                            IconButton(onClick = { onEdit(exercise.id) }) {
                                Icon(
                                    painterResource(R.drawable.ic_edit),
                                    stringResource(R.string.action_edit)
                                )
                            }
                        }
                        HideMenu(isHidden = exercise.isHidden, onSetHidden = onSetHidden)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                ExerciseDetailUiState.Loading -> CircularProgressIndicator(
                    Modifier.align(Alignment.Center)
                )
                ExerciseDetailUiState.NotFound -> Text(
                    text = stringResource(R.string.exercise_not_found),
                    modifier = Modifier.align(Alignment.Center)
                )
                is ExerciseDetailUiState.Loaded -> ExerciseDetailBody(
                    exercise = uiState.exercise,
                    onSaveNote = onSaveNote,
                    onSetHidden = onSetHidden
                )
            }
        }
    }
}

@Composable
private fun HideMenu(isHidden: Boolean, onSetHidden: (Boolean) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (isHidden) R.string.exercise_unhide else R.string.exercise_hide
                        )
                    )
                },
                onClick = {
                    expanded = false
                    onSetHidden(!isHidden)
                }
            )
        }
    }
}

@Composable
private fun ExerciseDetailBody(exercise: Exercise, onSaveNote: (String) -> Unit, onSetHidden: (Boolean) -> Unit) {
    var isEditingNote by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (exercise.isHidden) {
            Card(
                border = hairlineBorder(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.exercise_hidden_banner),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    LiftTextButton(stringResource(R.string.exercise_unhide), onClick = { onSetHidden(false) })
                }
            }
        }

        ExerciseImageAnimation(exercise)

        NoteSection(note = exercise.note, onEdit = { isEditingNote = true })

        MuscleSection(R.string.exercise_primary_muscles, exercise.primaryMuscles)
        MuscleSection(R.string.exercise_secondary_muscles, exercise.secondaryMuscles)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoLine(R.string.exercise_equipment, stringResource(exercise.equipment.labelRes))
            InfoLine(R.string.exercise_category, stringResource(exercise.category.labelRes))
            InfoLine(R.string.exercise_tracking, stringResource(exercise.trackingType.labelRes))
        }

        if (exercise.instructions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(R.string.exercise_instructions)
                exercise.instructions.forEachIndexed { index, step ->
                    Text("${index + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (!exercise.isCustom) {
            Text(
                text = stringResource(R.string.exercise_source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (isEditingNote) {
        NoteDialog(
            title = stringResource(R.string.exercise_note),
            hint = stringResource(R.string.exercise_note_hint),
            initialNote = exercise.note.orEmpty(),
            onDismiss = { isEditingNote = false },
            onSave = { note ->
                isEditingNote = false
                onSaveNote(note)
            }
        )
    }
}

// La note est mise en avant : c'est elle qui sera rappelée à chaque séance (réglage de machine, consignes).
@Composable
private fun NoteSection(note: String?, onEdit: () -> Unit) {
    Card(border = hairlineBorder(), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.exercise_note),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                val buttonLabel = if (note == null) R.string.exercise_note_add else R.string.action_edit
                LiftTextButton(stringResource(buttonLabel), onClick = onEdit)
            }
            if (note != null) {
                Text(
                    note,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MuscleSection(titleRes: Int, muscles: List<Muscle>) {
    if (muscles.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(titleRes)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Puces d'information, non cliquables : onClick vide et pas d'action pour le lecteur d'écran.
            muscles.forEach { muscle ->
                AssistChip(onClick = {
                }, enabled = false, label = { Text(stringResource(muscle.labelRes)) })
            }
        }
    }
}

@Composable
private fun InfoLine(labelRes: Int, value: String) {
    Row {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionTitle(titleRes: Int) {
    Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall)
}
