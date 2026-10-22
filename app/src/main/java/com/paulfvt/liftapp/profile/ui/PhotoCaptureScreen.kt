package com.paulfvt.liftapp.profile.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.paulfvt.liftapp.R
import java.io.File

private const val PORTRAIT_RATIO = 3f / 4f
private const val GHOST_ALPHA = 0.35f

// Prise de photo en plein écran : aperçu de la caméra au format 3:4, silhouette de la dernière photo de la même
// pose en transparence, choix de la pose et de la caméra. La permission caméra est demandée ici, au moment où elle
// sert.
@Composable
fun PhotoCaptureScreen(onClose: () -> Unit, viewModel: PhotoCaptureViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
        askedOnce = true
    }
    LaunchedEffect(Unit) {
        if (!hasPermission && !askedOnce) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(viewModel.isSaved) {
        if (viewModel.isSaved) onClose()
    }
    // Fond noir, texte clair : comme dans une application d'appareil photo, quel que soit le thème.
    Surface(color = Color.Black, contentColor = Color.White, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onClose) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_cancel))
                }
                Text(
                    stringResource(R.string.photo_take),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (hasPermission) {
                    IconToggleButton(checked = viewModel.showGhost, onCheckedChange = viewModel::onGhostToggle) {
                        Icon(
                            painterResource(R.drawable.ic_layers),
                            stringResource(R.string.camera_ghost),
                            tint = if (viewModel.showGhost) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }
            }
            if (hasPermission) {
                CameraContent(viewModel)
            } else {
                PermissionRequest(
                    onAllow = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        viewModel.beforeLeavingApp()
                        context.openAppSettings()
                    }
                )
            }
        }
    }
}

@Composable
private fun CameraContent(viewModel: PhotoCaptureViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var useFrontCamera by rememberSaveable { mutableStateOf(false) }
    val camera = remember { CameraUseCases() }
    BindCamera(camera, useFrontCamera)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(PORTRAIT_RATIO).background(Color.DarkGray)) {
            camera.surfaceRequest?.let { request ->
                CameraXViewfinder(surfaceRequest = request, modifier = Modifier.fillMaxSize())
            }
            val ghost = uiState.ghost
            if (viewModel.showGhost && ghost != null) {
                AsyncImage(
                    model = ghost,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = GHOST_ALPHA,
                    modifier = Modifier
                        .fillMaxSize()
                        // L'aperçu de la caméra avant est un miroir : la silhouette l'est aussi pour s'y superposer.
                        .graphicsLayer { scaleX = if (useFrontCamera) -1f else 1f }
                )
            }
        }
        Text(
            stringResource(
                when {
                    viewModel.captureFailed -> R.string.camera_error
                    uiState.ghost == null -> R.string.camera_first_photo
                    else -> R.string.camera_ghost_hint
                }
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = if (viewModel.captureFailed) MaterialTheme.colorScheme.error else Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        PoseSelector(
            selected = uiState.pose,
            onSelect = viewModel::onPoseChange,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            IconButton(onClick = { useFrontCamera = !useFrontCamera }, modifier = Modifier.size(56.dp)) {
                Icon(painterResource(R.drawable.ic_flip_camera), stringResource(R.string.camera_switch))
            }
            ShutterButton(
                busy = viewModel.isCapturing,
                onClick = {
                    viewModel.onCaptureStarted()
                    camera.takePicture(context, onSaved = viewModel::onCaptured, onError = viewModel::onCaptureFailed)
                }
            )
            // Même largeur que le bouton de gauche : le déclencheur reste centré.
            Spacer(Modifier.size(56.dp))
        }
    }
}

// Déclencheur : grand rond blanc cerclé, 80 dp pour être facile à toucher le bras tendu.
@Composable
private fun ShutterButton(busy: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.camera_shutter)
    Surface(
        onClick = onClick,
        enabled = !busy,
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier
            .size(80.dp)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
    ) {
        if (busy) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primaryContainer)
            }
        }
    }
}

@Composable
private fun PermissionRequest(onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Icon(
            painterResource(R.drawable.ic_photo_camera),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            stringResource(R.string.camera_permission_text),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onAllow,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) {
            Text(stringResource(R.string.camera_permission_allow))
        }
        // Après deux refus, Android ne montre plus la demande : il faut passer par les réglages de l'app.
        OutlinedButton(
            onClick = onOpenSettings,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) {
            Text(stringResource(R.string.camera_permission_settings), color = Color.White)
        }
    }
}

// Aperçu et prise de vue de CameraX, au format 4:3 (3:4 en portrait), comme les photos enregistrées.
private class CameraUseCases {
    var surfaceRequest by mutableStateOf<SurfaceRequest?>(null)
        private set

    private val resolution = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .build()

    val preview: Preview = Preview.Builder().setResolutionSelector(resolution).build().apply {
        setSurfaceProvider { request -> surfaceRequest = request }
    }

    val capture: ImageCapture = ImageCapture.Builder()
        .setResolutionSelector(resolution)
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    // La photo brute est écrite dans le cache, puis copiée réduite et redressée par PhotoRepository.
    fun takePicture(context: Context, onSaved: (File) -> Unit, onError: () -> Unit) {
        val file = File(context.cacheDir, "capture-${System.currentTimeMillis()}.jpg")
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) = onSaved(file)

                override fun onError(exception: ImageCaptureException) {
                    file.delete()
                    onError()
                }
            }
        )
    }
}

// Relie l'aperçu et la prise de vue à la caméra choisie, tant que l'écran est affiché.
@Composable
private fun BindCamera(camera: CameraUseCases, useFrontCamera: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    LaunchedEffect(useFrontCamera) {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        provider = cameraProvider
        val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.unbindAll()
        if (cameraProvider.hasCamera(selector)) {
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, camera.preview, camera.capture)
        }
    }
    DisposableEffect(Unit) {
        onDispose { provider?.unbindAll() }
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
