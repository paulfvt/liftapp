package com.paulfvt.liftapp.settings.data

import android.content.Context
import android.net.Uri
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.history.data.HistoryRepository
import com.paulfvt.liftapp.profile.data.MeasurementRepository
import com.paulfvt.liftapp.profile.ui.unitRes
import com.paulfvt.liftapp.settings.domain.MeasurementCsvLabels
import com.paulfvt.liftapp.settings.domain.csvDocument
import com.paulfvt.liftapp.settings.domain.historyCsvRows
import com.paulfvt.liftapp.settings.domain.measurementCsvRows
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

// Export CSV de l'historique et des mesures dans un fichier choisi par l'utilisateur avec le sélecteur d'Android
// (aucune permission de stockage n'est nécessaire). Les nombres renvoyés servent au message de confirmation.
@Singleton
class CsvExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val history: HistoryRepository,
    private val measurements: MeasurementRepository,
    private val appSettings: AppSettingsRepository
) {
    // Renvoie le nombre de séances exportées.
    suspend fun exportHistory(target: Uri): Int {
        val workouts = history.observeFinished().first()
        val unit = appSettings.current().units.weight
        val resources = context.resources
        val header = listOf(
            resources.getString(R.string.csv_date),
            resources.getString(R.string.csv_time),
            resources.getString(R.string.csv_workout),
            resources.getString(R.string.csv_exercise),
            resources.getString(R.string.csv_set),
            resources.getString(R.string.csv_set_type),
            resources.getString(R.string.csv_load, resources.getString(unit.labelRes)),
            resources.getString(R.string.csv_reps),
            resources.getString(R.string.csv_duration),
            resources.getString(R.string.csv_distance),
            resources.getString(R.string.csv_rpe)
        )
        val rows = historyCsvRows(workouts, unit, ZoneId.systemDefault()) { resources.getString(it.labelRes) }
        write(target, csvDocument(header, rows))
        return workouts.size
    }

    // Renvoie le nombre de mesures exportées.
    suspend fun exportMeasurements(target: Uri): Int {
        val entries = measurements.observeAll().first()
        val units = appSettings.current().units
        val resources = context.resources
        val header = listOf(
            R.string.csv_date,
            R.string.csv_measure,
            R.string.csv_value,
            R.string.csv_unit,
            R.string.csv_method
        )
            .map(resources::getString)
        val labels = MeasurementCsvLabels(
            type = { resources.getString(it.labelRes) },
            unit = { resources.getString(it.unitRes(units)) },
            method = { resources.getString(it.labelRes) }
        )
        write(target, csvDocument(header, measurementCsvRows(entries, units, ZoneId.systemDefault(), labels)))
        return entries.size
    }

    // « wt » vide le fichier avant d'écrire : certains fournisseurs gardent sinon la fin d'un contenu plus long.
    private suspend fun write(target: Uri, content: String) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(target, "wt")
            ?: throw FileNotFoundException("Fichier inaccessible : $target")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
    }
}
