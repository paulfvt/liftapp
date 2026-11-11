package com.paulfvt.liftapp.nutrition.ui

import android.net.Uri
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftButtonSize
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.SignalButton
import com.paulfvt.liftapp.core.designsystem.component.hairlineBorder
import com.paulfvt.liftapp.core.designsystem.theme.LocalExtendedColors
import com.paulfvt.liftapp.core.ui.hasCameraPermission
import com.paulfvt.liftapp.nutrition.domain.LabelReading
import java.util.concurrent.Executor

// Assombrit la vue de la caméra autour du cadre, pour guider le regard.
private val SCRIM = Color.Black.copy(alpha = 0.62f)
private val FRAME_RADIUS = 16.dp
private val FRAME_STROKE = 4.dp
private val FRAME_CORNER = 28.dp

// Images de 1 920 × 1 080 : de quoi lire les petits caractères d'un tableau qui remplit le cadre.
private val ANALYSIS_SIZE = Size(1920, 1080)

// Scan en direct de l'étiquette (11 novembre 2026) : la caméra lit le tableau en continu, les valeurs retenues
// s'affichent en bas dès que deux lectures concordent, puis « Utiliser ces valeurs » les rend à l'écran de l'aliment.
// La permission caméra est demandée avant d'ouvrir cet écran. Aucune image n'est enregistrée.
@Composable
fun LabelScanScreen(
    onClose: () -> Unit,
    onUse: (LabelReading) -> Unit,
    onPickImage: (Uri) -> Unit,
    viewModel: LabelScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (!context.hasCameraPermission()) onClose()
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPickImage)
    }
    val camera = remember { ScanCamera(viewModel.analyzer, ContextCompat.getMainExecutor(context)) }
    var isTorchOn by rememberSaveable { mutableStateOf(false) }
    BindScanCamera(camera, isTorchOn)
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    var frameInWindow by remember { mutableStateOf<Rect?>(null) }
    val result = viewModel.votes.result

    // Fond noir et texte clair, comme une application d'appareil photo ; le volet des valeurs suit le thème.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { rootOffset = it.positionInWindow() }
    ) {
        camera.surfaceRequest?.let { request ->
            CameraXViewfinder(surfaceRequest = request, modifier = Modifier.fillMaxSize())
        }
        FrameOverlay(frameInWindow?.translate(-rootOffset))
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                hasTorch = camera.camera?.cameraInfo?.hasFlashUnit() == true,
                isTorchOn = isTorchOn,
                onTorchChange = { isTorchOn = it },
                onClose = onClose
            )
            Text(
                stringResource(R.string.nutrition_scan_frame_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            Text(
                stringResource(R.string.nutrition_scan_frame_text),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            // Le cadre occupe la place libre entre les consignes et le volet des valeurs.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .onGloballyPositioned { frameInWindow = it.boundsInWindow() }
            )
            Text(
                stringResource(R.string.nutrition_scan_live_status),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )
            ValuesSheet(
                result = result,
                isPerPortionOnly = viewModel.isPerPortionOnly && result.foundCount == 0,
                onUse = { onUse(result) },
                onPickImage = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )
        }
    }
}

@Composable
private fun TopBar(hasTorch: Boolean, isTorchOn: Boolean, onTorchChange: (Boolean) -> Unit, onClose: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(4.dp)
    ) {
        IconButton(onClick = onClose) {
            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_close), tint = Color.White)
        }
        Text(
            stringResource(R.string.nutrition_scan_live),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        // La lampe aide contre les reflets d'un emballage brillant ou une cuisine sombre.
        if (hasTorch) {
            IconToggleButton(checked = isTorchOn, onCheckedChange = onTorchChange) {
                Icon(
                    painterResource(R.drawable.ic_flash_on),
                    stringResource(R.string.nutrition_scan_torch),
                    tint = if (isTorchOn) MaterialTheme.colorScheme.primary else Color.White
                )
            }
        }
    }
}

