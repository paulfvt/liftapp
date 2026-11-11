package com.paulfvt.liftapp.nutrition.data

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.paulfvt.liftapp.nutrition.domain.LabelReading
import com.paulfvt.liftapp.nutrition.domain.readNutritionLabel
import com.paulfvt.liftapp.profile.data.decodeUpright
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

// Lecture d'une étiquette : ML Kit reconnaît le texte sur le téléphone, sans internet, puis readNutritionLabel en tire
// les valeurs pour 100 g. Deux sources : les images de la caméra en direct (11 novembre 2026), jamais enregistrées,
// et une image choisie dans la galerie, seulement lue.
@Singleton
class LabelScanner @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun read(uri: Uri): LabelReading = withContext(Dispatchers.Default) {
        val bitmap = context.decodeUpright(uri, MAX_SIDE)
        val text = suspendCancellableCoroutine { continuation ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
        readNutritionLabel(text.toTextLines())
    }

    // Scan en direct : chaque image de la caméra est lue, puis rendue à onReading sur le fil principal. CameraX ne
    // garde que la dernière image : celles qui arrivent pendant une lecture sont sautées. Une image illisible est
    // simplement ignorée, la suivante arrive aussitôt.
    @OptIn(ExperimentalGetImage::class)
    fun analyzer(onReading: (LabelReading) -> Unit) = ImageAnalysis.Analyzer { frame ->
        val image = frame.image
        if (image == null) {
            frame.close()
            return@Analyzer
        }
        recognizer.process(InputImage.fromMediaImage(image, frame.imageInfo.rotationDegrees))
            .addOnSuccessListener { onReading(readNutritionLabel(it.toTextLines())) }
            .addOnCompleteListener { frame.close() }
    }

    private companion object {
        // Assez pour lire les petits caractères d'un tableau photographié avec tout le paquet (2 048 ne suffisait pas
        // toujours), sans charger une photo de 50 Mpx en mémoire.
        const val MAX_SIDE = 3072
    }
}
