package com.paulfvt.liftapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.paulfvt.liftapp.core.database.entity.ProgressPhotoEntity
import kotlinx.coroutines.flow.Flow

// Photos de progression : la ligne décrit la photo, le fichier lui-même est dans le dossier privé photos/.
@Dao
interface PhotoDao {
    @Query("SELECT * FROM progress_photo ORDER BY takenAt DESC")
    fun observeAll(): Flow<List<ProgressPhotoEntity>>

    @Query("SELECT * FROM progress_photo WHERE id = :id")
    fun observe(id: Long): Flow<ProgressPhotoEntity?>

    @Insert
    suspend fun insert(photo: ProgressPhotoEntity): Long

    @Query("DELETE FROM progress_photo WHERE id = :id")
    suspend fun delete(id: Long)
}