// Voile sombre percé à l'emplacement du cadre, avec ses quatre coins à la couleur de l'app.
@Composable
private fun FrameOverlay(frame: Rect?) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            // Hors écran : le trou découpé dans le voile laisse voir la caméra au lieu d'un fond noir.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        drawRect(SCRIM)
        if (frame == null) return@Canvas
        val radius = FRAME_RADIUS.toPx()
        drawRoundRect(Color.Transparent, frame.topLeft, frame.size, CornerRadius(radius), blendMode = BlendMode.Clear)
        val length = FRAME_CORNER.toPx()
        val topLeft = Path().apply {
            moveTo(frame.left, frame.top + length)
            lineTo(frame.left, frame.top + radius)
            arcTo(Rect(frame.left, frame.top, frame.left + 2 * radius, frame.top + 2 * radius), 180f, 90f, false)
            lineTo(frame.left + length, frame.top)
        }
        val stroke = Stroke(width = FRAME_STROKE.toPx(), cap = StrokeCap.Round)
        // Le coin haut gauche, retourné en miroir pour les trois autres.
        listOf(1f to 1f, -1f to 1f, 1f to -1f, -1f to -1f).forEach { (scaleX, scaleY) ->
            scale(scaleX, scaleY, pivot = frame.center) { drawPath(topLeft, accent, style = stroke) }
        }
    }
}

@Composable
private fun ValuesSheet(result: LabelReading, isPerPortionOnly: Boolean, onUse: () -> Unit, onPickImage: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.navigationBarsPadding().padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.nutrition_scan_values_title).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    pluralStringResource(R.plurals.nutrition_scan_found, result.foundCount, result.foundCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ValueTile(R.string.nutrition_energy, result.kcal?.let { kcalText(it) }, Modifier.weight(1f))
                ValueTile(R.string.nutrition_protein, result.protein?.let { gramsText(it) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ValueTile(R.string.nutrition_carbs, result.carbs?.let { gramsText(it) }, Modifier.weight(1f))
                ValueTile(R.string.nutrition_fat, result.fat?.let { gramsText(it) }, Modifier.weight(1f))
            }
            if (isPerPortionOnly) {
                Text(
                    stringResource(R.string.nutrition_scan_per_portion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            SignalButton(
                text = stringResource(R.string.nutrition_scan_use),
                onClick = onUse,
                enabled = result.foundCount > 0,
                size = LiftButtonSize.Large,
                modifier = Modifier.fillMaxWidth()
            )
            LiftTextButton(
                text = stringResource(R.string.nutrition_scan_pick_instead),
                onClick = onPickImage,
                icon = R.drawable.ic_photo_library,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Une valeur et son état : trouvée (coche verte) ou encore cherchée.
@Composable
private fun ValueTile(@StringRes label: Int, value: String?, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.medium
    val valueColor = if (value !=
        null
    ) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(hairlineBorder(), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value ?: stringResource(R.string.nutrition_scan_searching),
                style = if (value != null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                color = valueColor
            )
        }
        if (value != null) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = LocalExtendedColors.current.success,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Aperçu et analyse de CameraX au même format 16:9 : ce qui est lu est ce que l'on voit.
private class ScanCamera(analyzer: ImageAnalysis.Analyzer, executor: Executor) {
    var surfaceRequest by mutableStateOf<SurfaceRequest?>(null)
        private set

    var camera by mutableStateOf<Camera?>(null)

    private val resolution = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
        .setResolutionStrategy(
            ResolutionStrategy(ANALYSIS_SIZE, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
        )
        .build()

    val preview: Preview = Preview.Builder().setResolutionSelector(resolution).build().apply {
        setSurfaceProvider { request -> surfaceRequest = request }
    }

    val analysis: ImageAnalysis = ImageAnalysis.Builder()
        .setResolutionSelector(resolution)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .apply { setAnalyzer(executor, analyzer) }
}

// Relie l'aperçu et l'analyse à la caméra arrière tant que l'écran est affiché ; allume ou éteint la lampe.
@Composable
private fun BindScanCamera(camera: ScanCamera, isTorchOn: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        provider = cameraProvider
        val selector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.unbindAll()
        if (cameraProvider.hasCamera(selector)) {
            camera.camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, camera.preview, camera.analysis)
        }
    }
    LaunchedEffect(camera.camera, isTorchOn) {
        camera.camera?.takeIf { it.cameraInfo.hasFlashUnit() }?.cameraControl?.enableTorch(isTorchOn)
    }
    DisposableEffect(Unit) {
        onDispose { provider?.unbindAll() }
    }
}
