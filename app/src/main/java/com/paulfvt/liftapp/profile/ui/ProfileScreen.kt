package com.paulfvt.liftapp.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.workout.domain.formatDecimal
import java.util.Locale

// Onglet Profil : le profil fixe, puis les mesures, les photos et « À propos ».
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onEditProfile: () -> Unit, onOpenAbout: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { ScreenTitle(stringResource(R.string.nav_profile)) }) }) { padding ->
        if (uiState.isLoading) return@Scaffold
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            ProfileCard(uiState, onEditProfile, modifier = Modifier.padding(horizontal = 16.dp))
            Text(
                stringResource(R.string.profile_coming_soon),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.about_title)) },
                supportingContent = { Text(stringResource(R.string.profile_about_summary)) },
                leadingContent = { Icon(painterResource(R.drawable.ic_info), contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.clickable(role = Role.Button, onClick = onOpenAbout)
            )
        }
    }
}

// « Paul · 181 cm · 28 ans · Homme ». Un appui ouvre la modification.
@Composable
private fun ProfileCard(uiState: ProfileUiState, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val profile = uiState.profile
    val details = buildList {
        profile.heightCm?.let { add(stringResource(R.string.profile_height_value, formatDecimal(it))) }
        uiState.age?.let { add(pluralStringResource(R.plurals.profile_age, it, it)) }
        profile.sex?.let { add(stringResource(sexLabel(it))) }
    }
    ElevatedCard(onClick = onEdit, modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(
                    56.dp
                ).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
            ) {
                val initial = profile.firstName?.first()?.toString()?.uppercase(Locale.FRENCH)
                if (initial != null) {
                    Text(
                        initial,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        painterResource(R.drawable.ic_person),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.firstName ?: stringResource(R.string.profile_title_empty),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    details.joinToString(" · ").ifEmpty { stringResource(R.string.profile_details_empty) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painterResource(R.drawable.ic_edit),
                contentDescription = stringResource(R.string.profile_edit_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
