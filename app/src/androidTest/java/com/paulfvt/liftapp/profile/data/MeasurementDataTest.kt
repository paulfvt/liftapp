package com.paulfvt.liftapp.profile.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.model.BodyFatMethod
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.profile.domain.BodyMeasurement
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeasurementDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: LiftDatabase
    private lateinit var measurements: MeasurementRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, LiftDatabase::class.java).build()
        measurements = MeasurementRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun measure(type: MeasurementType, value: Double, at: String, method: BodyFatMethod? = null) =
        BodyMeasurement(0, type, value, method, Instant.parse(at))

    @Test
    fun eachEntryIsKeptAndTheMostRecentComesFirst() = runTest {
        measurements.add(measure(MeasurementType.BODY_WEIGHT, 81.0, "2026-10-01T06:00:00Z"))
        measurements.add(measure(MeasurementType.BODY_WEIGHT, 80.2, "2026-10-15T06:00:00Z"))
        measurements.add(measure(MeasurementType.BODY_FAT, 17.5, "2026-10-10T06:00:00Z", BodyFatMethod.CALIPER))

        val weights = measurements.observe(MeasurementType.BODY_WEIGHT).first()
        assertEquals(listOf(80.2, 81.0), weights.map { it.value })
        val all = measurements.observeAll().first()
        assertEquals(3, all.size)
        assertEquals(BodyFatMethod.CALIPER, all.single { it.type == MeasurementType.BODY_FAT }.method)
    }

    @Test
    fun deletingRemovesOnlyThatEntry() = runTest {
        val first = measurements.add(measure(MeasurementType.WAIST, 85.0, "2026-10-01T06:00:00Z"))
        measurements.add(measure(MeasurementType.WAIST, 84.0, "2026-10-08T06:00:00Z"))

        measurements.delete(first)

        assertEquals(listOf(84.0), measurements.observe(MeasurementType.WAIST).first().map { it.value })
    }
}
