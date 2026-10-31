package com.paulfvt.liftapp.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.paulfvt.liftapp.R
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

// Mises en forme partagées par la séance, l'historique et la progression. Les dates sont affichées dans le fuseau
// du téléphone, en français.

private val DAY_AND_TIME = DateTimeFormatter.ofPattern("EEE d MMM yyyy · HH:mm", Locale.FRENCH)
private val SHORT_DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)
private val MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)

// « 58 min » ou « 1 h 05 ».
@Composable
fun durationText(duration: Duration): String {
    val hours = duration.toHours().toInt()
    val minutes = (duration.toMinutes() % 60).toInt()
    return if (hours > 0) {
        stringResource(R.string.duration_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }
}

// Nombre entier avec séparateur de milliers français : « 9 840 ».
fun wholeNumber(value: Double): String = NumberFormat.getIntegerInstance(Locale.FRENCH).format(value.roundToLong())

// « mar. 21 oct. 2026 · 18:04 ».
fun dayAndTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    DAY_AND_TIME.format(instant.atZone(zone))

// « 21 oct. 2026 ».
fun shortDay(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String = SHORT_DAY.format(instant.atZone(zone))

// « 3 mai 1998 », pour une date sans heure (date de naissance).
fun shortDate(date: LocalDate): String = SHORT_DAY.format(date)

// « Octobre 2026 ».
fun monthTitle(month: YearMonth): String = MONTH.format(month).replaceFirstChar { it.uppercase(Locale.FRENCH) }
