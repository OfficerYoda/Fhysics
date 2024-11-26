package de.officeryoda.fhysics.engine.datastructures

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IndexedFreeListTest {

    private lateinit var freeList: IndexedFreeList<Int>

    @BeforeEach
    fun setUp() {
        freeList = IndexedFreeList()
    }

    @Test
    fun testAdd() {
        val index = freeList.add(10)
        assertEquals(0, index)
        assertEquals(10, freeList[0])
    }

    @Test
    fun testFree() {
        val index = freeList.add(20)
        freeList.free(index)
        assertThrows(IndexOutOfBoundsException::class.java) {
            freeList[index]
        }
    }

    @Test
    fun testClear() {
        freeList.add(30)
        freeList.clear()
        assertEquals(0, freeList.capacity())
    }

    @Test
    fun testCapacity() {
        freeList.add(40)
        freeList.add(50)
        assertEquals(2, freeList.capacity())
    }

    @Test
    fun testGet() {
        val index = freeList.add(60)
        assertEquals(60, freeList[index])
    }

    @Test
    fun testAddMultiple() {
        val index1 = freeList.add(10)
        val index2 = freeList.add(20)
        val index3 = freeList.add(30)
        assertEquals(0, index1)
        assertEquals(1, index2)
        assertEquals(2, index3)
        assertEquals(10, freeList[0])
        assertEquals(20, freeList[1])
        assertEquals(30, freeList[2])
    }

    @Test
    fun testFreeAndReuse() {
        val index1 = freeList.add(10)
        val index2 = freeList.add(20)
        freeList.free(index1)
        val index3 = freeList.add(30)
        assertEquals(index1, index3) // Reused the erased index
        assertEquals(30, freeList[index1])
        assertEquals(20, freeList[index2])
    }

    @Test
    fun testFreeNonExistent() {
        assertThrows(IndexOutOfBoundsException::class.java) {
            freeList.free(0)
        }
    }

    @Test
    fun testAddAfterClear() {
        freeList.add(10)
        freeList.clear()
        val index = freeList.add(20)
        assertEquals(0, index)
        assertEquals(20, freeList[0])
    }

    @Test
    fun testCapacityAfterFree() {
        freeList.add(10)
        freeList.add(20)
        freeList.free(0)
        assertEquals(2, freeList.capacity()) // Range should still be 2 even after erasing
    }

    @Test
    fun testAddAndFreeMultiple() {
        val indices = (0..9).map { freeList.add(it) }
        indices.forEach { freeList.free(it) }
        indices.forEach {
            assertThrows(IndexOutOfBoundsException::class.java) {
                freeList[it]
            }
        }
    }

    @Test
    fun testAddAfterFreeBlock() {
        val indices = (0..9).map { freeList.add(it) }
        indices.forEach { freeList.free(it) }
        val newIndex = freeList.add(100)
        assertEquals(indices[indices.size - 1], newIndex) // Should reuse the last erased index
        assertEquals(100, freeList[newIndex])
    }
}