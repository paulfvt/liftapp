package com.paulfvt.liftapp.workout.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.paulfvt.liftapp.core.designsystem.component.LiftButtonSize
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.SectionTitle
import com.paulfvt.liftapp.core.designsystem.component.SignalButton
import com.paulfvt.liftapp.core.designsystem.component.StatTile
import com.paulfvt.liftapp.core.designsystem.component.hairlineBorder
import com.paulfvt.liftapp.core.designsystem.theme.LocalExtendedColors
import com.paulfvt.liftapp.core.ui.LocalUnits
import com.paulfvt.liftapp.core.ui.durationText
import com.paulfvt.liftapp.core.ui.shortDay
import com.paulfvt.liftapp.core.ui.volumeText
import com.paulfvt.liftapp.core.ui.weightText
import com.paulfvt.liftapp.progress.data.ReachedTier
import com.paulfvt.liftapp.progress.domain.Badge
import com.paulfvt.liftapp.progress.ui.formatMetric
import com.paulfvt.liftapp.progress.ui.iconRes
import com.paulfvt.liftapp.progress.ui.labelRes
import com.paulfvt.liftapp.progress.ui.medalColor
import com.paulfvt.liftapp.progress.ui.recordLabelRes
import com.paulfvt.liftapp.progress.ui.titleRes
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
                SignalButton(
                    text = stringResource(R.string.workout_summary_close),
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    size = LiftButtonSize.Large
                )
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
                    volumeText(summary.tonnageKg),
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
            if (finished.achievements.badges.isNotEmpty()) UnlockedBadges(finished.achievements.badges)
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
    val unit = LocalUnits.current.weight
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
                                    formatMetric(resources, metric, recordBreak.value, exercise, unit),
                                    formatMetric(resources, metric, recordBreak.previous, exercise, unit)
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
                                    weightText(goal.valueOf(tier))
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
private fun UnlockedBadges(badges: List<Badge>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        SectionTitle(stringResource(R.string.summary_badges_title))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                badges.forEach { badge ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) {}
                    ) {
                        Icon(
                            painterResource(badge.iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(stringResource(badge.titleRes), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateUpdateCard(templateName: String, onUpdate: () -> Unit, onKeep: () -> Unit) {
    Card(
        border = hairlineBorder(),
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
                LiftTextButton(stringResource(R.string.workout_update_template_dismiss), onClick = onKeep)
                SignalButton(stringResource(R.string.workout_update_template_confirm), onClick = onUpdate)
            }
        }
    }
}
