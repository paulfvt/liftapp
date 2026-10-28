package com.paulfvt.liftapp.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.WeeklyRing
import com.paulfvt.liftapp.progress.domain.WeeklyStreak
import com.paulfvt.liftapp.progress.ui.WeeklyGoalStepper

// Carte « Cette semaine » : anneau de l'objectif, streak en semaines et ce qu'il reste à faire.
@Composable
fun WeekCard(streak: WeeklyStreak, onEditGoal: () -> Unit, modifier: Modifier = Modifier) {
    val week = streak.currentWeek
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                WeeklyRing(done = week.sessions, goal = week.goal)
                StreakBlock(streak, Modifier.weight(1f))
            }
            val remaining = week.goal - week.sessions
            Text(
                if (remaining > 0) {
                    pluralStringResource(R.plurals.week_remaining, remaining, remaining)
                } else {
                    stringResource(R.string.week_goal_reached)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onEditGoal, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.weekly_goal_edit))
            }
        }
    }
}

// Streak en gros chiffres, lue d'un seul tenant (« Streak, 7 semaines, record 9 semaines »).
@Composable
private fun StreakBlock(streak: WeeklyStreak, modifier: Modifier = Modifier) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                painterResource(R.drawable.ic_local_fire_department),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                stringResource(R.string.streak_title).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(streak.weeks.toString(), style = MaterialTheme.typography.displayMedium)
        Text(
            pluralStringResource(R.plurals.streak_weeks_unit, streak.weeks).uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (streak.bestWeeks > streak.weeks) {
            Text(
                pluralStringResource(R.plurals.streak_best, streak.bestWeeks, streak.bestWeeks),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// Changement d'objectif : il vaut dès la semaine en cours. Le choix en cours survit à une rotation.
@Composable
fun WeeklyGoalDialog(currentGoal: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var goal by rememberSaveable { mutableIntStateOf(currentGoal) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weekly_goal_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.weekly_goal_dialog_text), style = MaterialTheme.typography.bodyMedium)
                WeeklyGoalStepper(goal, onGoalChange = { goal = it })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(goal) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
