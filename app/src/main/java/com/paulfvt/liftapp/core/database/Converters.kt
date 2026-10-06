package com.paulfvt.liftapp.core.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

// SQLite ne connaît que les nombres, textes et blobs : on convertit les dates en nombres.
// Les énumérations n'ont pas besoin de convertisseur, Room les stocke par leur nom.
class Converters {
    // Instant (horodatage UTC) en millisecondes depuis le 1er janvier 1970.
    @TypeConverter
    fun instantToEpochMilli(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    // LocalDate (jour sans heure ni fuseau) en nombre de jours depuis le 1er janvier 1970.
    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)
}
