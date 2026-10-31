package com.paulfvt.liftapp.progress.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.ui.shortDay
import com.paulfvt.liftapp.progress.domain.Badge
import java.time.Instant

private const val BADGES_PER_ROW = 3

@get:StringRes
val Badge.titleRes: Int
    get() = when (this) {
        Badge.WORKOUTS_1 -> R.string.badge_workouts_1
        Badge.WORKOUTS_10 -> R.string.badge_workouts_10
        Badge.WORKOUTS_50 -> R.string.badge_workouts_50
        Badge.WORKOUTS_100 -> R.string.badge_workouts_100
        Badge.STREAK_4 -> R.string.badge_streak_4
        Badge.STREAK_12 -> R.string.badge_streak_12
        Badge.STREAK_52 -> R.string.badge_streak_52
        Badge.CLUB_BENCH_100 -> R.string.badge_club_100
        Badge.CLUB_SQUAT_140 -> R.string.badge_club_140
        Badge.CLUB_DEADLIFT_180 -> R.string.badge_club_180
    }

// Ce qu'il faut faire pour débloquer le badge.
@get:StringRes
private val Badge.descriptionRes: Int
    get() = when (this) {
        Badge.WORKOUTS_1 -> R.string.badge_workouts_1_text
        Badge.WORKOUTS_10 -> R.string.badge_workouts_10_text
        Badge.WORKOUTS_50 -> R.string.badge_workouts_50_text
        Badge.WORKOUTS_100 -> R.string.badge_workouts_100_text
        Badge.STREAK_4 -> R.string.badge_streak_4_text
        Badge.STREAK_12 -> R.string.badge_streak_12_text
        Badge.STREAK_52 -> R.string.badge_streak_52_text
        Badge.CLUB_BENCH_100 -> R.string.badge_club_100_text
        Badge.CLUB_SQUAT_140 -> R.string.badge_club_140_text
        Badge.CLUB_DEADLIFT_180 -> R.string.badge_club_180_text
    }

// Indice court sous un badge pas encore débloqué.
@get:StringRes
private val Badge.hintRes: Int
    get() = when (this) {
        Badge.WORKOUTS_1, Badge.WORKOUTS_10, Badge.WORKOUTS_50, Badge.WORKOUTS_100 -> R.string.badge_hint_workouts
        Badge.STREAK_4, Badge.STREAK_12, Badge.STREAK_52 -> R.string.badge_hint_streak
        Badge.CLUB_BENCH_100 -> R.string.badge_hint_bench
        Badge.CLUB_SQUAT_140 -> R.string.badge_hint_squat
        Badge.CLUB_DEADLIFT_180 -> R.string.badge_hint_deadlift
    }

@get:DrawableRes
val Badge.iconRes: Int
    get() = when (this) {
        Badge.WORKOUTS_1, Badge.WORKOUTS_10, Badge.WORKOUTS_50, Badge.WORKOUTS_100 -> R.drawable.ic_fitness_center
        Badge.STREAK_4, Badge.STREAK_12, Badge.STREAK_52 -> R.drawable.ic_local_fire_department
        Badge.CLUB_BENCH_100, Badge.CLUB_SQUAT_140, Badge.CLUB_DEADLIFT_180 -> R.drawable.ic_emoji_events
    }

// Grille des badges, trois par ligne : débloqués en ambre avec leur date, les autres en gris avec un indice.
@Composable
fun BadgeGrid(unlocked: Map<Badge, Instant>, onBadgeClick: (Badge) -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Badge.entries.chunked(BADGES_PER_ROW).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { badge ->
                        BadgeTile(badge, unlocked[badge], onClick = { onBadgeClick(badge) }, Modifier.weight(1f))
                    }
                    repeat(BADGES_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun BadgeTile(badge: Badge, unlockedAt: Instant?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val title = stringResource(badge.titleRes)
    val detail = unlockedAt?.let { shortDay(it) } ?: stringResource(badge.hintRes)
    val description = if (unlockedAt != null) {
        stringResource(R.string.badge_unlocked_description, title, detail)
    } else {
        stringResource(R.string.badge_locked_description, title, stringResource(badge.descriptionRes))
    }
    val isUnlocked = unlockedAt != null
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick {
                    onClick()
                    true
                }
            }
            .padding(vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .background(
                    if (isUnlocked) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    CircleShape
                )
        ) {
            val tint = if (isUnlocked) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.outline
            }
            Icon(painterResource(badge.iconRes), contentDescription = null, tint = tint)
        }
        Text(title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        Text(
            detail,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Détail d'un badge : comment le débloquer, et quand il l'a été.
@Composable
fun BadgeDialog(badge: Badge, unlockedAt: Instant?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(badge.iconRes), contentDescription = null) },
        title = { Text(stringResource(badge.titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(badge.descriptionRes))
                Text(
                    unlockedAt?.let { stringResource(R.string.badge_unlocked_on, shortDay(it)) }
                        ?: stringResource(R.string.badge_not_yet),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { LiftTextButton(stringResource(R.string.action_ok), onClick = onDismiss) }
    )
}
