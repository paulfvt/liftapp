package com.paulfvt.liftapp.progress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftCard
import com.paulfvt.liftapp.core.designsystem.theme.LocalExtendedColors
import com.paulfvt.liftapp.progress.domain.Flashback
import kotlin.math.abs

// Carte souvenir : la performance d'il y a quelques mois face à la dernière séance. En progrès, l'écart est mis en
// avant en vert ; sinon la formulation reste neutre (décision du 28 octobre 2026).
@Composable
fun FlashbackCard(flashback: Flashback, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val exercise = flashback.exercise
    val format = rememberMetricFormatter(flashback.metric, exercise)
    val metricName = stringResource(flashback.metric.chartLabelRes(exercise.trackingType))
    LiftCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp).semantics(mergeDescendants = true) {}
        ) {
            Icon(
                painterResource(R.drawable.ic_history),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    pluralStringResource(R.plurals.flashback_ago, flashback.months, flashback.months).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        R.string.flashback_values,
                        metricName,
                        format(flashback.then.value),
                        format(flashback.now.value)
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
                val change = flashback.change
                val changeColor = if (change > 0) {
                    LocalExtendedColors.current.success
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    when {
                        change > 0 -> stringResource(R.string.flashback_gain, format(change))
                        change < 0 -> stringResource(R.string.flashback_lower, format(abs(change)))
                        else -> stringResource(R.string.flashback_same)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = changeColor
                )
            }
        }
    }
}
