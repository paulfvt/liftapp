package com.paulfvt.liftapp.profile.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.paulfvt.liftapp.profile.domain.MAX_PHOTO_SIDE
import com.paulfvt.liftapp.profile.domain.parseExifDate
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

// Dossier des photos dans files/, aussi utilisé par la sauvegarde et la restauration.
const val PHOTO_DIRECTORY = "photos"
private const val JPEG_QUALITY = 90

// Fichiers des photos, dans le dossier privé files/photos/ : invisibles dans la galerie, exclus de la sauvegarde
// cloud d'Android (res/xml/data_extraction_rules.xml). Chaque photo est réduite à 2048 px et enregistrée droite,
// pour ne plus dépendre de l'orientation notée dans l'original.
@Singleton
class PhotoStorage @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val directory: File get() = File(context.filesDir, PHOTO_DIRECTORY).apply { mkdirs() }

    fun file(fileName: String): File = File(directory, fileName)

    // Copie d'une image choisie dans la galerie (ou prise par l'appareil photo). Renvoie le nom du fichier créé.
    suspend fun copy(uri: Uri): String = withContext(Dispatchers.IO) {
        save(context.decodeUpright(uri, MAX_PHOTO_SIDE))
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
