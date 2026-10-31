package com.paulfvt.liftapp.progress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.ui.weightText
import com.paulfvt.liftapp.progress.domain.MonthPart
import com.paulfvt.liftapp.progress.domain.ProjectionResult
import com.paulfvt.liftapp.progress.domain.monthPart
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MONTH = DateTimeFormatter.ofPattern("MMMM", Locale.FRENCH)
private val MONTH_AND_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)

// Projection du 1RM estimé : une fourchette et une période, jamais une date précise. Quand elle n'est pas possible,
// la carte explique pourquoi.
@Composable
fun ProjectionCard(result: ProjectionResult, today: LocalDate, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(16.dp).semantics(mergeDescendants = true) {}
        ) {
            when (result) {
                is ProjectionResult.Available -> {
                    val projection = result.projection
                    Text(
                        stringResource(
                            R.string.projection_available,
                            weightText(projection.lowKg),
                            weightText(projection.highKg),
                            periodText(projection.horizon, today)
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        stringResource(R.string.projection_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ProjectionResult.NotEnoughData -> Explanation(R.string.projection_not_enough)
                ProjectionResult.NoProgress -> Explanation(R.string.projection_no_progress)
                ProjectionResult.TooIrregular -> Explanation(R.string.projection_irregular)
            }
        }
    }
}

@Composable
private fun Explanation(textRes: Int) {
    Text(
        stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// « fin janvier », avec l'année seulement si ce n'est pas l'année en cours (« début janvier 2027 »).
@Composable
private fun periodText(date: LocalDate, today: LocalDate): String {
    val month = (if (date.year == today.year) MONTH else MONTH_AND_YEAR).format(date)
    return when (monthPart(date)) {
        MonthPart.EARLY -> stringResource(R.string.projection_period_early, month)
        MonthPart.MID -> stringResource(R.string.projection_period_mid, month)
        MonthPart.LATE -> stringResource(R.string.projection_period_late, month)
    }
}
