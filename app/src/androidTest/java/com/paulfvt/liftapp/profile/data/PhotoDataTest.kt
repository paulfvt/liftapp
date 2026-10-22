package com.paulfvt.liftapp.profile.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.model.PhotoPose
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: LiftDatabase
    private lateinit var photos: PhotoRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, LiftDatabase::class.java).build()
        photos = PhotoRepository(database, PhotoStorage(context))
    }

    @After
    fun tearDown() {
        database.close()
    }

    // Image couchée de 3000 x 2000, notée « à tourner de 90° » comme le font les appareils photo.
    private fun sourceImage(): Uri {
        val file = File(context.cacheDir, "source.jpg")
        FileOutputStream(file).use {
            Bitmap.createBitmap(3000, 2000, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2026:10:18 08:05:00")
            saveAttributes()
        }
        return Uri.fromFile(file)
    }

    @Test
    fun anImportedPhotoIsStoredUprightAndReducedWithItsDate() = runTest {
        val uri = sourceImage()
        assertEquals(Instant.parse("2026-10-18T08:05:00Z"), photos.suggestedDate(uri, ZoneOffset.UTC))

        photos.add(uri, PhotoPose.FRONT, Instant.parse("2026-10-18T08:05:00Z"))

        val photo = photos.observeAll().first().single()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photos.file(photo).absolutePath, bounds)
        // Redressée (portrait) et réduite à 2048 px sur le grand côté.
        assertEquals(2048, bounds.outHeight)
        assertTrue(bounds.outWidth < bounds.outHeight)
    }

    @Test
    fun deletingAPhotoAlsoDeletesItsFile() = runTest {
        photos.add(sourceImage(), PhotoPose.SIDE, Instant.parse("2026-10-18T08:05:00Z"))
        val photo = photos.observeAll().first().single()
        val file = photos.file(photo)
        assertTrue(file.exists())

        photos.delete(photo)

        assertFalse(file.exists())
        assertTrue(photos.observeAll().first().isEmpty())
    }
}
