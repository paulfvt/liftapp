package com.paulfvt.liftapp.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R

// Poignée de glisser-déposer, zone tactile de 48 dp. Le modifier reçoit le geste (Modifier.draggableHandle).
// Sans description : la liste doit proposer au lecteur d'écran des actions « Monter » et « Descendre ».
@Composable
fun DragHandle(modifier: Modifier = Modifier) {
    Icon(
        painterResource(R.drawable.ic_drag_indicator),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(48.dp).padding(12.dp)
    )
}
