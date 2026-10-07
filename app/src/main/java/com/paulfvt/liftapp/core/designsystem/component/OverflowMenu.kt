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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.paulfvt.liftapp.R

data class MenuEntry(
    @param:StringRes val textRes: Int,
    @param:DrawableRes val iconRes: Int? = null,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

// Bouton « ⋮ » et son menu. Une action destructive (supprimer) est affichée en rouge erreur.
@Composable
fun OverflowMenu(entries: List<MenuEntry>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            entries.forEach { entry ->
                val colors = if (entry.isDestructive) {
                    MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.error,
                        leadingIconColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    MenuDefaults.itemColors()
                }
                DropdownMenuItem(
                    text = { Text(stringResource(entry.textRes)) },
                    leadingIcon = entry.iconRes?.let { icon ->
                        { Icon(painterResource(icon), contentDescription = null) }
                    },
                    colors = colors,
                    onClick = {
                        expanded = false
                        entry.onClick()
                    }
                )
            }
        }
    }
}
