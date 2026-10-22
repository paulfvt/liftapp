package com.paulfvt.liftapp.profile.domain

import com.paulfvt.liftapp.core.model.PhotoPose
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

// Photo de progression : fileName est le nom du fichier dans le dossier privé photos/ de l'app.
data class ProgressPhoto(val id: Long, val fileName: String, val pose: PhotoPose, val takenAt: Instant)

// Plus grand côté d'une photo enregistrée : assez pour comparer le détail des muscles, sans garder les 12 Mo
// d'une photo de téléphone moderne.
const val MAX_PHOTO_SIDE = 2048

// Facteur de sous-échantillonnage au décodage (une puissance de 2, comme l'exige Android) : le plus grand qui
// garde le grand côté au moins égal à maxSide. La réduction exacte se fait ensuite.
fun sampleSizeFor(width: Int, height: Int, maxSide: Int = MAX_PHOTO_SIDE): Int {
    val longest = maxOf(width, height)
    var sample = 1
    while (longest / (sample * 2) >= maxSide) sample *= 2
    return sample
}

private val EXIF_DATE = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

// Date de prise de vue écrite dans la photo (« 2026:10:21 18:30:05 », heure locale de l'appareil qui l'a prise).
// null si elle manque ou est illisible : la date du jour est alors proposée.
fun parseExifDate(text: String?, zone: ZoneId): Instant? {
    if (text.isNullOrBlank()) return null
    return try {
        LocalDateTime.parse(text.trim(), EXIF_DATE).atZone(zone).toInstant()
    } catch (_: DateTimeParseException) {
        null
    }
}

// Photos d'une pose (toutes si pose est null), dans l'ordre reçu.
fun List<ProgressPhoto>.ofPose(pose: PhotoPose?): List<ProgressPhoto> =
    if (pose == null) this else filter { it.pose == pose }

// Silhouette fantôme : la photo la plus récente de la même pose, pour garder le même cadrage.
fun List<ProgressPhoto>.latestOfPose(pose: PhotoPose): ProgressPhoto? =
    filter { it.pose == pose }.maxByOrNull { it.takenAt }

// Moment enregistré pour une photo importée : l'heure de prise de vue notée dans la photo si on garde son jour,
// sinon la même règle que pour une mesure (maintenant pour aujourd'hui, midi pour un jour passé).
fun photoTakenAt(date: LocalDate, fromImage: Instant?, now: Instant, zone: ZoneId): Instant =
    fromImage?.takeIf { it.atZone(zone).toLocalDate() == date } ?: measuredAtFor(date, now, zone)

// Comparaison avant/après : deux photos de la même pose, la plus ancienne avant.
data class PhotoComparison(val before: ProgressPhoto, val after: ProgressPhoto)

// Photos de la pose, de la plus ancienne à la plus récente (ordre des bandeaux de choix).
fun List<ProgressPhoto>.chronologicalOfPose(pose: PhotoPose): List<ProgressPhoto> =
    filter { it.pose == pose }.sortedBy { it.takenAt }

// Poses qui ont assez de photos pour une comparaison.
fun List<ProgressPhoto>.comparablePoses(): List<PhotoPose> =
    PhotoPose.entries.filter { pose -> count { it.pose == pose } >= 2 }

// Paire montrée : celle choisie si elle est valable, sinon la première et la dernière photo de la pose (le plus
// grand écart). null s'il y a moins de deux photos de cette pose.
fun List<ProgressPhoto>.comparison(pose: PhotoPose, beforeId: Long?, afterId: Long?): PhotoComparison? {
    val ofPose = chronologicalOfPose(pose)
    if (ofPose.size < 2) return null
    val before = ofPose.firstOrNull { it.id == beforeId } ?: ofPose.first()
    val after =
        ofPose.firstOrNull { it.id == afterId && it.id != before.id } ?: ofPose.last().takeIf { it.id != before.id }
            ?: ofPose.first { it.id != before.id }
    return if (before.takenAt <= after.takenAt) PhotoComparison(before, after) else PhotoComparison(after, before)
}

// Écart en jours de calendrier entre les deux photos, dans le fuseau du téléphone.
fun PhotoComparison.daysBetween(zone: ZoneId): Long =
    ChronoUnit.DAYS.between(before.takenAt.atZone(zone).toLocalDate(), after.takenAt.atZone(zone).toLocalDate())
