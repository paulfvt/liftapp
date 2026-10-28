package com.paulfvt.liftapp.workout.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.SectionTitle
import com.paulfvt.liftapp.core.designsystem.component.StatTile
import com.paulfvt.liftapp.core.designsystem.theme.LocalExtendedColors
import com.paulfvt.liftapp.core.ui.durationText
import com.paulfvt.liftapp.core.ui.kgText
import com.paulfvt.liftapp.core.ui.shortDay
import com.paulfvt.liftapp.core.ui.wholeNumber
import com.paulfvt.liftapp.progress.data.ReachedTier
import com.paulfvt.liftapp.progress.ui.formatMetric
import com.paulfvt.liftapp.progress.ui.labelRes
import com.paulfvt.liftapp.progress.ui.medalColor
import com.paulfvt.liftapp.progress.ui.recordLabelRes
import com.paulfvt.liftapp.workout.data.FinishedRecord
import com.paulfvt.liftapp.workout.data.FinishedWorkout
import com.paulfvt.liftapp.workout.domain.tonnageChange
import java.time.Instant
import kotlin.math.roundToInt

private const val PERCENT = 100

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
            tonnageChange(summary.tonnageKg, finished.previousTonnageKg)?.let { change ->
                TonnageComparison(change, finished.previousStartedAt)
            }
            if (finished.records.isNotEmpty()) BrokenRecords(finished.records)
            if (finished.achievements.tiers.isNotEmpty()) ReachedTiers(finished.achievements.tiers)
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

// « +6 % de volume par rapport au 21 oct. 2026 », en vert seulement quand le volume augmente.
@Composable
private fun TonnageComparison(change: Double, previousStartedAt: Instant?) {
    val percent = (change * PERCENT).roundToInt()
    val signed = when {
        percent > 0 -> "+$percent %"
        percent < 0 -> "−${-percent} %"
        else -> "0 %"
    }
    Text(
        previousStartedAt?.let { stringResource(R.string.summary_volume_change, signed, shortDay(it)) }
            ?: stringResource(R.string.summary_volume_change_no_date, signed),
        style = MaterialTheme.typography.bodyLarge,
        color = if (percent > 0) LocalExtendedColors.current.success else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun BrokenRecords(records: List<FinishedRecord>) {
    val resources = LocalResources.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        SectionTitle(stringResource(R.string.summary_records_title))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                records.forEach { (exercise, recordBreak) ->
                    val metric = recordBreak.metric
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) {}
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_emoji_events),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Column {
                            Text(exercise.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(
                                    R.string.summary_record_line,
                                    stringResource(metric.recordLabelRes(exercise.trackingType)),
                                    formatMetric(resources, metric, recordBreak.value, exercise),
                                    formatMetric(resources, metric, recordBreak.previous, exercise)
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// Paliers d'objectifs atteints par cette séance, avec la couleur de leur médaille.
@Composable
private fun ReachedTiers(tiers: List<ReachedTier>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        SectionTitle(stringResource(R.string.summary_tiers_title))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                tiers.forEach { (exercise, goal, tier) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) {}
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_military_tech),
                            contentDescription = null,
                            tint = tier.medalColor
                        )
                        Column {
                            Text(exercise.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(
                                    R.string.summary_tier_line,
                                    stringResource(tier.labelRes),
                                    kgText(goal.valueOf(tier))
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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
