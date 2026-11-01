package com.paulfvt.liftapp.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.WeeklyRing
import com.paulfvt.liftapp.progress.domain.WeeklyStreak
import com.paulfvt.liftapp.progress.ui.WeeklyGoalStepper
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val DAYS_IN_WEEK = 7L

// Semaine en tête de l'accueil, posée sur le fond comme dans la maquette : anneau de l'objectif, streak en semaines,
// jours entraînés, puis ce qu'il reste à faire et le changement d'objectif.
@Composable
fun WeekHero(
    streak: WeeklyStreak,
    today: LocalDate,
    trainedDays: Set<LocalDate>,
    onEditGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val week = streak.currentWeek
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            WeeklyRing(done = week.sessions, goal = week.goal)
            Column(modifier = Modifier.weight(1f)) {
                StreakBlock(streak)
                WeekDays(week.monday, today, trainedDays, modifier = Modifier.padding(top = 12.dp))
            }
        }
        val remaining = week.goal - week.sessions
        Text(
            if (remaining > 0) {
                pluralStringResource(R.plurals.week_remaining, remaining, remaining)
            } else {
                stringResource(R.string.week_goal_reached)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
        LiftTextButton(text = stringResource(R.string.weekly_goal_edit), onClick = onEditGoal)
    }
}

// Streak en très gros chiffres, lue d'un seul tenant (« Streak, 7 semaines, record 9 semaines »).
@Composable
private fun StreakBlock(streak: WeeklyStreak, modifier: Modifier = Modifier) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                painterResource(R.drawable.ic_local_fire_department),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                stringResource(R.string.streak_title).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.12.em),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            streak.weeks.toString(),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 76.sp, lineHeight = 70.sp),
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            pluralStringResource(R.plurals.streak_weeks_unit, streak.weeks).uppercase(),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp, letterSpacing = 0.06.em),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (streak.bestWeeks > streak.weeks) {
            Text(
                pluralStringResource(R.plurals.streak_best, streak.bestWeeks, streak.bestWeeks),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Rangée L M M J V S D : jour avec une séance en rouge signal, aujourd'hui entouré. Lue d'un bloc (« Séances cette
// semaine : lundi, mercredi »).
@Composable
private fun WeekDays(monday: LocalDate, today: LocalDate, trainedDays: Set<LocalDate>, modifier: Modifier = Modifier) {
    val days = (0 until DAYS_IN_WEEK).map { monday.plusDays(it) }
    val trainedNames = days.filter { it in trainedDays }
        .map { it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH) }
    val description = if (trainedNames.isEmpty()) {
        stringResource(R.string.home_no_trained_day)
    } else {
        stringResource(R.string.home_trained_days, trainedNames.joinToString(", "))
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.clearAndSetSemantics { contentDescription = description }
    ) {
        days.forEach { day -> DayChip(day, isTrained = day in trainedDays, isToday = day == today) }
    }
}

@Composable
private fun DayChip(day: LocalDate, isTrained: Boolean, isToday: Boolean) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraSmall
    val background = if (isTrained) colors.primaryContainer else colors.surfaceContainerHigh
    val content = when {
        isTrained -> colors.onPrimaryContainer
        isToday -> colors.primary
        else -> colors.onSurfaceVariant
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(24.dp)
            .background(background, shape)
            .border(1.dp, if (isToday) colors.primary else Color.Transparent, shape)
    ) {
        Text(
            day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.FRENCH),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 12.sp, lineHeight = 12.sp),
            color = content
        )
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
        confirmButton = { LiftTextButton(stringResource(R.string.action_save), onClick = { onConfirm(goal) }) },
        dismissButton = { LiftTextButton(stringResource(R.string.action_cancel), onClick = onDismiss) }
    )
}
