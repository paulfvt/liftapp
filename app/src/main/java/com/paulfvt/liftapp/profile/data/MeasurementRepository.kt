package com.paulfvt.liftapp.profile.data

import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.database.entity.BodyMeasurementEntity
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.profile.domain.BodyMeasurement
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Mesures du corps, les plus récentes d'abord.
@Singleton
class MeasurementRepository @Inject constructor(database: LiftDatabase) {
    private val dao = database.measurementDao()

    fun observeAll(): Flow<List<BodyMeasurement>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observe(type: MeasurementType): Flow<List<BodyMeasurement>> =
        dao.observeOfType(type).map { rows -> rows.map { it.toDomain() } }

    // Chaque saisie crée une nouvelle ligne : une ancienne mesure n'est jamais écrasée.
    suspend fun add(measurement: BodyMeasurement): Long = dao.insert(
        BodyMeasurementEntity(
            type = measurement.type,
            value = measurement.value,
            method = measurement.method,
            measuredAt = measurement.measuredAt
        )
    )

    suspend fun delete(id: Long) = dao.delete(id)
}

private fun BodyMeasurementEntity.toDomain() = BodyMeasurement(id, type, value, method, measuredAt)
