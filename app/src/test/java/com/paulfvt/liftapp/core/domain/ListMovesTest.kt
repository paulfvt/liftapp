package com.paulfvt.liftapp.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ListMovesTest {
    private val list = listOf("A", "B", "C", "D")

    @Test
    fun `moving down shifts the items in between up`() {
        assertEquals(listOf("B", "C", "A", "D"), list.moved(0, 2))
    }

    @Test
    fun `moving up shifts the items in between down`() {
        assertEquals(listOf("D", "A", "B", "C"), list.moved(3, 0))
    }

    @Test
    fun `moving an item onto itself changes nothing`() {
        assertEquals(list, list.moved(1, 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an index outside the list is rejected`() {
        list.moved(0, 4)
    }

    @Test
    fun `an item is inserted right after its anchor`() {
        assertEquals(listOf("A", "B", "X", "C", "D"), list.insertedAfter("B", "X"))
    }

    @Test
    fun `an item is inserted after the last element`() {
        assertEquals(listOf("A", "B", "C", "D", "X"), list.insertedAfter("D", "X"))
    }

    @Test
    fun `a missing anchor puts the item at the end`() {
        assertEquals(listOf("A", "B", "C", "D", "X"), list.insertedAfter("Z", "X"))
    }
}
