package com.paulfvt.liftapp.exercise.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.model.Exercise
import kotlinx.coroutines.delay

// Les images du catalogue sont embarquées dans l'APK (dossier assets) : elles s'affichent hors-ligne.
private fun Exercise.imageUri(index: Int): String? = catalogKey?.takeIf {
    index < imageCount
}?.let { "file:///android_asset/catalog/images/$it/$index.webp" }

// Vignette d'un exercice. zoomable : un appui l'agrandit (positions de départ et d'arrivée en alternance), à utiliser
// là où la vignette n'est pas déjà dans une ligne cliquable.
@Composable
fun ExerciseThumbnail(exercise: Exercise, modifier: Modifier = Modifier, zoomable: Boolean = false) {
    val shape = RoundedCornerShape(8.dp)
    val uri = exercise.imageUri(0)
    if (uri != null) {
        var isZoomed by rememberSaveable { mutableStateOf(false) }
        val zoomDescription = stringResource(R.string.exercise_image_open, exercise.name)
        AsyncImage(
            model = uri,
            // Image décorative : le nom de l'exercice est lu juste à côté par le lecteur d'écran.
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(56.dp)
                .clip(shape)
                .then(
                    if (zoomable) {
                        Modifier
                            .clickable(role = Role.Button) { isZoomed = true }
                            .semantics { contentDescription = zoomDescription }
                    } else {
                        Modifier
                    }
                )
        )
        if (isZoomed) ExerciseImageDialog(exercise, onDismiss = { isZoomed = false })
    } else {
        Box(
            modifier = modifier
                .size(56.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_fitness_center),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Image agrandie : le mouvement en grand, le nom de l'exercice dessous. Un appui n'importe où la referme.
@Composable
private fun ExerciseImageDialog(exercise: Exercise, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable(
                    onClickLabel = stringResource(R.string.action_close),
                    role = Role.Button,
                    onClick = onDismiss
                )
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                ExerciseImageAnimation(exercise)
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                LiftTextButton(
                    text = stringResource(R.string.action_close),
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

// Alterne entre la position de départ et la position d'arrivée pour montrer le mouvement.
@Composable
fun ExerciseImageAnimation(exercise: Exercise, modifier: Modifier = Modifier) {
    if (exercise.imageCount == 0 || exercise.catalogKey == null) return
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(exercise.imageCount) {
        while (exercise.imageCount > 1) {
            delay(IMAGE_SWITCH_DELAY_MS)
            index = (index + 1) % exercise.imageCount
        }
    }
    val description = stringResource(R.string.exercise_images_description, exercise.name)
    Crossfade(
        targetState = index,
        animationSpec = tween(durationMillis = 400),
        label = "exerciseImage",
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 2f)
            .clip(RoundedCornerShape(12.dp))
            .semantics { contentDescription = description }
    ) { imageIndex ->
        AsyncImage(
            model = exercise.imageUri(imageIndex),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 2f)
        )
    }
}

private const val IMAGE_SWITCH_DELAY_MS = 1_200L
