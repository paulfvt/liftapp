package com.paulfvt.liftapp.profile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoLockSettingsScreen(onBack: () -> Unit, viewModel: PhotoLockSettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val setup = uiState.setup
    // Pendant le choix d'un code, « Retour » annule ce choix au lieu de quitter l'écran.
    BackHandler(enabled = setup != null) { viewModel.cancelSetup() }
    var confirmDisable by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { if (setup != null) viewModel.cancelSetup() else onBack() }) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                title = { ScreenTitle(stringResource(R.string.lock_settings_title)) }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Unit
            setup != null -> PinEntryContent(
                title = stringResource(
                    if (setup.step ==
                        PinStep.CONFIRM_PIN
                    ) {
                        R.string.lock_confirm_pin_title
                    } else {
                        R.string.lock_new_pin_title
                    }
                ),
                subtitle = stringResource(
                    if (setup.step == PinStep.CONFIRM_PIN) {
                        R.string.lock_confirm_pin_subtitle
                    } else {
                        R.string.lock_new_pin_subtitle
                    }
                ),
                entry = setup,
                message = if (setup.error == PinError.MISMATCH) stringResource(R.string.lock_mismatch) else null,
                enabled = !setup.isBusy,
                onDigit = viewModel::onDigit,
                onDelete = viewModel::onDelete,
                onConfirm = viewModel::onConfirm,
                modifier = Modifier.padding(padding)
            ) {
                LiftTextButton(stringResource(R.string.action_cancel), onClick = viewModel::cancelSetup)
            }
            else -> SettingsList(
                uiState = uiState,
                onToggleLock = { enable -> if (enable) viewModel.startSetup() else confirmDisable = true },
                onChangePin = viewModel::startSetup,
                onToggleBiometric = viewModel::setBiometric,
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (confirmDisable) {
        AlertDialog(
            onDismissRequest = { confirmDisable = false },
            title = { Text(stringResource(R.string.lock_disable_title)) },
            text = { Text(stringResource(R.string.lock_disable_text)) },
            confirmButton = {
                LiftTextButton(
                    text = stringResource(R.string.lock_disable_confirm),
                    onClick = {
                        confirmDisable = false
                        viewModel.disable()
                    }
                )
            },
            dismissButton = {
                LiftTextButton(stringResource(R.string.action_cancel), onClick = { confirmDisable = false })
            }
        )
    }
}

@Composable
private fun SettingsList(
    uiState: PhotoLockSettingsUiState,
    onToggleLock: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val biometricAvailable = LocalContext.current.canUseBiometric()
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Toute la ligne est cliquable, avec le rôle d'interrupteur pour le lecteur d'écran.
        ListItem(
            headlineContent = { Text(stringResource(R.string.lock_enable)) },
            supportingContent = { Text(stringResource(R.string.lock_enable_detail)) },
            trailingContent = { Switch(checked = uiState.isEnabled, onCheckedChange = null) },
            modifier = Modifier.clickable(role = Role.Switch) { onToggleLock(!uiState.isEnabled) }
        )
        if (uiState.isEnabled) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.lock_change_pin)) },
                modifier = Modifier.clickable(role = Role.Button, onClick = onChangePin)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.lock_biometric)) },
                supportingContent = {
                    val detail = if (biometricAvailable) {
                        R.string.lock_biometric_detail
                    } else {
                        R.string.lock_biometric_unavailable
                    }
                    Text(stringResource(detail))
                },
                trailingContent = {
                    Switch(
                        checked = uiState.useBiometric && biometricAvailable,
                        onCheckedChange = null,
                        enabled = biometricAvailable
                    )
                },
                modifier = Modifier.clickable(role = Role.Switch, enabled = biometricAvailable) {
                    onToggleBiometric(!uiState.useBiometric)
                }
            )
        }
        Text(
            stringResource(R.string.lock_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}
