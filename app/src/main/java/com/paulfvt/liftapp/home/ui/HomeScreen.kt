package com.paulfvt.liftapp.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.designsystem.component.SectionBlock
import com.paulfvt.liftapp.core.ui.LocalUnits
import com.paulfvt.liftapp.core.ui.shortDay
import com.paulfvt.liftapp.home.domain.LatestRecord
import com.paulfvt.liftapp.progress.ui.FlashbackCard
import com.paulfvt.liftapp.progress.ui.formatMetric
import com.paulfvt.liftapp.progress.ui.recordLabelRes
import com.paulfvt.liftapp.template.domain.TemplateSummary

// Accueil (tableau de bord) : la semaine, la prochaine séance avec « Démarrer », le dernier record et le souvenir.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenWorkout: () -> Unit, onOpenExercise: (Long) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isGoalDialogOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel.isWorkoutReady) {
        if (viewModel.isWorkoutReady) {
            onOpenWorkout()
            viewModel.onWorkoutOpened()
        }
    }
    val emptyName = stringResource(R.string.workout_empty_name)
    Scaffold(topBar = { TopAppBar(title = { ScreenTitle(stringResource(R.string.nav_home)) }) }) { padding ->
        val dashboard = uiState.dashboard ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "week") {
                SectionBlock(stringResource(R.string.week_title)) {
                    WeekCard(dashboard.streak, onEditGoal = { isGoalDialogOpen = true })
                }
            }
            item(key = "next") {
                SectionBlock(stringResource(R.string.home_next_session)) {
                    NextSessionCard(
                        liveWorkoutName = uiState.liveWorkoutName,
                        template = dashboard.nextTemplate,
                        onStart = { templateId -> viewModel.start(templateId, emptyName) }
                    )
                }
            }
            dashboard.latestRecord?.let { latest ->
                item(key = "record") {
                    SectionBlock(stringResource(R.string.home_latest_record)) {
                        LatestRecordCard(latest, onClick = { onOpenExercise(latest.exercise.id) })
                    }
                }
            }
            dashboard.flashback?.let { flashback ->
                item(key = "flashback") {
                    SectionBlock(stringResource(R.string.flashback_title)) {
                        FlashbackCard(flashback, onClick = { onOpenExercise(flashback.exercise.id) })
                    }
                }
            }
        }
    }
    val streak = uiState.dashboard?.streak
    if (isGoalDialogOpen && streak != null) {
        WeeklyGoalDialog(
            currentGoal = streak.currentWeek.goal,
            onConfirm = { goal ->
                viewModel.onWeeklyGoalChange(goal)
                isGoalDialogOpen = false
            },
            onDismiss = { isGoalDialogOpen = false }
        )
    }
}

// Séance en cours à reprendre, sinon la prochaine séance proposée, sinon une séance vide (aucun template).
// onStart(null) lance une séance vide.
@Composable
private fun NextSessionCard(liveWorkoutName: String?, template: TemplateSummary?, onStart: (Long?) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                liveWorkoutName != null -> {
                    Text(stringResource(R.string.home_live_workout), style = MaterialTheme.typography.labelLarge)
                    Text(liveWorkoutName, style = MaterialTheme.typography.titleLarge)
                    PrimaryButton(stringResource(R.string.workout_resume), onClick = { onStart(null) })
                }
                template != null -> {
                    Text(template.name, style = MaterialTheme.typography.titleLarge)
                    if (template.exerciseNames.isNotEmpty()) {
                        Text(
                            template.exerciseNames.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    PrimaryButton(stringResource(R.string.template_preview_start), onClick = { onStart(template.id) })
                }
                else -> {
                    Text(
                        stringResource(R.string.home_no_template),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrimaryButton(stringResource(R.string.workout_start_empty), onClick = { onStart(null) })
                }
            }
        }
    }
}

// Action principale de l'écran, en rouge signal (guide de style ON AIR).
@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).heightIn(min = 56.dp)
    ) {
        Icon(painterResource(R.drawable.ic_play_arrow), contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
    }
}

// « Squat à la barre · 1RM estimé : 135 kg, le 28 oct. 2026 ». Un appui ouvre la progression de l'exercice.
@Composable
private fun LatestRecordCard(latest: LatestRecord, onClick: () -> Unit) {
    val resources = LocalResources.current
    val (exercise, recordBreak) = latest
    val metric = recordBreak.metric
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp).semantics(mergeDescendants = true) {}
        ) {
            Icon(
                painterResource(R.drawable.ic_emoji_events),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(metric.recordLabelRes(exercise.trackingType)) + " : " +
                        formatMetric(resources, metric, recordBreak.value, exercise, LocalUnits.current.weight),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    stringResource(R.string.progress_record_date, shortDay(recordBreak.achievedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
