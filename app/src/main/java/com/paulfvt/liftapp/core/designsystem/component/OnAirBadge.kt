package com.paulfvt.liftapp.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private const val PULSE_MILLIS = 800

// Signature « ON AIR » : enseigne rouge signal allumée, avec un point qui pulse et une lueur néon.
// Réservée à la séance en cours (guide de style : la lueur ne sert qu'aux moments forts).
@Composable
fun OnAirBadge(text: String, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "onAir")
    val dotAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
        label = "dot"
    )
    val red = MaterialTheme.colorScheme.primaryContainer
    val onRed = MaterialTheme.colorScheme.onPrimaryContainer
    val shape = MaterialTheme.shapes.extraSmall
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .shadow(elevation = 10.dp, shape = shape, ambientColor = red, spotColor = red)
            .background(red, shape)
            .heightIn(min = 28.dp)
            .padding(horizontal = 12.dp)
            .clearAndSetSemantics { contentDescription = text }
    ) {
        Box(Modifier.size(8.dp).graphicsLayer { alpha = dotAlpha }.background(onRed, CircleShape))
        Text(
            text.uppercase(),
            color = onRed,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 14.sp, letterSpacing = 0.14.em)
        )
    }
}
