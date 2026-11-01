package com.paulfvt.liftapp.home.data

import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.model.WorkoutStatus
import com.paulfvt.liftapp.home.domain.LatestRecord
import com.paulfvt.liftapp.home.domain.TemplateSession
import com.paulfvt.liftapp.home.domain.lastDuration
import com.paulfvt.liftapp.home.domain.latestRecord
import com.paulfvt.liftapp.home.domain.nextTemplate
import com.paulfvt.liftapp.home.domain.trainedDays
import com.paulfvt.liftapp.progress.data.MotivationRepository
import com.paulfvt.liftapp.progress.data.WeeklyGoalRepository
import com.paulfvt.liftapp.progress.domain.Flashback
import com.paulfvt.liftapp.progress.domain.WeeklyStreak
import com.paulfvt.liftapp.progress.domain.flashback
import com.paulfvt.liftapp.template.data.TemplateRepository
import com.paulfvt.liftapp.template.domain.TemplateSummary
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

// Ce que montrent l'accueil et le widget hors séance. nextTemplate est null quand il n'y a aucun template.
data class Dashboard(
    val today: LocalDate,
    val streak: WeeklyStreak,
    // Jours de la semaine en cours avec au moins une séance terminée.
    val trainedDays: Set<LocalDate>,
    val nextTemplate: TemplateSummary?,
    // Dossier de la prochaine séance et durée de la dernière fois avec ce template, quand ils existent.
    val nextFolderName: String?,
    val nextLastDuration: Duration?,
    val latestRecord: LatestRecord?,
    val flashback: Flashback?
)

// Tableau de bord, partagé par l'écran Accueil et le widget : une seule façon de le calculer.
@Singleton
class DashboardRepository @Inject constructor(
    database: LiftDatabase,
    private val weeklyGoals: WeeklyGoalRepository,
    private val templates: TemplateRepository,
    private val motivation: MotivationRepository,
    private val clock: Clock
) {
    private val workoutDao = database.workoutDao()

    fun observe(): Flow<Dashboard> = combine(
        weeklyGoals.observeStreak(),
        templates.observeLibrary(),
        workoutDao.observeRecentTemplateIds(WorkoutStatus.FINISHED),
        motivation.observeHistories(),
        combine(
            workoutDao.observeStartTimes(WorkoutStatus.FINISHED),
            workoutDao.observeTemplateSessions(WorkoutStatus.FINISHED)
        ) { starts, sessions -> starts to sessions }
    ) { streak, library, recentTemplateIds, histories, (starts, sessionRows) ->
        val zone = ZoneId.systemDefault()
        val today = clock.instant().atZone(zone).toLocalDate()
        val next = nextTemplate(library, recentTemplateIds)
        val sessions = sessionRows.map { TemplateSession(it.templateId, it.startedAt, it.endedAt) }
        Dashboard(
            today = today,
            streak = streak,
            trainedDays = trainedDays(starts, streak.currentWeek.monday, zone),
            nextTemplate = next,
            nextFolderName = next?.folderId?.let { id -> library.folders.firstOrNull { it.folder.id == id } }
                ?.folder?.name,
            nextLastDuration = next?.let { lastDuration(it.id, sessions) },
            latestRecord = latestRecord(histories),
            flashback = flashback(histories, today, zone)
        )
    }.flowOn(Dispatchers.Default)
}
