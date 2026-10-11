package com.paulfvt.liftapp.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

private const val TICK_MILLIS = 1_000L

// Secondes écoulées depuis startedAt, mises à jour chaque seconde tant que l'écran est affiché.
@Composable
fun rememberElapsedSeconds(startedAt: Instant): Int {
    val elapsed by produceState(initialValue = secondsSince(startedAt), startedAt) {
        while (true) {
            value = secondsSince(startedAt)
            delay(TICK_MILLIS)
        }
    }
    return elapsed
}

private fun secondsSince(startedAt: Instant): Int =
    Duration.between(startedAt, Instant.now()).seconds.coerceAtLeast(0).toInt()
