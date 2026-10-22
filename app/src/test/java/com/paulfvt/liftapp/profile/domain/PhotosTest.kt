package com.paulfvt.liftapp.profile.domain

import com.paulfvt.liftapp.core.model.PhotoPose
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotosTest {
    private val paris = ZoneId.of("Europe/Paris")

    private fun photo(id: Long, pose: PhotoPose, at: String) = ProgressPhoto(id, "$id.jpg", pose, Instant.parse(at))

    @Test
    fun `sample size keeps the longest side at least the maximum`() {
        assertEquals(1, sampleSizeFor(1080, 1920))
        assertEquals(1, sampleSizeFor(4000, 3000))
        assertEquals(2, sampleSizeFor(3000, 4096))
        assertEquals(4, sampleSizeFor(8192, 6144))
    }

    @Test
    fun `exif date is read in the phone time zone`() {
        assertEquals(Instant.parse("2026-10-21T16:30:05Z"), parseExifDate("2026:10:21 18:30:05", paris))
    }

    @Test
    fun `a missing or broken exif date gives nothing`() {
        assertNull(parseExifDate(null, paris))
        assertNull(parseExifDate("  ", paris))
        assertNull(parseExifDate("0000:00:00 00:00:00", paris))
        assertNull(parseExifDate("21/10/2026", paris))
    }

    @Test
    fun `the ghost is the latest photo of the same pose`() {
        val photos = listOf(
            photo(1, PhotoPose.FRONT, "2026-09-01T07:00:00Z"),
            photo(2, PhotoPose.FRONT, "2026-10-01T07:00:00Z"),
            photo(3, PhotoPose.SIDE, "2026-10-15T07:00:00Z")
        )
        assertEquals(2L, photos.latestOfPose(PhotoPose.FRONT)?.id)
        assertNull(photos.latestOfPose(PhotoPose.BACK))
        assertEquals(listOf(3L), photos.ofPose(PhotoPose.SIDE).map { it.id })
        assertEquals(photos, photos.ofPose(null))
    }

    @Test
    fun `an imported photo keeps its own time unless its day is changed`() {
        val fromImage = Instant.parse("2026-10-18T16:45:00Z")
        val now = Instant.parse("2026-10-21T09:00:00Z")
        assertEquals(fromImage, photoTakenAt(LocalDate.parse("2026-10-18"), fromImage, now, paris))
        assertEquals(
            Instant.parse("2026-10-17T10:00:00Z"),
            photoTakenAt(LocalDate.parse("2026-10-17"), fromImage, now, paris)
        )
        assertEquals(now, photoTakenAt(LocalDate.parse("2026-10-21"), null, now, paris))
    }
}
