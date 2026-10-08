package com.paulfvt.liftapp.core.domain

import com.paulfvt.liftapp.core.model.SetType.DROP
import com.paulfvt.liftapp.core.model.SetType.WARMUP
import com.paulfvt.liftapp.core.model.SetType.WORK
import org.junit.Assert.assertEquals
import org.junit.Test

class SetNumbersTest {
    @Test
    fun `only work sets are numbered, warm-ups and drop sets are skipped`() {
        assertEquals(
            listOf(null, null, 1, 2, null, 3),
            workSetNumbers(listOf(WARMUP, WARMUP, WORK, WORK, DROP, WORK))
        )
    }

    @Test
    fun `no sets gives no numbers`() {
        assertEquals(emptyList<Int?>(), workSetNumbers(emptyList()))
    }
}
