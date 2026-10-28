package com.paulfvt.liftapp.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R

private const val STROKE_RATIO = 12f / 128f
private const val FULL_TURN = 360f
private const val START_ANGLE = -90f
private const val FILL_DURATION_MS = 600

// Anneau hebdomadaire du guide de style : piste grise, trait rouge signal qui se remplit à chaque séance.
// Lu d'un seul tenant par le lecteur d'écran (« 2 séances sur 3 cette semaine »).
@Composable
fun WeeklyRing(done: Int, goal: Int, modifier: Modifier = Modifier, size: Dp = 128.dp) {
    val fraction by animateFloatAsState(
        targetValue = if (goal > 0) (done.toFloat() / goal).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(FILL_DURATION_MS),
        label = "weeklyRing"
    )
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val bar = MaterialTheme.colorScheme.primaryContainer
    val description = pluralStringResource(R.plurals.weekly_ring_description, done, done, goal)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).clearAndSetSemantics { contentDescription = description }
    ) {
        Canvas(Modifier.size(size)) { drawRing(fraction, track, bar) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$done/$goal", style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(if (done >= goal) R.string.weekly_ring_reached else R.string.weekly_ring_sessions)
                    .uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// La lueur est le même arc repassé plus large et transparent : le flou n'est pas disponible avant Android 9.
private fun DrawScope.drawRing(fraction: Float, track: Color, bar: Color) {
    val stroke = size.minDimension * STROKE_RATIO
    // Marge pour que la lueur, plus large que le trait, ne soit pas coupée au bord.
    val inset = stroke * 1.2f
    val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
    val topLeft = Offset(inset, inset)
    drawArc(track, 0f, FULL_TURN, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
    if (fraction <= 0f) return
    val sweep = FULL_TURN * fraction
    listOf(2.2f to 0.12f, 1.6f to 0.22f, 1f to 1f).forEach { (width, alpha) ->
        drawArc(
            color = bar.copy(alpha = alpha),
            startAngle = START_ANGLE,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke * width, cap = StrokeCap.Round)
        )
    }
}
