package com.paulfvt.liftapp.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.MenuEntry
import com.paulfvt.liftapp.core.designsystem.component.menuEntryColors
import com.paulfvt.liftapp.core.model.SetType

// Nom d'une série pour le lecteur d'écran : « Série 2 », « Échauffement », « Dégressive ».
@Composable
fun setName(type: SetType, workNumber: Int?): String =
    if (workNumber != null) stringResource(R.string.set_label_work, workNumber) else stringResource(type.labelRes)

// Pastille du type de série (numéro, « É » ou « D ») : un appui ouvre le choix échauffement, travail ou dégressive,
// suivi des actions supplémentaires de l'écran (RPE, retirer la série…).
@Composable
fun SetTypeButton(
    type: SetType,
    workNumber: Int?,
    onTypeChange: (SetType) -> Unit,
    extraEntries: List<MenuEntry> = emptyList()
) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.template_set_change_type, setName(type, workNumber))
    val (text, color) = when (type) {
        SetType.WARMUP -> stringResource(R.string.set_short_warmup) to MaterialTheme.colorScheme.tertiary
        SetType.DROP -> stringResource(R.string.set_short_drop) to MaterialTheme.colorScheme.secondary
        SetType.WORK -> workNumber.toString() to MaterialTheme.colorScheme.onSurface
    }
    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(48.dp).semantics { contentDescription = description }
        ) {
            Text(text, style = MaterialTheme.typography.headlineSmall, color = color)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SetType.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    trailingIcon = if (option == type) {
                        { Icon(painterResource(R.drawable.ic_check), contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onTypeChange(option)
                    }
                )
            }
            if (extraEntries.isNotEmpty()) HorizontalDivider()
            extraEntries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(stringResource(entry.textRes)) },
                    leadingIcon = entry.iconRes?.let { icon ->
                        { Icon(painterResource(icon), contentDescription = null) }
                    },
                    colors = menuEntryColors(entry),
                    enabled = entry.isEnabled,
                    onClick = {
                        expanded = false
                        entry.onClick()
                    }
                )
            }
        }
    }
}
