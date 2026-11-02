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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftCard
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.designsystem.component.SectionBlock
import com.paulfvt.liftapp.core.designsystem.component.SectionTitle
import com.paulfvt.liftapp.core.ui.monthTitle
import com.paulfvt.liftapp.core.ui.shortDay
import com.paulfvt.liftapp.exercise.ui.ExerciseThumbnail
import com.paulfvt.liftapp.progress.domain.Badge
import com.paulfvt.liftapp.progress.domain.MonthlyRecap
import com.paulfvt.liftapp.progress.domain.PerformedExercise
import com.paulfvt.liftapp.progress.domain.metricsFor
import java.time.YearMonth

// Onglet Progrès : carte du corps, objectifs, badges et récap en haut, puis chaque exercice déjà fait avec son record
// principal. La semaine et le souvenir sont sur l'accueil.
// Un appui sur un exercice ouvre ses records et ses courbes.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onOpenExercise: (Long) -> Unit,
    onOpenRecap: (YearMonth) -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var openBadge by rememberSaveable { mutableStateOf<Badge?>(null) }
    Scaffold(topBar = { TopAppBar(title = { ScreenTitle(stringResource(R.string.nav_progress)) }) }) { padding ->
        if (uiState.isLoading) return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.bodyFigures.isNotEmpty()) {
                item(key = "body-map") {
                    SectionBlock(stringResource(R.string.body_map_title)) {
                        BodyMapCard(uiState.muscleSets, uiState.bodyFigures)
                    }
                }
            }
            if (uiState.goals.isNotEmpty()) {
                item(key = "goals") {
                    SectionBlock(stringResource(R.string.goals_title)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            uiState.goals.forEach { (exercise, status) ->
                                GoalCard(status, title = exercise.name, onClick = { onOpenExercise(exercise.id) })
                            }
                        }
                    }
                }
            }
            item(key = "badges") {
                SectionBlock(stringResource(R.string.badges_title, uiState.badges.size, Badge.entries.size)) {
                    BadgeGrid(uiState.badges, onBadgeClick = { openBadge = it })
                }
            }
            uiState.monthRecap?.let { recap ->
                item(key = "recap") {
                    SectionBlock(stringResource(R.string.recap_title)) {
                        RecapEntryCard(recap, onClick = { onOpenRecap(recap.month) })
                    }
                }
            }
            item(key = "exercises-title") {
                SectionTitle(stringResource(R.string.progress_exercises), Modifier.padding(top = 8.dp))
            }
            if (uiState.exercises.isEmpty()) {
                item(key = "exercises-empty") { NoExerciseYet() }
            }
            items(uiState.exercises, key = { it.exercise.id }) { performed ->
                PerformedExerciseCard(performed, onClick = { onOpenExercise(performed.exercise.id) })
            }
        }
    }
    openBadge?.let { badge ->
        BadgeDialog(badge, uiState.badges[badge], onDismiss = { openBadge = null })
    }
}

// Entrée du récap : le mois en cours en une ligne, l'écran complet au toucher.
@Composable
private fun RecapEntryCard(recap: MonthlyRecap, onClick: () -> Unit) {
    LiftCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(monthTitle(recap.month), style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(R.plurals.progress_sessions, recap.workoutCount, recap.workoutCount) +
                        " · " + tonnageText(recap.tonnageKg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null)
        }
    }
}

@Composable
private fun NoExerciseYet() {
    LiftCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.progress_empty_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.progress_empty_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PerformedExerciseCard(performed: PerformedExercise, onClick: () -> Unit) {
    val exercise = performed.exercise
    LiftCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
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
