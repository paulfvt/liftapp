package com.paulfvt.liftapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.paulfvt.liftapp.core.database.entity.BodyMeasurementEntity
import com.paulfvt.liftapp.core.model.MeasurementType
import kotlinx.coroutines.flow.Flow

// Mesures du corps : on ajoute ou on supprime, on ne modifie jamais une mesure enregistrée.
@Dao
interface MeasurementDao {
    @Query("SELECT * FROM body_measurement ORDER BY measuredAt DESC")
    fun observeAll(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurement WHERE type = :type ORDER BY measuredAt DESC")
    fun observeOfType(type: MeasurementType): Flow<List<BodyMeasurementEntity>>

    @Insert
    suspend fun insert(measurement: BodyMeasurementEntity): Long

    @Query("DELETE FROM body_measurement WHERE id = :id")
    suspend fun delete(id: Long)
}
