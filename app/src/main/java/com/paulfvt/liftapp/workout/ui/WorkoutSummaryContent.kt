package com.paulfvt.liftapp.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.StatTile
import com.paulfvt.liftapp.workout.data.FinishedWorkout
import java.text.NumberFormat
import java.time.Duration
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun WorkoutSummaryContent(
    finished: FinishedWorkout,
    isTemplateUpdated: Boolean,
    onUpdateTemplate: () -> Unit,
    onKeepTemplate: () -> Unit,
    onClose: () -> Unit
) {
    val summary = finished.summary
    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .heightIn(min = 56.dp)
                ) {
                    Text(stringResource(R.string.workout_summary_close), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                stringResource(R.string.workout_summary_title).uppercase(),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                finished.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    stringResource(R.string.workout_summary_duration),
                    durationText(summary.duration),
                    Modifier.weight(1f)
                )
                StatTile(
                    stringResource(R.string.workout_summary_tonnage),
                    stringResource(R.string.weight_kg, wholeNumber(summary.tonnageKg)),
                    Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    stringResource(R.string.workout_summary_sets),
                    summary.completedSets.toString(),
                    Modifier.weight(1f)
                )
                StatTile(
                    stringResource(R.string.workout_summary_exercises),
                    summary.exerciseCount.toString(),
                    Modifier.weight(1f)
                )
            }
            val update = finished.templateUpdate
            when {
                isTemplateUpdated -> Text(
                    stringResource(R.string.workout_template_updated),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                update != null -> TemplateUpdateCard(update.name, onUpdateTemplate, onKeepTemplate)
            }
        }
    }
}

@Composable
private fun TemplateUpdateCard(templateName: String, onUpdate: () -> Unit, onKeep: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.workout_update_template_title, templateName),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.workout_update_template_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onKeep) { Text(stringResource(R.string.workout_update_template_dismiss)) }
                Button(onClick = onUpdate) { Text(stringResource(R.string.workout_update_template_confirm)) }
            }
        }
    }
}

// « 58 min » ou « 1 h 05 ».
@Composable
private fun durationText(duration: Duration): String {
    val hours = duration.toHours().toInt()
    val minutes = (duration.toMinutes() % 60).toInt()
    return if (hours > 0) {
        stringResource(R.string.duration_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }
}

// Nombre entier avec séparateur de milliers français : « 9 840 ».
private fun wholeNumber(value: Double): String =
    NumberFormat.getIntegerInstance(Locale.FRENCH).format(value.roundToLong())
