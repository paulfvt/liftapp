package com.paulfvt.liftapp.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.paulfvt.liftapp.R

data class MenuEntry(
    @param:StringRes val textRes: Int,
    @param:DrawableRes val iconRes: Int? = null,
    val isDestructive: Boolean = false,
    val isEnabled: Boolean = true,
    // Non nul : réglage activable, affiché avec un interrupteur à droite.
    val isChecked: Boolean? = null,
    val onClick: () -> Unit
)

// Bouton « ⋮ » et son menu.
@Composable
fun OverflowMenu(entries: List<MenuEntry>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            entries.forEach { entry ->
                val state = entry.isChecked?.let {
                    stringResource(if (it) R.string.setting_on else R.string.setting_off)
                }
                DropdownMenuItem(
                    text = { Text(stringResource(entry.textRes)) },
                    leadingIcon = entry.iconRes?.let { icon ->
                        { Icon(painterResource(icon), contentDescription = null) }
                    },
                    // L'interrupteur n'est qu'un affichage : c'est toute la ligne qui bascule le réglage.
                    trailingIcon = entry.isChecked?.let { checked ->
                        { Switch(checked = checked, onCheckedChange = null) }
                    },
                    modifier = if (state != null) Modifier.semantics { stateDescription = state } else Modifier,
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

// Une action destructive (supprimer, retirer) est affichée en rouge erreur.
@Composable
fun menuEntryColors(entry: MenuEntry): MenuItemColors = if (entry.isDestructive) {
    MenuDefaults.itemColors(
        textColor = MaterialTheme.colorScheme.error,
        leadingIconColor = MaterialTheme.colorScheme.error
    )
} else {
    MenuDefaults.itemColors()
}
