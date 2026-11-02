package com.paulfvt.liftapp.profile.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.ButtonLabel
import com.paulfvt.liftapp.core.designsystem.component.LiftCard
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.designsystem.component.SectionTitle
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.core.ui.LocalUnits
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.core.ui.shortDay
import com.paulfvt.liftapp.profile.domain.BodyMeasurement
import com.paulfvt.liftapp.profile.domain.toDisplay
import com.paulfvt.liftapp.progress.domain.ChartPeriod
import com.paulfvt.liftapp.progress.ui.ProgressChart
import com.paulfvt.liftapp.progress.ui.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementScreen(
    onBack: () -> Unit,
    onAdd: (MeasurementType) -> Unit,
    viewModel: MeasurementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                title = { ScreenTitle(stringResource(uiState.type.labelRes)) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAdd(uiState.type) },
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { ButtonLabel(stringResource(R.string.measurement_add)) }
            )
        }
    ) { padding ->
        if (uiState.isLoading) return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            // Marge du bas : la dernière mesure n'est pas cachée par le bouton flottant.
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Header(uiState) }
            if (uiState.entries.isNotEmpty()) {
                item { ChartSection(uiState, viewModel::onPeriodSelected) }
                item { SectionTitle(stringResource(R.string.measurement_history), Modifier.padding(top = 12.dp)) }
                items(uiState.entries, key = { it.id }) { entry ->
                    EntryRow(entry, onDelete = { pendingDelete = entry.id })
                }
            }
        }
    }

    val toDelete = uiState.entries.firstOrNull { it.id == pendingDelete }
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.measurement_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.measurement_delete_text,
                        measureText(toDelete.type, toDelete.value),
                        shortDay(toDelete.measuredAt)
                    )
                )
            },
            confirmButton = {
                LiftTextButton(
                    text = stringResource(R.string.action_delete),
                    onClick = {
                        viewModel.delete(toDelete.id)
                        pendingDelete = null
                    }
                )
            },
            dismissButton = {
                LiftTextButton(stringResource(R.string.action_cancel), onClick = { pendingDelete = null })
            }
        )
    }
}

// Dernière valeur en grand, sa date et l'écart avec la précédente.
@Composable
private fun Header(uiState: MeasurementUiState) {
    val summary = uiState.summary
    if (summary == null) {
        Text(
            stringResource(R.string.measurements_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        return
    }
    val latest = summary.latest
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            measureText(latest.type, latest.value),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
        val details = buildList {
            add(shortDay(latest.measuredAt))
            summary.change?.let { add(changeText(latest.type, it)) }
        }
        Text(
            details.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChartSection(uiState: MeasurementUiState, onPeriodSelected: (ChartPeriod) -> Unit) {
    val type = uiState.type
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.progress_chart))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        ) {
            ChartPeriod.entries.forEach { period ->
                FilterChip(
                    selected = period == uiState.period,
                    onClick = { onPeriodSelected(period) },
                    label = { Text(stringResource(period.labelRes())) }
                )
            }
        }
        val days = uiState.chartDays
        if (days.isEmpty()) {
            Text(
                stringResource(R.string.measurement_none_in_period),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
            return@Column
        }
        val units = LocalUnits.current
        val format = rememberMeasureFormatter(type)
        val summary = pluralStringResource(R.plurals.measurement_days, days.size, days.size) + " · " +
            stringResource(
                R.string.progress_chart_range,
                measureText(type, days.first().value),
                measureText(type, days.last().value)
            )
        ProgressChart(
            values = days.map { it.copy(value = type.toDisplay(it.value, units)) },
            valueText = format,
            description = stringResource(type.labelRes) + ". " + summary,
            color = MaterialTheme.colorScheme.primary,
            trend = uiState.trend.map { it.copy(value = type.toDisplay(it.value, units)) },
            trendLabel = stringResource(R.string.measurement_trend_short)
        )
        Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (uiState.trend.isNotEmpty()) {
            Text(
                stringResource(R.string.measurement_trend_legend),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// « 79,4 kg · 21 oct. 2026 », avec la méthode pour la masse grasse, et un bouton de suppression.
@Composable
private fun EntryRow(entry: BodyMeasurement, onDelete: () -> Unit) {
    LiftCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
        ) {
            Column(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(measureText(entry.type, entry.value), style = MaterialTheme.typography.titleMedium)
                val details = listOfNotNull(
                    shortDay(entry.measuredAt),
                    entry.method?.let { stringResource(it.labelRes) }
                )
                Text(
                    details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painterResource(R.drawable.ic_delete),
                    stringResource(R.string.measurement_delete_description, shortDay(entry.measuredAt))
                )
            }
        }
    }
}
