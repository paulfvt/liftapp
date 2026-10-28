package com.paulfvt.liftapp.progress.ui

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.EmptyState
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.designsystem.component.SectionTitle
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.PersonalRecordType
import com.paulfvt.liftapp.core.ui.kgText
import com.paulfvt.liftapp.core.ui.shortDay
import com.paulfvt.liftapp.progress.domain.ChartPeriod
import com.paulfvt.liftapp.progress.domain.ExerciseMetric
import com.paulfvt.liftapp.progress.domain.GoalTier
import com.paulfvt.liftapp.progress.domain.PersonalRecord
import com.paulfvt.liftapp.progress.domain.recordType
import com.paulfvt.liftapp.workout.domain.formatDecimal

// Progression d'un exercice : records datés, puis la courbe de la mesure et de la période choisies.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressScreen(onBack: () -> Unit, viewModel: ExerciseProgressViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exercise = uiState.exercise
    var isGoalDialogOpen by rememberSaveable { mutableStateOf(false) }
    if (isGoalDialogOpen) {
        GoalDialog(
            currentKg = uiState.currentOneRepMaxKg,
            onCreate = { tiers ->
                viewModel.onCreateGoal(tiers)
                isGoalDialogOpen = false
            },
            onDismiss = { isGoalDialogOpen = false }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                title = { ScreenTitle(exercise?.name.orEmpty()) }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading || exercise == null -> Unit
            !uiState.hasHistory -> EmptyState(
                iconRes = R.drawable.ic_trending_up,
                title = stringResource(R.string.progress_no_history_title),
                text = stringResource(R.string.progress_no_history_text),
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "records") { RecordsSection(exercise, uiState.records, uiState.metrics) }
                if (uiState.canHaveGoal) {
                    item(key = "goal") {
                        GoalSection(uiState, onAdd = { isGoalDialogOpen = true }, onDelete = viewModel::onDeleteGoal)
                    }
                }
                item(key = "chart") {
                    ChartSection(
                        exercise = exercise,
                        uiState = uiState,
                        onMetricSelected = viewModel::onMetricSelected,
                        onPeriodSelected = viewModel::onPeriodSelected
                    )
                }
                val repsAtWeight = uiState.records.filter { it.type == PersonalRecordType.MAX_REPS_AT_WEIGHT }
                if (repsAtWeight.isNotEmpty()) {
                    item(key = "reps-at-weight") { RepsAtWeightSection(repsAtWeight) }
                }
            }
        }
    }
}

// Objectif en cours (barre et paliers), sinon un bouton pour en fixer un, avec le dernier objectif atteint.
@Composable
private fun GoalSection(uiState: ExerciseProgressUiState, onAdd: () -> Unit, onDelete: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.goal_section))
        val active = uiState.activeGoal
        if (active != null) {
            GoalCard(active, onDelete = { onDelete(active.goal.id) })
        } else {
            uiState.completedGoals.firstOrNull()?.let { done ->
                val date = done.reachedAt[GoalTier.GOLD]?.let { shortDay(it) }.orEmpty()
                Text(
                    stringResource(R.string.goal_target, kgText(done.goldKg)) + " · " +
                        stringResource(R.string.goal_completed_on, date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                stringResource(R.string.goal_add_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onAdd, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(
                    painterResource(R.drawable.ic_flag),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Text(stringResource(R.string.goal_add), modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
            }
        }
    }
}

@Composable
private fun RecordsSection(exercise: Exercise, records: List<PersonalRecord>, metrics: List<ExerciseMetric>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.progress_records))
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                metrics.forEach { metric ->
                    val record = records.firstOrNull { it.type == metric.recordType() }
                    RecordRow(
                        label = stringResource(metric.recordLabelRes(exercise.trackingType)),
                        value = record?.let { rememberMetricFormatter(metric, exercise)(it.value) }
                            ?: stringResource(R.string.workout_previous_none),
                        date = record?.let { shortDay(it.achievedAt) }
                    )
                }
            }
        }
    }
}

// Ligne lue d'un seul tenant : « 1RM estimé, 116,5 kg, le 14 oct. 2026 ».
@Composable
private fun RecordRow(label: String, value: String, date: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            date?.let {
                Text(
                    stringResource(R.string.progress_record_date, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun ChartSection(
    exercise: Exercise,
    uiState: ExerciseProgressUiState,
    onMetricSelected: (ExerciseMetric) -> Unit,
    onPeriodSelected: (ChartPeriod) -> Unit
) {
    val metric = uiState.metric ?: return
    val format = rememberMetricFormatter(metric, exercise)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.progress_chart))
        if (uiState.metrics.size > 1) {
            ChipRow {
                uiState.metrics.forEach { option ->
                    FilterChip(
                        selected = option == metric,
                        onClick = { onMetricSelected(option) },
                        label = { Text(stringResource(option.chartLabelRes(exercise.trackingType))) }
                    )
                }
            }
        }
        ChipRow {
            ChartPeriod.entries.forEach { period ->
                FilterChip(
                    selected = period == uiState.period,
                    onClick = { onPeriodSelected(period) },
                    label = { Text(stringResource(period.labelRes())) }
                )
            }
        }
        val points = uiState.points
        if (points.isEmpty()) {
            Text(
                stringResource(R.string.progress_no_session_in_period),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            val resources = LocalResources.current
            val summary = resources.getQuantityString(R.plurals.progress_sessions, points.size, points.size) + " · " +
                resources.getString(
                    R.string.progress_chart_range,
                    format(points.first().value),
                    format(points.last().value)
                )
            val metricName = stringResource(metric.chartLabelRes(exercise.trackingType))
            ProgressChart(values = uiState.chartDays, valueText = format, description = "$metricName. $summary")
            // Résumé visible, utile à tous : d'où l'on part et où l'on en est sur la période.
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    ) { content() }
}

fun ChartPeriod.labelRes(): Int = when (this) {
    ChartPeriod.MONTH -> R.string.period_month
    ChartPeriod.THREE_MONTHS -> R.string.period_three_months
    ChartPeriod.YEAR -> R.string.period_year
    ChartPeriod.ALL -> R.string.period_all
}

// Reps max à chaque charge, de la plus lourde à la plus légère : « 100 kg · 5 reps ».
@Composable
private fun RepsAtWeightSection(records: List<PersonalRecord>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.record_reps_at_weight))
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                records.sortedByDescending { it.weightKg }.forEach { record ->
                    val reps = record.value.toInt()
                    RecordRow(
                        label = stringResource(R.string.weight_kg, formatDecimal(record.weightKg)),
                        value = pluralStringResource(R.plurals.workout_previous_reps, reps, reps),
                        date = shortDay(record.achievedAt)
                    )
                }
            }
        }
    }
}
