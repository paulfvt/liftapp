package com.paulfvt.liftapp.workout.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.ui.LocalUnits
import com.paulfvt.liftapp.progress.ui.formatMetric
import com.paulfvt.liftapp.progress.ui.recordLabelRes
import kotlinx.coroutines.delay

private const val VISIBLE_MS = 3_500L

// Célébration discrète d'un record battu pendant la séance : un bandeau ambre (couleur réservée aux records) qui
// apparaît avec un léger rebond, une vibration de confirmation et une annonce du lecteur d'écran.
// Les records déjà présents à l'ouverture de l'écran ne sont pas recélébrés : seuls les nouveaux couples
// « série, mesure » déclenchent le bandeau. La liste des couples vus survit à une rotation.
@Composable
fun RecordCelebration(records: List<SessionRecord>, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    var seenKeys by rememberSaveable { mutableStateOf<List<String>?>(null) }
    // shown garde le dernier bandeau : il reste lisible pendant l'animation de disparition.
    var shown by remember { mutableStateOf<List<SessionRecord>>(emptyList()) }
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(records) {
        val known = seenKeys
        val fresh = if (known == null) emptyList() else records.filter { it.key !in known }
        seenKeys = (known.orEmpty() + records.map { it.key }).distinct()
        if (fresh.isNotEmpty()) {
            shown = fresh
            isVisible = true
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }
    LaunchedEffect(shown) {
        if (isVisible) {
            delay(VISIBLE_MS)
            isVisible = false
        }
    }
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        RecordBanner(shown)
    }
}

@Composable
private fun RecordBanner(records: List<SessionRecord>) {
    val first = records.firstOrNull() ?: return
    val resources = LocalResources.current
    val label = stringResource(first.record.metric.recordLabelRes(first.exercise.trackingType))
    val value =
        formatMetric(resources, first.record.metric, first.record.value, first.exercise, LocalUnits.current.weight)
    val tertiary = MaterialTheme.colorScheme.tertiary
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, tertiary),
        // Lueur ambre : ombre colorée (visible à partir d'Android 9, grise avant).
        modifier = Modifier
            .shadow(16.dp, MaterialTheme.shapes.large, ambientColor = tertiary, spotColor = tertiary)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(painterResource(R.drawable.ic_emoji_events), contentDescription = null, tint = tertiary)
            Column {
                Text(
                    stringResource(R.string.record_new).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = tertiary
                )
                Text(
                    stringResource(R.string.record_new_detail, first.exercise.name, label, value),
                    style = MaterialTheme.typography.bodyMedium
                )
                val others = records.size - 1
                if (others > 0) {
                    Text(
                        pluralStringResource(R.plurals.record_new_others, others, others),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
