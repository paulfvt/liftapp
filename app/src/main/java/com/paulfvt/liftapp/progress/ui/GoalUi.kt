package com.paulfvt.liftapp.progress.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.MenuEntry
import com.paulfvt.liftapp.core.designsystem.component.OverflowMenu
import com.paulfvt.liftapp.core.ui.kgText
import com.paulfvt.liftapp.core.ui.shortDay
import com.paulfvt.liftapp.progress.domain.GoalDraft
import com.paulfvt.liftapp.progress.domain.GoalError
import com.paulfvt.liftapp.progress.domain.GoalStatus
import com.paulfvt.liftapp.progress.domain.GoalTier
import com.paulfvt.liftapp.progress.domain.GoalTiers
import com.paulfvt.liftapp.progress.domain.problem
import com.paulfvt.liftapp.progress.domain.suggestedTiers
import com.paulfvt.liftapp.workout.domain.formatDecimal
import com.paulfvt.liftapp.workout.domain.parseDecimal

// Couleurs des médailles, identiques dans les deux thèmes. Elles ne portent jamais seules l'information : le nom du
// palier est toujours écrit à côté.
val GoalTier.medalColor: Color
    get() = when (this) {
        GoalTier.BRONZE -> Color(0xFFCD7F32)
        GoalTier.SILVER -> Color(0xFFB4B9C0)
        GoalTier.GOLD -> Color(0xFFE6B422)
    }

@get:StringRes
val GoalTier.labelRes: Int
    get() = when (this) {
        GoalTier.BRONZE -> R.string.goal_tier_bronze
        GoalTier.SILVER -> R.string.goal_tier_silver
        GoalTier.GOLD -> R.string.goal_tier_gold
    }

// Carte d'un objectif : cible, barre de progression avec les trois paliers, 1RM actuel et paliers datés.
// title : le nom de l'exercice dans l'onglet Progrès, rien sur l'écran de l'exercice.
@Composable
fun GoalCard(
    status: GoalStatus,
    modifier: Modifier = Modifier,
    title: String? = null,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val goal = status.goal
    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(if (goal.isCompleted) R.drawable.ic_military_tech else R.drawable.ic_flag),
                    contentDescription = null,
                    tint = if (goal.isCompleted) GoalTier.GOLD.medalColor else MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                    Text(
                        stringResource(R.string.goal_target, kgText(goal.goldKg)),
                        style = if (title ==
                            null
                        ) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyMedium
                        }
                    )
                }
                onDelete?.let {
                    OverflowMenu(
                        listOf(
                            MenuEntry(R.string.goal_delete, R.drawable.ic_delete, isDestructive = true, onClick = it)
                        )
                    )
                }
            }
            GoalBar(status)
            Text(
                stringResource(R.string.goal_current, kgText(status.bestKg)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalTier.entries.forEach { tier -> TierLabel(status, tier, Modifier.weight(1f)) }
            }
        }
    }
    if (onClick != null) {
        ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) { content() }
    } else {
        ElevatedCard(modifier = modifier.fillMaxWidth()) { content() }
    }
}

// Barre du 1RM à la création jusqu'à l'or, un repère rond par palier (plein une fois atteint).
@Composable
private fun GoalBar(status: GoalStatus) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val fill = MaterialTheme.colorScheme.tertiary
    val description = stringResource(R.string.goal_progress_description, (status.progress * PERCENT).toInt())
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(20.dp)
            .clearAndSetSemantics { contentDescription = description }
    ) {
        val barHeight = 8.dp.toPx()
        val radius = 7.dp.toPx()
        val top = (size.height - barHeight) / 2
        val usable = size.width - 2 * radius
        drawRoundRect(track, Offset(radius, top), Size(usable, barHeight), CornerRadius(barHeight / 2))
        drawRoundRect(fill, Offset(radius, top), Size(usable * status.progress, barHeight), CornerRadius(barHeight / 2))
        GoalTier.entries.forEach { tier ->
            val center = Offset(radius + usable * status.fractionOf(status.goal.valueOf(tier)), size.height / 2)
            val reached = tier in status.goal.reachedAt
            drawCircle(if (reached) tier.medalColor else track, radius, center)
            drawCircle(tier.medalColor, radius, center, style = Stroke(2.dp.toPx()))
        }
    }
}

// « Bronze · 95 kg · le 12 oct. 2026 », lu d'un seul tenant.
@Composable
private fun TierLabel(status: GoalStatus, tier: GoalTier, modifier: Modifier = Modifier) {
    val reachedAt = status.goal.reachedAt[tier]
    val name = stringResource(tier.labelRes)
    val value = kgText(status.goal.valueOf(tier))
    val date = reachedAt?.let { shortDay(it) }
    val description = if (date != null) {
        stringResource(R.string.goal_tier_reached_description, name, value, date)
    } else {
        stringResource(R.string.goal_tier_pending_description, name, value)
    }
    Column(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                painterResource(R.drawable.ic_military_tech),
                contentDescription = null,
                tint = if (reachedAt != null) tier.medalColor else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
            Text(name, style = MaterialTheme.typography.labelMedium)
        }
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(
            date ?: stringResource(R.string.goal_tier_pending),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private const val PERCENT = 100

// Création d'un objectif : l'utilisateur saisit la cible (l'or) ; bronze et argent sont proposés à 1/3 et 2/3 du
// chemin depuis son 1RM estimé actuel, et suivent la cible tant qu'il ne les a pas modifiés lui-même.
@Composable
fun GoalDialog(currentKg: Double, onCreate: (GoalTiers) -> Unit, onDismiss: () -> Unit) {
    var gold by rememberSaveable { mutableStateOf("") }
    var bronze by rememberSaveable { mutableStateOf("") }
    var silver by rememberSaveable { mutableStateOf("") }
    var tiersEdited by rememberSaveable { mutableStateOf(false) }
    val draft = GoalDraft(bronze, silver, gold)
    val problem = draft.problem(currentKg)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goal_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.goal_dialog_text, kgText(currentKg)),
                    style = MaterialTheme.typography.bodyMedium
                )
                KgField(R.string.goal_tier_gold, gold) { text ->
                    gold = text
                    val goldKg = parseDecimal(text)
                    if (!tiersEdited && goldKg != null && goldKg > currentKg) {
                        val (proposedBronze, proposedSilver) = suggestedTiers(currentKg, goldKg)
                        bronze = formatDecimal(proposedBronze)
                        silver = formatDecimal(proposedSilver)
                    }
                }
                KgField(R.string.goal_tier_silver, silver) {
                    silver = it
                    tiersEdited = true
                }
                KgField(R.string.goal_tier_bronze, bronze) {
                    bronze = it
                    tiersEdited = true
                }
                if (gold.isNotBlank() && problem != null) {
                    Text(
                        stringResource(problem.messageRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = problem == null, onClick = { draft.parsed()?.let(onCreate) }) {
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun KgField(@StringRes labelRes: Int, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        suffix = { Text(stringResource(R.string.unit_kg)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@StringRes
private fun GoalError.messageRes(): Int = when (this) {
    GoalError.INVALID_NUMBER -> R.string.goal_error_invalid
    GoalError.NOT_ABOVE_CURRENT -> R.string.goal_error_not_above_current
    GoalError.NOT_INCREASING -> R.string.goal_error_not_increasing
}
