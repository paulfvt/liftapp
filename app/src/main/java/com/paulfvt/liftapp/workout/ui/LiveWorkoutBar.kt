package com.paulfvt.liftapp.workout.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.ui.rememberElapsedSeconds
import com.paulfvt.liftapp.workout.domain.formatDuration
import java.time.Instant

private const val PULSE_MILLIS = 800

// Bandeau rouge signal au-dessus de la barre du bas tant qu'une séance est en cours : un appui y revient.
@Composable
fun LiveWorkoutBar(name: String, startedAt: Instant, onClick: () -> Unit) {
    val elapsed = formatDuration(rememberElapsedSeconds(startedAt))
    val pulse = rememberInfiniteTransition(label = "liveBar")
    val dotAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
        label = "dot"
    )
    val onRed = MaterialTheme.colorScheme.onPrimaryContainer
    val description = stringResource(R.string.workout_live_bar, name, elapsed)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            // Un seul élément pour le lecteur d'écran, avec une phrase complète à la place des morceaux de texte.
            .clearAndSetSemantics {
                contentDescription = description
                onClick {
                    onClick()
                    true
                }
            }
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp)
    ) {
        Box(Modifier.size(8.dp).graphicsLayer { alpha = dotAlpha }.background(onRed, CircleShape))
        Text(
            stringResource(R.string.workout_live).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = onRed
        )
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            color = onRed,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(elapsed, style = MaterialTheme.typography.titleMedium, color = onRed)
        Icon(
            painterResource(R.drawable.ic_expand_more),
            contentDescription = null,
            tint = onRed,
            modifier = Modifier.rotate(180f)
        )
    }
}
