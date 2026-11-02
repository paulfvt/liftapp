package com.paulfvt.liftapp.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftButtonSize
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.LiftTonalButton
import com.paulfvt.liftapp.core.ui.durationLabel
import com.paulfvt.liftapp.workout.domain.REST_ADD_SECONDS
import com.paulfvt.liftapp.workout.domain.REST_REMOVE_SECONDS
import com.paulfvt.liftapp.workout.domain.RestPeriod
import com.paulfvt.liftapp.workout.domain.formatDuration
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

private const val MILLIS_PER_SECOND = 1_000L

// Minuteur de repos du guide de style : chiffres ambre avec lueur néon, boutons −15 s, +30 s et Passer. Rien n'est
// affiché une fois le repos terminé. Pendant la séance, il tient sur une ligne au-dessus du bouton de fin (maquette ON
// AIR) pour laisser voir les séries ; large : version du mode salle, chiffres géants et gros boutons empilés.
@Composable
fun RestTimer(
    rest: RestPeriod,
    onRemove: () -> Unit,
    onAdd: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    footer: @Composable () -> Unit = {}
) {
    val remaining = rememberRemainingSeconds(rest)
    if (remaining == 0) return
    val amber = MaterialTheme.colorScheme.tertiary
    val shape = MaterialTheme.shapes.large
    val actions = RestActions(onRemove, onAdd, onSkip)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = shape, ambientColor = amber, spotColor = amber)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, shape)
            .border(1.dp, amber, shape)
            .padding(
                if (large) {
                    PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                } else {
                    PaddingValues(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                }
            )
    ) {
        if (large) LargeRestTimer(remaining, actions) else CompactRestTimer(remaining, actions)
        footer()
    }
}

private class RestActions(val onRemove: () -> Unit, val onAdd: () -> Unit, val onSkip: () -> Unit)

// « REPOS 1:47 » à gauche, −15 S et +30 S, puis l'icône Passer : une seule ligne (.dock .timer).
@Composable
private fun CompactRestTimer(remaining: Int, actions: RestActions) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            RestTitle()
            RestTime(remaining, fontSize = 44.sp)
        }
        RestButton(
            text = stringResource(R.string.rest_remove, REST_REMOVE_SECONDS),
            description = pluralStringResource(
                R.plurals.rest_remove_description,
                REST_REMOVE_SECONDS,
                REST_REMOVE_SECONDS
            ),
            size = LiftButtonSize.Regular,
            onClick = actions.onRemove
        )
        RestButton(
            text = stringResource(R.string.rest_add, REST_ADD_SECONDS),
            description = pluralStringResource(R.plurals.rest_add_description, REST_ADD_SECONDS, REST_ADD_SECONDS),
            size = LiftButtonSize.Regular,
            onClick = actions.onAdd
        )
        IconButton(onClick = actions.onSkip) {
            Icon(
                painterResource(R.drawable.ic_skip_next),
                contentDescription = stringResource(R.string.rest_skip_description),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

// Mode salle : titre, chiffres géants, puis −15 S, +30 S et PASSER en gros boutons. PASSER garde sa largeur, les deux
// autres se partagent le reste : le mot tient en entier même sur un écran étroit.
@Composable
private fun LargeRestTimer(remaining: Int, actions: RestActions) {
    RestTitle()
    RestTime(remaining, fontSize = 112.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        RestButton(
            text = stringResource(R.string.rest_remove, REST_REMOVE_SECONDS),
            description = pluralStringResource(
                R.plurals.rest_remove_description,
                REST_REMOVE_SECONDS,
                REST_REMOVE_SECONDS
            ),
            size = LiftButtonSize.Gym,
            onClick = actions.onRemove,
            modifier = Modifier.weight(1f)
        )
        RestButton(
            text = stringResource(R.string.rest_add, REST_ADD_SECONDS),
            description = pluralStringResource(R.plurals.rest_add_description, REST_ADD_SECONDS, REST_ADD_SECONDS),
            size = LiftButtonSize.Gym,
            onClick = actions.onAdd,
            modifier = Modifier.weight(1f)
        )
        LiftTextButton(
            text = stringResource(R.string.rest_skip),
            onClick = actions.onSkip,
            size = LiftButtonSize.Gym,
            description = stringResource(R.string.rest_skip_description)
        )
    }
}

@Composable
private fun RestTitle() {
    Text(
        stringResource(R.string.rest_title).uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.14.em),
        color = MaterialTheme.colorScheme.tertiary
    )
}

@Composable
private fun RestTime(remaining: Int, fontSize: TextUnit) {
    val description = stringResource(R.string.rest_remaining_description, durationLabel(remaining))
    Text(
        formatDuration(remaining),
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = fontSize,
            lineHeight = fontSize,
            letterSpacing = 0.02.em
        ),
        color = MaterialTheme.colorScheme.tertiary,
        // Le lecteur d'écran lit une phrase (« Repos, il reste 1 min 47 ») au lieu de « 1:47 ».
        modifier = Modifier.clearAndSetSemantics { contentDescription = description }
    )
}

@Composable
private fun RestButton(
    text: String,
    description: String,
    size: LiftButtonSize,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiftTonalButton(
        text = text,
        onClick = onClick,
        size = size,
        description = description,
        // Marges serrées : le minuteur compact tient sur une ligne même sur un petit écran.
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = modifier
    )
}

// Secondes restantes, recalculées à partir de l'heure de fin à chaque changement de seconde affichée.
@Composable
fun rememberRemainingSeconds(rest: RestPeriod): Int {
    val remaining by produceState(initialValue = rest.remainingSeconds(Instant.now()), rest) {
        while (true) {
            val now = Instant.now()
            value = rest.remainingSeconds(now)
            if (value == 0) break
            // Attente jusqu'au prochain passage de seconde (affichage arrondi au-dessus).
            val untilNextSecond = Duration.between(now, rest.endsAt).toMillis() % MILLIS_PER_SECOND
            delay(if (untilNextSecond == 0L) MILLIS_PER_SECOND else untilNextSecond)
        }
    }
    return remaining
}
