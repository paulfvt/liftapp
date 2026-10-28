package com.paulfvt.liftapp.progress.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.progress.domain.WEEKLY_GOAL_RANGE

// Choix du nombre de séances par semaine, de 1 à 7 : écran de bienvenue et carte de la semaine.
@Composable
fun WeeklyGoalStepper(goal: Int, onGoalChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        FilledTonalIconButton(
            onClick = { onGoalChange(goal - 1) },
            enabled = goal > WEEKLY_GOAL_RANGE.first,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(painterResource(R.drawable.ic_remove), stringResource(R.string.weekly_goal_decrease))
        }
        // Zone vivante : le lecteur d'écran annonce la nouvelle valeur après chaque appui.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
        ) {
            Text(goal.toString(), style = MaterialTheme.typography.displaySmall)
            Text(
                pluralStringResource(R.plurals.weekly_goal_per_week, goal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FilledTonalIconButton(
            onClick = { onGoalChange(goal + 1) },
            enabled = goal < WEEKLY_GOAL_RANGE.last,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(painterResource(R.drawable.ic_add), stringResource(R.string.weekly_goal_increase))
        }
    }
}
