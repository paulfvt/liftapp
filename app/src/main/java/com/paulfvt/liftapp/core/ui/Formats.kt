package com.paulfvt.liftapp.core.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.paulfvt.liftapp.R
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

// Mises en forme partagées par la séance, l'historique et la progression. Les dates sont affichées dans le fuseau
// du téléphone, en français.

private val DAY_AND_TIME = DateTimeFormatter.ofPattern("EEE d MMM yyyy · HH:mm", Locale.FRENCH)
private val SHORT_DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)
private val MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)
private val DAY_TITLE = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
private const val DAYS_IN_WEEK = 7L

// « 58 min » ou « 1 h 05 ». La version avec Resources sert au widget, qui n'a pas les ressources de Compose.
fun durationText(resources: Resources, duration: Duration): String {
    val hours = duration.toHours().toInt()
    val minutes = (duration.toMinutes() % 60).toInt()
    return if (hours > 0) {
        resources.getString(R.string.duration_hours_minutes, hours, minutes)
    } else {
        resources.getString(R.string.duration_minutes, minutes)
    }
}

@Composable
fun durationText(duration: Duration): String = durationText(LocalResources.current, duration)

// +0,06 → « +6 », −0,04 → « −4 » (signe moins typographique), pour « +6 % vs S43 ».
fun signedPercent(change: Double): String {
    val percent = (change * PERCENT).roundToInt()
    return when {
        percent > 0 -> "+$percent"
        percent < 0 -> "\u2212${-percent}"
        else -> "0"
    }
}

private const val PERCENT = 100

// Nombre entier avec séparateur de milliers français : « 9 840 ».
fun wholeNumber(value: Double): String = NumberFormat.getIntegerInstance(Locale.FRENCH).format(value.roundToLong())

// « mar. 21 oct. 2026 · 18:04 ».
fun dayAndTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    DAY_AND_TIME.format(instant.atZone(zone))

// « Lundi » pour une date de moins d'une semaine, « 12 oct. » au-delà (puce « Lundi : 80 × 8 » de la séance).
fun recentDay(instant: Instant, zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): String {
    val date = instant.atZone(zone).toLocalDate()
    return if (ChronoUnit.DAYS.between(date, today) in 0 until DAYS_IN_WEEK) {
        date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH).replaceFirstChar { it.uppercase(Locale.FRENCH) }
    } else {
        DAY_MONTH.format(date)
    }
}

// « dimanche 1 novembre », en tête de l'accueil.
fun dayTitle(date: LocalDate): String = DAY_TITLE.format(date)

// « 21 oct. 2026 ».
fun shortDay(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String = SHORT_DAY.format(instant.atZone(zone))

// « 3 mai 1998 », pour une date sans heure (date de naissance).
fun shortDate(date: LocalDate): String = SHORT_DAY.format(date)

// « Octobre 2026 ».
fun monthTitle(month: YearMonth): String = MONTH.format(month).replaceFirstChar { it.uppercase(Locale.FRENCH) }
