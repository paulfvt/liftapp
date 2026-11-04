package com.paulfvt.liftapp.nutrition.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.paulfvt.liftapp.nutrition.domain.LabelReading
import com.paulfvt.liftapp.nutrition.domain.TextLine
import com.paulfvt.liftapp.nutrition.domain.readNutritionLabel
import com.paulfvt.liftapp.profile.data.decodeUpright
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

// Lecture d'une étiquette (décision du 5 novembre 2026) : ML Kit reconnaît le texte sur le téléphone, sans internet,
// puis readNutritionLabel en tire les valeurs pour 100 g. La photo prise par l'appareil photo n'est qu'un fichier
// temporaire, effacé après lecture ; une image de la galerie est seulement lue.
@Singleton
class LabelScanner @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    private val photoFile: File get() = File(File(context.cacheDir, LABEL_DIRECTORY).apply { mkdirs() }, PHOTO_NAME)

    // Adresse que l'appareil photo d'Android remplit (FileProvider, voir res/xml/file_paths.xml).
    fun photoUri(): Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)

    suspend fun read(uri: Uri): LabelReading = withContext(Dispatchers.Default) {
        val bitmap = context.decodeUpright(uri, MAX_SIDE)
        val text = suspendCancellableCoroutine { continuation ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
        val lines = text.textBlocks.flatMap { it.lines }.mapNotNull { line ->
            line.boundingBox?.let { box -> TextLine(line.text, box.left, box.top, box.right, box.bottom) }
        }
        readNutritionLabel(lines)
    }

    // La photo de l'appareil photo ne sert qu'à la lecture : elle est effacée aussitôt après.
    fun deletePhoto() {
        photoFile.delete()
    }

    private companion object {
        const val LABEL_DIRECTORY = "label"
        const val PHOTO_NAME = "label.jpg"

        // Assez pour lire les petits caractères d'un tableau, sans charger une photo de 50 Mpx en mémoire.
        const val MAX_SIDE = 2048
    }
}
