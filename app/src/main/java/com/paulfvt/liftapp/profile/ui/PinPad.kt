package com.paulfvt.liftapp.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftButtonSize
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.profile.domain.PIN_MAX_LENGTH
import com.paulfvt.liftapp.profile.domain.PIN_MIN_LENGTH

private val KEY_SIZE = 72.dp
private val DOT_SIZE = 14.dp

// Un point par chiffre tapé, sans jamais montrer les chiffres. Le lecteur d'écran annonce seulement leur nombre.
@Composable
fun PinDots(length: Int, modifier: Modifier = Modifier) {
    val description = pluralStringResource(R.plurals.pin_digits_entered, length, length)
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.clearAndSetSemantics { contentDescription = description }
    ) {
        repeat(maxOf(length, PIN_MIN_LENGTH)) { index ->
            val filled = index < length
            Box(
                Modifier
                    .size(DOT_SIZE)
                    .background(
                        if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        CircleShape
                    )
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

// Pavé numérique : touches de 72 dp, « Effacer » en bas à gauche et « Valider » en bas à droite.
@Composable
fun PinPad(
    length: Int,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val rows = listOf("123", "456", "789")
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { digit -> DigitKey(digit, enabled && length < PIN_MAX_LENGTH, onDigit) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(KEY_SIZE), contentAlignment = Alignment.Center) {
                IconButton(onClick = onDelete, enabled = enabled && length > 0, modifier = Modifier.size(KEY_SIZE)) {
                    Icon(painterResource(R.drawable.ic_backspace), stringResource(R.string.pin_delete))
                }
            }
            DigitKey('0', enabled && length < PIN_MAX_LENGTH, onDigit)
            Box(Modifier.size(KEY_SIZE), contentAlignment = Alignment.Center) {
                LiftTextButton(
                    text = stringResource(R.string.action_ok),
                    onClick = onConfirm,
                    enabled = enabled && length >= PIN_MIN_LENGTH,
                    size = LiftButtonSize.Large
                )
            }
        }
    }
}

@Composable
private fun DigitKey(digit: Char, enabled: Boolean, onDigit: (Char) -> Unit) {
    FilledTonalButton(
        onClick = { onDigit(digit) },
        enabled = enabled,
        shape = CircleShape,
        modifier = Modifier.size(KEY_SIZE)
    ) {
        Text(digit.toString(), style = MaterialTheme.typography.headlineSmall)
    }
}
