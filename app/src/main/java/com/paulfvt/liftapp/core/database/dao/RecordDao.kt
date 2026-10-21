package com.paulfvt.liftapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.paulfvt.liftapp.core.database.entity.PersonalRecordEntity
import kotlinx.coroutines.flow.Flow

// Cache des records : il est toujours réécrit en entier pour un exercice, jamais modifié ligne à ligne.
@Dao
interface RecordDao {
    @Query("SELECT * FROM personal_record WHERE exerciseId = :exerciseId")
    fun observeFor(exerciseId: Long): Flow<List<PersonalRecordEntity>>

    @Query("SELECT * FROM personal_record")
    fun observeAll(): Flow<List<PersonalRecordEntity>>

    @Query("DELETE FROM personal_record WHERE exerciseId = :exerciseId")
    suspend fun deleteFor(exerciseId: Long)

    @Insert
    suspend fun insertAll(records: List<PersonalRecordEntity>)
}
