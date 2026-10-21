package com.paulfvt.liftapp.history.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.MenuEntry
import com.paulfvt.liftapp.core.designsystem.component.OverflowMenu
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.designsystem.component.StatTile
import com.paulfvt.liftapp.core.domain.workSetNumbers
import com.paulfvt.liftapp.core.ui.dayAndTime
import com.paulfvt.liftapp.core.ui.durationText
import com.paulfvt.liftapp.core.ui.setName
import com.paulfvt.liftapp.core.ui.wholeNumber
import com.paulfvt.liftapp.exercise.ui.ExerciseThumbnail
import com.paulfvt.liftapp.history.domain.toHistoryItem
import com.paulfvt.liftapp.workout.domain.PreviousSet
import com.paulfvt.liftapp.workout.domain.Workout
import com.paulfvt.liftapp.workout.domain.WorkoutExercise
import com.paulfvt.liftapp.workout.domain.formatDecimal
import com.paulfvt.liftapp.workout.ui.previousLabel

// Détail d'une séance terminée, en lecture. Modifier et Supprimer sont dans le menu.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenProgress: (exerciseId: Long) -> Unit,
    viewModel: WorkoutDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val workout = uiState.workout
    // Séance supprimée (ici ou ailleurs) : plus rien à afficher.
    LaunchedEffect(uiState) {
        if (!uiState.isLoading && workout == null) onBack()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                title = { ScreenTitle(workout?.name.orEmpty()) },
                actions = {
                    OverflowMenu(
                        listOf(
                            MenuEntry(R.string.history_edit, R.drawable.ic_edit) { onEdit(viewModel.workoutId) },
                            MenuEntry(R.string.action_delete, R.drawable.ic_delete, isDestructive = true) {
                                viewModel.openDeleteDialog()
                            }
                        )
                    )
                }
            )
        }
    ) { padding ->
        if (workout != null) WorkoutDetailContent(workout, onOpenProgress, Modifier.padding(padding))
    }
    if (viewModel.isDeleteDialogOpen) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_text)) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::delete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun WorkoutDetailContent(workout: Workout, onOpenProgress: (Long) -> Unit, modifier: Modifier) {
    val item = workout.toHistoryItem()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    dayAndTime(workout.startedAt),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        stringResource(R.string.workout_summary_duration),
                        durationText(item.duration),
                        Modifier.weight(1f)
                    )
                    StatTile(
                        stringResource(R.string.workout_summary_tonnage),
                        stringResource(R.string.weight_kg, wholeNumber(item.tonnageKg)),
                        Modifier.weight(1f)
                    )
                    StatTile(
                        stringResource(R.string.workout_summary_sets),
                        item.setCount.toString(),
                        Modifier.weight(1f)
                    )
                }
                workout.note?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                }
            }
        }
        items(workout.exercises, key = { it.id }) { exercise ->
            ExerciseDetailCard(exercise, onOpenProgress = { onOpenProgress(exercise.exercise.id) })
        }
    }
}

@Composable
private fun ExerciseDetailCard(item: WorkoutExercise, onOpenProgress: () -> Unit) {
    val progressLabel = stringResource(R.string.history_open_progress, item.exercise.name)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, top = 12.dp)) {
            ExerciseThumbnail(item.exercise)
            Text(
                item.exercise.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            IconButton(onClick = onOpenProgress) {
                Icon(painterResource(R.drawable.ic_trending_up), contentDescription = progressLabel)
            }
        }
        val workNumbers = workSetNumbers(item.sets.map { it.type })
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
        ) {
            item.sets.forEachIndexed { index, set ->
                // Une ligne lue d'un seul tenant : « Série 2, 80 × 8, RPE 8 ».
                Row(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                    Text(
                        setName(set.type, workNumbers[index]),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(120.dp)
                    )
                    Text(
                        previousLabel(PreviousSet(set.type, set.values), item.exercise),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    set.rpe?.let {
                        Text(
                            stringResource(R.string.workout_rpe, formatDecimal(it)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}
