package com.paulfvt.liftapp.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.model.PhotoPose
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.core.ui.shortDay
import com.paulfvt.liftapp.profile.domain.ProgressPhoto
import java.io.File

private const val PORTRAIT_RATIO = 3f / 4f
private const val SHADE_ALPHA = 0.7f

// Vignette d'une photo (format portrait), avec sa pose en bas. Lue « Photo de face du 21 oct. 2026 ».
@Composable
fun PhotoThumbnail(photo: ProgressPhoto, file: File, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = photoDescription(photo)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(PORTRAIT_RATIO)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Image
            }
    ) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            stringResource(photo.pose.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = SHADE_ALPHA))))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun photoDescription(photo: ProgressPhoto): String = stringResource(
    when (photo.pose) {
        PhotoPose.FRONT -> R.string.photo_description_front
        PhotoPose.SIDE -> R.string.photo_description_side
        PhotoPose.BACK -> R.string.photo_description_back
    },
    shortDay(photo.takenAt)
)
