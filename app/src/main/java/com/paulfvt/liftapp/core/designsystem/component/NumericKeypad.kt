package com.paulfvt.liftapp.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R

// Touche facultative de la colonne de droite, troisième rangée (le RPE pendant la séance). onClick nul : grisée.
class KeypadSideAction(val label: String, val description: String, val onClick: (() -> Unit)?)

// Pavé numérique façon Strong (décision du 2 novembre 2026), partagé par la séance et la nutrition : chiffres à
// gauche ; fermer, − et +, la touche facultative et l'action principale à droite. cornerKey : touche en bas à gauche
// (virgule, deux-points ou rien). stepLabel : ce que font − et +, lu par le lecteur d'écran (« 1,25 kg »).
@Composable
fun NumericKeypad(
    cornerKey: Char?,
    stepLabel: String,
    actionLabel: String,
    onType: (Char) -> Unit,
    onErase: () -> Unit,
    onStep: (Int) -> Unit,
    onClose: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    sideAction: KeypadSideAction? = null,
    actionEnabled: Boolean = true
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(KeyGap),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        KeyRow {
            DigitKeys("123", onType)
            IconKey(R.drawable.ic_keyboard_hide, stringResource(R.string.keypad_close), onClick = onClose)
        }
        KeyRow {
            DigitKeys("456", onType)
            StepKeys(stepLabel, onStep)
        }
        KeyRow {
            DigitKeys("789", onType)
            if (sideAction == null) {
                Spacer(Modifier.weight(SIDE_WEIGHT))
            } else {
                SideKey(onClick = { sideAction.onClick?.invoke() }, enabled = sideAction.onClick != null) {
                    ButtonLabel(sideAction.label, description = sideAction.description)
                }
            }
        }
        KeyRow {
            Key(onClick = { cornerKey?.let(onType) }, enabled = cornerKey != null) {
                Text(cornerKey?.toString().orEmpty(), style = MaterialTheme.typography.headlineSmall)
            }
            DigitKeys("0", onType)
            Key(onClick = onErase) {
                Icon(painterResource(R.drawable.ic_backspace), stringResource(R.string.keypad_erase))
            }
            val colors = MaterialTheme.colorScheme
            SideKey(
                onClick = onAction,
                enabled = actionEnabled,
                color = if (actionEnabled) colors.primaryContainer else colors.surfaceContainerHighest
            ) {
                ButtonLabel(
                    actionLabel,
                    color = if (actionEnabled) colors.onPrimaryContainer else colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(KeyGap),
        modifier = Modifier.fillMaxWidth().height(KeyHeight),
        content = content
    )
}

@Composable
private fun RowScope.DigitKeys(digits: String, onType: (Char) -> Unit) {
    digits.forEach { digit ->
        Key(onClick = { onType(digit) }) {
            Text(digit.toString(), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

// Touche de chiffre : sans fond, comme dans Strong, pour que la colonne de droite ressorte.
@Composable
private fun RowScope.Key(onClick: () -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    ) { content() }
}

// Colonne de droite, un peu plus large : − et + y tiennent côte à côte avec 48 dp chacun.
@Composable
private fun RowScope.SideKey(
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(SIDE_WEIGHT)
            .fillMaxHeight()
            .background(color, MaterialTheme.shapes.small)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    ) { content() }
}

@Composable
private fun RowScope.IconKey(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    SideKey(onClick = onClick) { Icon(painterResource(icon), description) }
}

@Composable
private fun RowScope.StepKeys(stepLabel: String, onStep: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .weight(SIDE_WEIGHT)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small)
    ) {
        Key(onClick = { onStep(-1) }) {
            Icon(painterResource(R.drawable.ic_remove), stringResource(R.string.gym_decrease, stepLabel))
        }
        Key(onClick = { onStep(1) }) {
            Icon(painterResource(R.drawable.ic_add), stringResource(R.string.gym_increase, stepLabel))
        }
    }
}

private val KeyHeight = 52.dp
private val KeyGap = 6.dp
private const val SIDE_WEIGHT = 1.2f
