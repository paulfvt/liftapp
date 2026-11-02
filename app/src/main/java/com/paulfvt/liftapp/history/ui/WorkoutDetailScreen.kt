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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftCard
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.MenuEntry
import com.paulfvt.liftapp.core.designsystem.component.OverflowMenu
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.designsystem.component.StatTile
import com.paulfvt.liftapp.core.domain.workSetNumbers
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.core.ui.LocalUnits
import com.paulfvt.liftapp.core.ui.dayAndTime
import com.paulfvt.liftapp.core.ui.durationText
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.core.ui.setName
import com.paulfvt.liftapp.core.ui.volumeText
import com.paulfvt.liftapp.core.ui.weightInput
import com.paulfvt.liftapp.exercise.ui.ExerciseThumbnail
import com.paulfvt.liftapp.history.domain.toHistoryItem
import com.paulfvt.liftapp.workout.domain.DistanceUnit
import com.paulfvt.liftapp.workout.domain.SetValues
import com.paulfvt.liftapp.workout.domain.Workout
import com.paulfvt.liftapp.workout.domain.WorkoutExercise
import com.paulfvt.liftapp.workout.domain.distanceUnitFor
import com.paulfvt.liftapp.workout.domain.formatDecimal
import com.paulfvt.liftapp.workout.domain.formatDistance
import com.paulfvt.liftapp.workout.domain.formatDuration

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
                LiftTextButton(
                    text = stringResource(R.string.action_delete),
                    onClick = viewModel::delete,
                    color = MaterialTheme.colorScheme.error
                )
            },
            dismissButton = {
                LiftTextButton(stringResource(R.string.action_cancel), onClick = viewModel::dismissDeleteDialog)
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
                // Deux tuiles par ligne, comme le résumé de fin de séance : les chiffres restent sur une ligne.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        stringResource(R.string.workout_summary_duration),
                        durationText(item.duration),
                        Modifier.weight(1f)
                    )
                    StatTile(
                        stringResource(R.string.workout_summary_tonnage),
                        volumeText(item.tonnageKg),
                        Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        stringResource(R.string.workout_summary_sets),
                        item.setCount.toString(),
                        Modifier.weight(1f)
                    )
                    StatTile(
                        stringResource(R.string.workout_summary_exercises),
                        item.exerciseNames.size.toString(),
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
                        performedSetText(set.values, item.exercise),
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

// Poids d'une série faite dans l'unité choisie : « 80 kg », « 176,4 lb ».
@Composable
private fun weightWithUnit(kg: Double): String {
    val unit = LocalUnits.current.weight
    return stringResource(R.string.value_with_unit, weightInput(kg, unit), stringResource(unit.labelRes))
}

// « 80 kg × 8 », « +10 kg × 8 » (lest), « 12 reps », « 1:30 », « 5,2 km · 25:00 ».
@Composable
private fun performedSetText(values: SetValues, exercise: Exercise): String {
    val reps = values.reps
    val weight = values.weightKg
    return when (exercise.trackingType) {
        TrackingType.WEIGHT_REPS, TrackingType.REPS -> when {
            reps == null -> stringResource(R.string.workout_previous_none)
            weight == null || (exercise.trackingType == TrackingType.REPS && weight == 0.0) ->
                pluralStringResource(R.plurals.workout_previous_reps, reps, reps)
            exercise.trackingType == TrackingType.REPS && weight > 0 ->
                stringResource(R.string.weight_reps, "+" + weightWithUnit(weight), reps)
            else -> stringResource(R.string.weight_reps, weightWithUnit(weight), reps)
        }
        TrackingType.DURATION -> formatDuration(values.durationSec)
        TrackingType.DISTANCE -> {
            val unit = distanceUnitFor(exercise.category)
            val unitLabel = stringResource(
                if (unit == DistanceUnit.KILOMETER) R.string.workout_col_km else R.string.workout_col_m
            )
            listOfNotNull(
                values.distanceM?.let { "${formatDistance(it, unit)} $unitLabel" },
                values.durationSec?.let(::formatDuration)
            ).joinToString(" · ")
        }
    }
}
