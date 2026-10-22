package com.paulfvt.liftapp.profile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.EmptyState
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.designsystem.component.SectionTitle
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.core.ui.shortDay
import com.paulfvt.liftapp.profile.domain.PhotoComparison
import com.paulfvt.liftapp.profile.domain.ProgressPhoto
import java.io.File
import kotlin.math.roundToInt

private const val PORTRAIT_RATIO = 3f / 4f
private const val HALF = 0.5f
private const val LABEL_ALPHA = 0.6f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCompareScreen(onBack: () -> Unit, viewModel: PhotoCompareViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                title = { ScreenTitle(stringResource(R.string.compare_title)) }
            )
        }
    ) { padding ->
        if (uiState.isLoading) return@Scaffold
        val comparison = uiState.comparison
        if (comparison == null) {
            EmptyState(
                iconRes = R.drawable.ic_compare,
                title = stringResource(R.string.compare_title),
                text = stringResource(R.string.compare_need_two),
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            if (uiState.poses.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    uiState.poses.forEach { pose ->
                        FilterChip(
                            selected = pose == uiState.pose,
                            onClick = { viewModel.onPoseChange(pose) },
                            label = { Text(stringResource(pose.labelRes)) }
                        )
                    }
                }
            }
            BeforeAfter(comparison, viewModel::file, modifier = Modifier.padding(horizontal = 16.dp))
            Text(
                pluralStringResource(R.plurals.compare_gap, uiState.daysBetween.toInt(), uiState.daysBetween.toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            PhotoStrip(
                title = stringResource(R.string.compare_before),
                photos = uiState.photos,
                selected = comparison.before,
                fileOf = viewModel::file,
                onSelect = viewModel::onBeforeChange
            )
            PhotoStrip(
                title = stringResource(R.string.compare_after),
                photos = uiState.photos,
                selected = comparison.after,
                fileOf = viewModel::file,
                onSelect = viewModel::onAfterChange
            )
        }
    }
}

// Les deux photos l'une sur l'autre : « après » n'est visible qu'à droite du curseur. On fait glisser le doigt sur
// la photo, ou on règle la barre en dessous (celle-ci est aussi utilisable au lecteur d'écran).
@Composable
private fun BeforeAfter(comparison: PhotoComparison, fileOf: (ProgressPhoto) -> File, modifier: Modifier = Modifier) {
    var split by rememberSaveable { mutableFloatStateOf(HALF) }
    val beforeDay = shortDay(comparison.before.takenAt)
    val afterDay = shortDay(comparison.after.takenAt)
    val description = stringResource(R.string.compare_description, beforeDay, afterDay)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PORTRAIT_RATIO)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .pointerInput(Unit) {
                    detectTapGestures { position -> split = (position.x / size.width).coerceIn(0f, 1f) }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        split = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                }
                .clearAndSetSemantics { contentDescription = description }
        ) {
            AsyncImage(
                model = fileOf(comparison.before),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            AsyncImage(
                model = fileOf(comparison.after),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent { clipRect(left = size.width * split) { this@drawWithContent.drawContent() } }
            )
            // Trait du curseur et sa poignée.
            val lineX = (constraints.maxWidth * split).roundToInt()
            Box(
                modifier = Modifier
                    .offset { IntOffset(lineX - 1.dp.roundToPx(), 0) }
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(lineX - 14.dp.roundToPx(), 0) }
                    .size(28.dp)
                    .background(Color.White, CircleShape)
                    .border(BorderStroke(3.dp, MaterialTheme.colorScheme.primaryContainer), CircleShape)
            )
            PhotoLabel(stringResource(R.string.compare_before) + " · " + beforeDay, Modifier.align(Alignment.TopStart))
            PhotoLabel(stringResource(R.string.compare_after) + " · " + afterDay, Modifier.align(Alignment.TopEnd))
        }
        val sliderDescription = stringResource(R.string.compare_slider)
        Slider(
            value = split,
            onValueChange = { split = it },
            modifier = Modifier.semantics { contentDescription = sliderDescription }
        )
    }
}

@Composable
private fun PhotoLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = modifier
            .padding(8.dp)
            .background(Color.Black.copy(alpha = LABEL_ALPHA), MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

// Bandeau de vignettes pour choisir la photo « avant » ou « après ». La photo choisie est entourée.
@Composable
private fun PhotoStrip(
    title: String,
    photos: List<ProgressPhoto>,
    selected: ProgressPhoto,
    fileOf: (ProgressPhoto) -> File,
    onSelect: (ProgressPhoto) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title, Modifier.padding(horizontal = 16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            photos.forEach { photo ->
                val isSelected = photo.id == selected.id
                Surface(
                    shape = MaterialTheme.shapes.small,
                    border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .width(88.dp)
                        .semantics { this.selected = isSelected }
                ) {
                    PhotoThumbnail(photo, fileOf(photo), onClick = { onSelect(photo) })
                }
            }
        }
    }
}
