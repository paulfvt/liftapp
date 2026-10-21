package com.paulfvt.liftapp.workout.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.workout.notification.AlertPermission

// Première séance : explique puis demande les notifications (Android 13+), puis l'accès « Alarmes et rappels ».
// Une seule fois : ensuite, le minuteur propose « Activer » tant qu'il manque quelque chose.
@Composable
fun WorkoutPermissionRequests(asked: Boolean, missingPermissions: () -> List<AlertPermission>, onFinished: () -> Unit) {
    val context = LocalContext.current
    // Étape affichée ; rememberSaveable la garde après une rotation.
    var step by rememberSaveable { mutableStateOf<AlertPermission?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Notifications accordées ou non, on passe aux alarmes si elles manquent aussi.
        step = missingPermissions().firstOrNull { it == AlertPermission.EXACT_ALARMS }
        if (step == null) onFinished()
    }
    LaunchedEffect(asked) {
        if (!asked) {
            step = missingPermissions().firstOrNull()
            if (step == null) onFinished()
        }
    }
    val later = {
        step = null
        onFinished()
    }
    when (step) {
        null -> Unit
        AlertPermission.NOTIFICATIONS -> PermissionDialog(
            title = stringResource(R.string.permission_notifications_title),
            text = stringResource(R.string.permission_notifications_text),
            onAllow = {
                step = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Avant Android 13, pas de demande possible : les notifications ont été coupées dans les réglages.
                    context.openAlertSettings(AlertPermission.NOTIFICATIONS)
                    onFinished()
                }
            },
            onLater = later
        )
        AlertPermission.EXACT_ALARMS -> PermissionDialog(
            title = stringResource(R.string.permission_alarms_title),
            text = stringResource(R.string.permission_alarms_text),
            onAllow = {
                step = null
                context.openAlertSettings(AlertPermission.EXACT_ALARMS)
                onFinished()
            },
            onLater = later
        )
    }
}

@Composable
private fun PermissionDialog(title: String, text: String, onAllow: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onAllow) { Text(stringResource(R.string.permission_allow)) } },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.permission_later)) } }
    )
}

// Ouvre l'écran des réglages Android qui accorde ce qui manque. Le retour dans l'app relance la vérification
// (WorkoutViewModel.onResume).
fun Context.openAlertSettings(permission: AlertPermission) {
    val intent = when (permission) {
        AlertPermission.NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        AlertPermission.EXACT_ALARMS -> {
            // Avant Android 12, les alarmes exactes ne demandent aucune permission : rien à ouvrir.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:$packageName".toUri())
        }
    }
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w("WorkoutPermissions", "Écran de réglages introuvable", e)
    }
}
