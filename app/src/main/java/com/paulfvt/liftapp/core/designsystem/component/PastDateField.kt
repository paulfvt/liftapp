package com.paulfvt.liftapp.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.ui.shortDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private const val OLDEST_YEAR_OFFSET = 100

// Champ de date (aujourd'hui ou avant) : une carte cliquable qui ouvre le calendrier. clearDescription non nul
// ajoute un bouton pour effacer la date ; firstShown est le mois montré quand aucune date n'est choisie.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastDateField(
    label: String,
    date: LocalDate?,
    today: LocalDate,
    onChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    emptyText: String = "",
    clearDescription: String? = null,
    errorText: String? = null,
    firstShown: LocalDate = today
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.action_edit)) {
                    showPicker = true
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
            ) {
                Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (errorText != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(date?.let { shortDate(it) } ?: emptyText, style = MaterialTheme.typography.bodyLarge)
                }
                if (date != null && clearDescription != null) {
                    IconButton(onClick = { onChange(null) }) {
                        Icon(painterResource(R.drawable.ic_close), clearDescription)
                    }
                }
            }
        }
        if (errorText != null) {
            Text(
                errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
    if (showPicker) {
        // Le calendrier de Material 3 travaille en millisecondes UTC : une date y est minuit UTC.
        val todayMillis = today.toUtcMillis()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.toUtcMillis(),
            initialDisplayedMonthMillis = (date ?: firstShown).toUtcMillis(),
            yearRange = (today.year - OLDEST_YEAR_OFFSET)..today.year,
            selectableDates = remember(todayMillis) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onChange(it.toUtcDate()) }
                        showPicker = false
                    }
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
