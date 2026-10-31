package com.paulfvt.liftapp.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.util.Locale

// Boutons du guide de style ON AIR (design-system/styles.css, classes .btn) : coins de 8 dp (12 dp en grand format),
// libellé en majuscules espacées. Ils remplacent les boutons Material 3, arrondis en pilule et non modifiables par
// le thème. Rôles : SignalButton pour l'action principale d'un écran (rouge signal), LiftOutlinedButton pour une
// action secondaire, LiftTonalButton pour une action d'appoint, LiftTextButton dans les dialogues et les cartes.

enum class LiftButtonSize(val minHeight: Dp) {
    // 40 dp de haut, posé dans une zone tactile de 48 dp par Material.
    Regular(40.dp),

    // .btn-lg : actions principales en bas d'écran.
    Large(56.dp),

    // Mode salle : boutons géants, utilisables d'une main entre deux séries.
    Gym(72.dp)
}

@Composable
private fun LiftButtonSize.shape(): Shape =
    if (this == LiftButtonSize.Regular) MaterialTheme.shapes.small else MaterialTheme.shapes.medium

@Composable
private fun LiftButtonSize.labelStyle(): TextStyle = MaterialTheme.typography.labelLarge.copy(
    fontSize = when (this) {
        LiftButtonSize.Regular -> 14.sp
        LiftButtonSize.Large -> 16.sp
        LiftButtonSize.Gym -> 20.sp
    },
    letterSpacing = 0.06.em
)

// Libellé en capitales. Le lecteur d'écran lit le texte d'origine : certains épellent un mot tout en capitales.
@Composable
fun ButtonLabel(text: String, size: LiftButtonSize = LiftButtonSize.Regular, color: Color = Color.Unspecified) {
    Text(
        text.uppercase(Locale.FRENCH),
        style = size.labelStyle(),
        color = color,
        modifier = Modifier.semantics { contentDescription = text }
    )
}

@Composable
private fun RowScope.LabelWithIcon(text: String, @DrawableRes icon: Int?, size: LiftButtonSize, isLoading: Boolean) {
    if (isLoading) {
        CircularProgressIndicator(
            color = LocalContentColor.current,
            strokeWidth = 2.dp,
            modifier = Modifier.size(24.dp)
        )
        return
    }
    if (icon != null) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .padding(end = ButtonDefaults.IconSpacing)
                .size(if (size == LiftButtonSize.Regular) ButtonDefaults.IconSize else 20.dp)
        )
    }
    ButtonLabel(text, size)
}

private fun contentPadding(@DrawableRes icon: Int?): PaddingValues =
    if (icon != null) ButtonDefaults.ButtonWithIconContentPadding else ButtonDefaults.ContentPadding

// Plein rouge signal (.btn-filled) : l'action principale de l'écran. isLoading remplace le libellé par un indicateur.
@Composable
fun SignalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    size: LiftButtonSize = LiftButtonSize.Regular,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = size.shape(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = contentPadding(icon),
        modifier = modifier.heightIn(min = size.minHeight)
    ) { LabelWithIcon(text, icon, size, isLoading) }
}

// À contour (.btn-outlined) : action secondaire. color remplace la couleur du texte et du contour (sur une photo,
// par exemple).
@Composable
fun LiftOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    size: LiftButtonSize = LiftButtonSize.Regular,
    color: Color = Color.Unspecified
) {
    val content = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    val border = if (color == Color.Unspecified) MaterialTheme.colorScheme.outline else color.copy(alpha = 0.5f)
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = size.shape(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = content),
        border = if (enabled) BorderStroke(1.dp, border) else ButtonDefaults.outlinedButtonBorder(enabled = false),
        contentPadding = contentPadding(icon),
        modifier = modifier.heightIn(min = size.minHeight)
    ) { LabelWithIcon(text, icon, size, isLoading = false) }
}

// Tonal (.btn-tonal) : action d'appoint, comme −15 s et +30 s du minuteur.
@Composable
fun LiftTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    size: LiftButtonSize = LiftButtonSize.Regular
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = size.shape(),
        contentPadding = contentPadding(icon),
        modifier = modifier.heightIn(min = size.minHeight)
    ) { LabelWithIcon(text, icon, size, isLoading = false) }
}

// Texte (.btn-text) : actions des dialogues et des cartes. color : rouge d'erreur pour une action destructrice.
@Composable
fun LiftTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    size: LiftButtonSize = LiftButtonSize.Regular,
    color: Color = Color.Unspecified
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = size.shape(),
        colors = if (color == Color.Unspecified) {
            ButtonDefaults.textButtonColors()
        } else {
            ButtonDefaults.textButtonColors(contentColor = color)
        },
        contentPadding = if (icon !=
            null
        ) {
            ButtonDefaults.TextButtonWithIconContentPadding
        } else {
            ButtonDefaults.TextButtonContentPadding
        },
        modifier = modifier.heightIn(min = size.minHeight)
    ) { LabelWithIcon(text, icon, size, isLoading = false) }
}
