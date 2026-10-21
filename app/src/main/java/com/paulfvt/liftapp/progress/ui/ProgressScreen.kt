package com.paulfvt.liftapp.progress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.EmptyState
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.ui.shortDay
import com.paulfvt.liftapp.exercise.ui.ExerciseThumbnail
import com.paulfvt.liftapp.progress.domain.PerformedExercise
import com.paulfvt.liftapp.progress.domain.metricsFor

// Onglet Progrès : chaque exercice déjà fait, avec son record principal. Un appui ouvre ses records et ses courbes.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(onOpenExercise: (Long) -> Unit, viewModel: ProgressViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { ScreenTitle(stringResource(R.string.nav_progress)) }) }) { padding ->
        when {
            uiState.isLoading -> Unit
            uiState.exercises.isEmpty() -> EmptyState(
                iconRes = R.drawable.ic_trending_up,
                title = stringResource(R.string.progress_empty_title),
                text = stringResource(R.string.progress_empty_text),
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.exercises, key = { it.exercise.id }) { performed ->
                    PerformedExerciseCard(performed, onClick = { onOpenExercise(performed.exercise.id) })
                }
            }
        }
    }
}

@Composable
private fun PerformedExerciseCard(performed: PerformedExercise, onClick: () -> Unit) {
    val exercise = performed.exercise
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            ExerciseThumbnail(exercise)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(R.string.progress_last_time, shortDay(performed.lastPerformedAt)) + " · " +
                        pluralStringResource(
                            R.plurals.progress_sessions,
                            performed.workoutCount,
                            performed.workoutCount
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            performed.headline?.let { record ->
                val metric = metricsFor(exercise.trackingType).first()
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(metric.recordLabelRes(exercise.trackingType)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        rememberMetricFormatter(metric, exercise)(record.value),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
