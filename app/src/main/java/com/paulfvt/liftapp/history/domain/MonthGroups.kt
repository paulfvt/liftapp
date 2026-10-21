package com.paulfvt.liftapp.history.domain

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class MonthGroup<T>(val month: YearMonth, val items: List<T>)

// Regroupe une liste déjà triée par mois, dans le fuseau du téléphone : une séance commencée le 31 octobre à
// 23 h 30 à Paris est en octobre, même si elle est enregistrée le 31 à 22 h 30 UTC. L'ordre est conservé.
fun <T> groupByMonth(items: List<T>, zone: ZoneId, date: (T) -> Instant): List<MonthGroup<T>> =
    items.groupBy { YearMonth.from(date(it).atZone(zone)) }.map { (month, inMonth) -> MonthGroup(month, inMonth) }
