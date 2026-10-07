package com.paulfvt.liftapp.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.paulfvt.liftapp.R

// Durée courte lisible : « 45 s », « 2 min », « 1 min 30 ».
@Composable
fun durationLabel(seconds: Int): String {
    val minutes = seconds / 60
    val remainder = seconds % 60
    return when {
        minutes == 0 -> stringResource(R.string.duration_seconds, remainder)
        remainder == 0 -> stringResource(R.string.duration_minutes, minutes)
        else -> stringResource(R.string.duration_minutes_seconds, minutes, remainder)
    }
}
