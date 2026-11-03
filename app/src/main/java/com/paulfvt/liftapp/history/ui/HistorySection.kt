package com.paulfvt.liftapp.history.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftCard
import com.paulfvt.liftapp.core.designsystem.component.SectionTitle
import com.paulfvt.liftapp.core.ui.dayAndTime
import com.paulfvt.liftapp.core.ui.durationText
import com.paulfvt.liftapp.core.ui.monthTitle
import com.paulfvt.liftapp.core.ui.volumeText
import com.paulfvt.liftapp.history.domain.HistoryItem
import com.paulfvt.liftapp.history.domain.MonthGroup

// Historique sous les templates de l'onglet Séances (décision du 3 novembre 2026) : séances terminées par mois,
// la plus récente en haut. Ajouté à la liste de l'onglet, d'où l'extension de LazyListScope.
@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.historySection(months: List<MonthGroup<HistoryItem>>, onOpenWorkout: (Long) -> Unit) {
    item(key = "history-title") {
        SectionTitle(stringResource(R.string.history_title), Modifier.padding(top = 24.dp, bottom = 4.dp))
    }
    if (months.isEmpty()) {
        item(key = "history-empty") { HistoryEmpty() }
        return
    }
    months.forEach { group ->
        // En-tête collé en haut tant que ses séances défilent.
        stickyHeader(key = "month-${group.month}") {
            Text(
                monthTitle(group.month),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 12.dp, bottom = 4.dp)
                    .semantics { heading() }
            )
        }
        // Préfixe : les identifiants des séances pourraient croiser ceux des templates de la même liste.
        items(group.items, key = { "workout-${it.id}" }) { item ->
            HistoryCard(item, onClick = { onOpenWorkout(item.id) })
        }
    }
}

@Composable
private fun HistoryEmpty() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.history_empty_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryCard(item: HistoryItem, onClick: () -> Unit) {
    LiftCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                item.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                dayAndTime(item.startedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                listOf(
                    durationText(item.duration),
                    volumeText(item.tonnageKg),
                    pluralStringResource(R.plurals.history_sets, item.setCount, item.setCount)
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                item.exerciseNames.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
