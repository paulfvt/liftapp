package com.paulfvt.liftapp.profile.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.BuildConfig
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.designsystem.component.SectionTitle
import com.paulfvt.liftapp.profile.data.OpenLicense

// Ce que l'app utilise et à qui on le doit.
private class Credit(@param:StringRes val nameRes: Int, @param:StringRes val detailRes: Int)

// Crédits regroupés par licence. Le catalogue est dans le domaine public : aucun texte de licence à joindre.
private class CreditGroup(@param:StringRes val titleRes: Int, val credits: List<Credit>, val license: OpenLicense?)

private val CREDIT_GROUPS = listOf(
    CreditGroup(
        R.string.about_credits_exercises,
        listOf(Credit(R.string.credit_free_exercise_db, R.string.credit_free_exercise_db_detail)),
        license = null
    ),
    CreditGroup(
        R.string.about_credits_body_map,
        listOf(Credit(R.string.credit_musclemap, R.string.credit_musclemap_detail)),
        OpenLicense.MIT_MUSCLEMAP
    ),
    CreditGroup(
        R.string.about_credits_fonts,
        listOf(Credit(R.string.credit_barlow, R.string.credit_barlow_detail)),
        OpenLicense.OFL_1_1
    ),
    CreditGroup(
        R.string.about_credits_libraries,
        listOf(
            Credit(R.string.credit_jetpack, R.string.credit_jetpack_detail),
            Credit(R.string.credit_hilt, R.string.credit_by_google),
            Credit(R.string.credit_kotlin, R.string.credit_kotlin_detail),
            Credit(R.string.credit_coil, R.string.credit_coil_detail),
            Credit(R.string.credit_reorderable, R.string.credit_reorderable_detail),
            Credit(R.string.credit_vico, R.string.credit_vico_detail),
            Credit(R.string.credit_material_icons, R.string.credit_by_google)
        ),
        OpenLicense.APACHE_2_0
    )
)

// Version de l'app et crédits. Écran sans état : pas de ViewModel.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, onOpenLicense: (OpenLicense) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                title = { ScreenTitle(stringResource(R.string.about_title)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AppHeader()
            CREDIT_GROUPS.forEach { group -> CreditCard(group, onOpenLicense) }
        }
    }
}

@Composable
private fun AppHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(stringResource(R.string.about_description), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CreditCard(group: CreditGroup, onOpenLicense: (OpenLicense) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(group.titleRes))
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                group.credits.forEach { credit ->
                    // Nom et détail lus d'un seul tenant par le lecteur d'écran.
                    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                        Text(stringResource(credit.nameRes), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(credit.detailRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                group.license?.let { license ->
                    TextButton(onClick = { onOpenLicense(license) }) {
                        Text(stringResource(R.string.about_read_license, stringResource(license.titleRes)))
                    }
                }
            }
        }
    }
}
