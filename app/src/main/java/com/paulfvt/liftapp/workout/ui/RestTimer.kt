package com.paulfvt.liftapp.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
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

// Minuteur de repos du guide de style : chiffres géants ambre avec lueur néon, boutons −15 s, +30 s et Passer.
// Rien n'est affiché une fois le repos terminé. large : version du mode salle, chiffres et boutons agrandis.
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
    val description = stringResource(R.string.rest_remaining_description, durationLabel(remaining))
    val buttonHeight = if (large) 64.dp else 48.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = shape, ambientColor = amber, spotColor = amber)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, shape)
            .border(1.dp, amber, shape)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            stringResource(R.string.rest_title).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.14.em),
            color = amber
        )
        Text(
            formatDuration(remaining),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = if (large) 112.sp else 64.sp,
                lineHeight = if (large) 112.sp else 64.sp,
                letterSpacing = 0.02.em
            ),
            color = amber,
            // Le lecteur d'écran lit une phrase (« Repos, il reste 1 min 47 ») au lieu de « 1:47 ».
            modifier = Modifier.clearAndSetSemantics { contentDescription = description }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RestButton(
                text = stringResource(R.string.rest_remove, REST_REMOVE_SECONDS),
                description = pluralStringResource(
                    R.plurals.rest_remove_description,
                    REST_REMOVE_SECONDS,
                    REST_REMOVE_SECONDS
                ),
                height = buttonHeight,
                onClick = onRemove,
                modifier = Modifier.weight(1f)
            )
            RestButton(
                text = stringResource(R.string.rest_add, REST_ADD_SECONDS),
                description = pluralStringResource(R.plurals.rest_add_description, REST_ADD_SECONDS, REST_ADD_SECONDS),
                height = buttonHeight,
                onClick = onAdd,
                modifier = Modifier.weight(1f)
            )
            val skipDescription = stringResource(R.string.rest_skip_description)
            LiftTextButton(
                text = stringResource(R.string.rest_skip),
                onClick = onSkip,
                size = if (large) LiftButtonSize.Gym else LiftButtonSize.Regular,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = buttonHeight)
                    .semantics { contentDescription = skipDescription }
            )
        }
        footer()
    }
}

@Composable
private fun RestButton(text: String, description: String, height: Dp, onClick: () -> Unit, modifier: Modifier) {
    LiftTonalButton(
        text = text,
        onClick = onClick,
        size = if (height > 48.dp) LiftButtonSize.Gym else LiftButtonSize.Regular,
        modifier = modifier.heightIn(min = height).semantics { contentDescription = description }
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
