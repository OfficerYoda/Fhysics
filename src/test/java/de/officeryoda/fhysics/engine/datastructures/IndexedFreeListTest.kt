package de.officeryoda.fhysics.engine.datastructures

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IndexedFreeListTest {

    private lateinit var list: IndexedFreeList<Int>

    @BeforeEach
    fun setUp() {
        list = IndexedFreeList()
    }

    @Test
    fun testAdd() {
        val index = list.add(10)
        assertEquals(0, index)
        assertEquals(10, list[0])
    }

    @Test
    fun testRemove() {
        val index = list.add(20)
        list.remove(index)
        assertThrows(IndexOutOfBoundsException::class.java) {
            list[index]
        }
    }

    @Test
    fun testClear() {
        list.add(30)
        list.clear()
        assertEquals(0, list.capacity())
    }

    @Test
    fun testCapacity() {
        list.add(40)
        list.add(50)
        assertEquals(2, list.capacity())
    }

    @Test
    fun testGet() {
        val index = list.add(60)
        assertEquals(60, list[index])
    }

    @Test
    fun testAddMultiple() {
        val index1 = list.add(10)
        val index2 = list.add(20)
        val index3 = list.add(30)
        assertEquals(0, index1)
        assertEquals(1, index2)
        assertEquals(2, index3)
        assertEquals(10, list[0])
        assertEquals(20, list[1])
        assertEquals(30, list[2])
    }

    @Test
    fun testRemoveAndReuse() {
        val index1 = list.add(10)
        val index2 = list.add(20)
        list.remove(index1)
        val index3 = list.add(30)
        assertEquals(index1, index3) // Reused the erased index
        assertEquals(30, list[index1])
        assertEquals(20, list[index2])
    }

    @Test
    fun testRemoveNonExistent() {
        assertThrows(IndexOutOfBoundsException::class.java) {
            list.remove(0)
        }
    }

    @Test
    fun testAddAfterClear() {
        list.add(10)
        list.clear()
        val index = list.add(20)
        assertEquals(0, index)
        assertEquals(20, list[0])
    }

    @Test
    fun testCapacityAfterRemove() {
        list.add(10)
        list.add(20)
        list.remove(0)
        assertEquals(2, list.capacity()) // Range should still be 2 even after erasing
    }

    @Test
    fun testAddAndRemoveMultiple() {
        val indices = (0..9).map { list.add(it) }
        indices.forEach { list.remove(it) }
        indices.forEach {
            assertThrows(IndexOutOfBoundsException::class.java) {
                list[it]
            }
        }
    }

    @Test
    fun testAddAfterRemoveBlock() {
        val indices = (0..9).map { list.add(it) }
        indices.forEach { list.remove(it) }
        val newIndex = list.add(100)
        assertEquals(indices[0], newIndex) // Should reuse the first erased index
        assertEquals(100, list[newIndex])
    }
}