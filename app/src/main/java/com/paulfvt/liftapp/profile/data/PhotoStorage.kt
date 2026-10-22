package com.paulfvt.liftapp.profile.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.paulfvt.liftapp.profile.domain.MAX_PHOTO_SIDE
import com.paulfvt.liftapp.profile.domain.parseExifDate
import com.paulfvt.liftapp.profile.domain.sampleSizeFor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DIRECTORY = "photos"
private const val JPEG_QUALITY = 90
private const val QUARTER_TURN = 90f
private const val HALF_TURN = 180f
private const val THREE_QUARTER_TURN = 270f

// Fichiers des photos, dans le dossier privé files/photos/ : invisibles dans la galerie, exclus de la sauvegarde
// cloud d'Android (res/xml/data_extraction_rules.xml). Chaque photo est réduite à 2048 px et enregistrée droite,
// pour ne plus dépendre de l'orientation notée dans l'original.
@Singleton
class PhotoStorage @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val directory: File get() = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    fun file(fileName: String): File = File(directory, fileName)

    // Copie d'une image choisie dans la galerie (ou prise par l'appareil photo). Renvoie le nom du fichier créé.
    suspend fun copy(uri: Uri): String = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Image illisible : $uri")
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight) }
        val decoded = open(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Image illisible : $uri")
        val orientation = open(uri).use { ExifInterface(it).orientation() }
        save(decoded.scaledDown().rotated(orientation))
    }

    // Date de prise de vue notée dans l'image, si elle existe.
    suspend fun takenAt(uri: Uri, zone: ZoneId): Instant? = withContext(Dispatchers.IO) {
        try {
            open(uri).use { input ->
                val exif = ExifInterface(input)
                parseExifDate(
                    exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                        ?: exif.getAttribute(ExifInterface.TAG_DATETIME),
                    zone
                )
            }
        } catch (_: IOException) {
            null
        }
    }

    suspend fun delete(fileName: String) {
        withContext(Dispatchers.IO) { file(fileName).delete() }
    }

    private fun open(uri: Uri): InputStream =
        context.contentResolver.openInputStream(uri) ?: throw IOException("Image introuvable : $uri")

    private fun save(bitmap: Bitmap): String {
        val name = "${UUID.randomUUID()}.jpg"
        FileOutputStream(file(name)).use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        return name
    }
}

private fun ExifInterface.orientation(): Int =
    getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

private fun Bitmap.scaledDown(): Bitmap {
    val longest = maxOf(width, height)
    if (longest <= MAX_PHOTO_SIDE) return this
    val ratio = MAX_PHOTO_SIDE.toFloat() / longest
    return Bitmap.createScaledBitmap(this, (width * ratio).toInt(), (height * ratio).toInt(), true)
}

// Redresse la photo selon l'orientation notée par l'appareil qui l'a prise (les miroirs, rares, sont ignorés).
private fun Bitmap.rotated(orientation: Int): Bitmap {
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> QUARTER_TURN
        ExifInterface.ORIENTATION_ROTATE_180 -> HALF_TURN
        ExifInterface.ORIENTATION_ROTATE_270 -> THREE_QUARTER_TURN
        else -> return this
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees) }, true)
}
