package com.paulfvt.liftapp.profile.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftOutlinedButton
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

private const val TICK_MILLIS = 1_000L

// Verrou devant un écran de photos : tant que les photos sont verrouillées, le pavé numérique remplace l'écran.
// L'écran lui-même (et son ViewModel) n'est créé qu'une fois déverrouillé.
@Composable
fun PhotoLockGate(
    onBack: () -> Unit,
    viewModel: PhotoLockViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        uiState.isLoading -> Unit
        uiState.isLocked -> LockScreen(uiState, viewModel, onBack)
        else -> content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LockScreen(uiState: PhotoLockUiState, viewModel: PhotoLockViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity
    val entry = uiState.entry
    val biometricTitle = stringResource(R.string.lock_biometric_title)
    val usePin = stringResource(R.string.lock_use_pin)
    val canUseBiometric = uiState.useBiometric && entry.step == PinStep.UNLOCK && context.canUseBiometric()
    val askBiometric = { activity?.askBiometric(biometricTitle, usePin, viewModel::onBiometricSuccess) }
    // Visage ou empreinte proposés tout de suite, le pavé reste derrière en secours.
    LaunchedEffect(Unit) {
        if (canUseBiometric) askBiometric()
    }
    var showNoScreenLock by rememberSaveable { mutableStateOf(false) }
    val resetTitle = stringResource(R.string.lock_reset_title)
    val resetSubtitle = stringResource(R.string.lock_reset_subtitle)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                title = { ScreenTitle(stringResource(R.string.profile_photos)) }
            )
        }
    ) { padding ->
        val remaining = rememberRemaining(uiState.lockedUntil)
        PinEntryContent(
            title = stringResource(
                when (entry.step) {
                    PinStep.UNLOCK -> R.string.lock_title
                    PinStep.NEW_PIN -> R.string.lock_new_pin_title
                    PinStep.CONFIRM_PIN -> R.string.lock_confirm_pin_title
                }
            ),
            subtitle = stringResource(
                when (entry.step) {
                    PinStep.UNLOCK -> R.string.lock_subtitle
                    PinStep.NEW_PIN -> R.string.lock_new_pin_subtitle
                    PinStep.CONFIRM_PIN -> R.string.lock_confirm_pin_subtitle
                }
            ),
            entry = entry,
            message = when {
                remaining != null -> stringResource(R.string.lock_wait, remaining.seconds)
                entry.error == PinError.WRONG_PIN -> stringResource(R.string.lock_wrong_pin)
                entry.error == PinError.MISMATCH -> stringResource(R.string.lock_mismatch)
                else -> null
            },
            enabled = remaining == null && !entry.isBusy,
            onDigit = viewModel::onDigit,
            onDelete = viewModel::onDelete,
            onConfirm = viewModel::onConfirm,
            modifier = Modifier.padding(padding)
        ) {
            if (canUseBiometric) {
                LiftOutlinedButton(
                    text = stringResource(R.string.lock_use_biometric),
                    onClick = { askBiometric() },
                    icon = R.drawable.ic_fingerprint
                )
            }
            if (entry.step == PinStep.UNLOCK) {
                LiftTextButton(
                    text = stringResource(R.string.lock_forgot),
                    onClick = {
                        if (context.hasScreenLock() && activity != null) {
                            activity.askScreenLock(resetTitle, resetSubtitle, viewModel::onScreenLockConfirmed)
                        } else {
                            showNoScreenLock = true
                        }
                    }
                )
            }
        }
    }

    if (showNoScreenLock) {
        AlertDialog(
            onDismissRequest = { showNoScreenLock = false },
            title = { Text(stringResource(R.string.lock_no_screen_lock_title)) },
            text = { Text(stringResource(R.string.lock_no_screen_lock_text)) },
            confirmButton = {
                LiftTextButton(stringResource(R.string.action_ok), onClick = { showNoScreenLock = false })
            }
        )
    }
}

// Temps restant avant un nouvel essai, recalculé chaque seconde ; null quand l'attente est finie.
@Composable
private fun rememberRemaining(until: Instant?): Duration? {
    val remaining by produceState<Duration?>(initialValue = null, until) {
        while (until != null) {
            val left = Duration.between(Instant.now(), until)
            value = left.takeIf { !it.isNegative && !it.isZero }?.plusMillis(TICK_MILLIS - 1)
            if (value == null) break
            delay(TICK_MILLIS)
        }
        value = null
    }
    return remaining
}

// Titre, points du code, message, pavé numérique et actions en dessous. Partagé par le verrou et ses réglages.
@Composable
fun PinEntryContent(
    title: String,
    subtitle: String,
    entry: PinEntryState,
    message: String?,
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Icon(
            painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        PinDots(entry.pin.length)
        // Zone annoncée par le lecteur d'écran dès qu'elle change (code faux, attente).
        Text(
            message.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        PinPad(
            length = entry.pin.length,
            onDigit = onDigit,
            onDelete = onDelete,
            onConfirm = onConfirm,
            enabled = enabled
        )
        actions()
    }
}
