package com.paulfvt.liftapp.profile.data

import android.net.Uri
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.database.entity.ProgressPhotoEntity
import com.paulfvt.liftapp.core.model.PhotoPose
import com.paulfvt.liftapp.profile.domain.ProgressPhoto
import java.io.File
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Photos de progression, les plus récentes d'abord. Le fichier et sa ligne en base vont toujours ensemble.
@Singleton
class PhotoRepository @Inject constructor(database: LiftDatabase, private val storage: PhotoStorage) {
    private val dao = database.photoDao()

    fun observeAll(): Flow<List<ProgressPhoto>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observe(id: Long): Flow<ProgressPhoto?> = dao.observe(id).map { it?.toDomain() }

    fun file(photo: ProgressPhoto): File = storage.file(photo.fileName)

    suspend fun suggestedDate(uri: Uri, zone: ZoneId): Instant? = storage.takenAt(uri, zone)

    suspend fun add(uri: Uri, pose: PhotoPose, takenAt: Instant): Long {
        val fileName = storage.copy(uri)
        return dao.insert(ProgressPhotoEntity(filePath = fileName, pose = pose, takenAt = takenAt))
    }

    suspend fun delete(photo: ProgressPhoto) {
        dao.delete(photo.id)
        storage.delete(photo.fileName)
    }
}

private fun ProgressPhotoEntity.toDomain() = ProgressPhoto(id, filePath, pose, takenAt)
