package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.Circle
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class QuadTreeTest {

    @BeforeEach
    fun setUp() {
        QuadTree.clear()
    }

    @Test
    fun testClear() {
        QuadTree.insert(Circle(Vector2(5f, 5f), 1f))
        QuadTree.insert(Circle(Vector2(10f, 10f), 1f))
        QuadTree.clear()
        assertEquals(0, QuadTree.getObjectCount())
    }

    @Test
    fun testInsertAndQuery() {
        val obj = Circle(Vector2(5f, 5f), 1f)
        QuadTree.insert(obj)
        val result = QuadTree.query(Vector2(5f, 5f))
        assertNotNull(result)
        assertEquals(obj, result)
    }

    @Test
    fun testQueryEmptyTree() {
        val result = QuadTree.query(Vector2(5f, 5f))
        assertNull(result)
    }

    @Test
    fun testRemove() {
        val obj = Circle(Vector2(5f, 5f), 1f)
        QuadTree.insert(obj)
        QuadTree.remove(obj)
        QuadTree.insertPendingAdditions()
        val result = QuadTree.query(Vector2(5f, 5f))
        assertNull(result)
    }

    @Test
    fun testInsertMultipleObjects() {
        val obj1 = Circle(Vector2(5f, 5f), 1f)
        val obj2 = Circle(Vector2(10f, 10f), 1f)
        QuadTree.insert(obj1)
        QuadTree.insert(obj2)
        val result1 = QuadTree.query(Vector2(5f, 5f))
        val result2 = QuadTree.query(Vector2(10f, 10f))
        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(obj1, result1)
        assertEquals(obj2, result2)
    }

    @Test
    fun testInsertMultipleObjectsSamePosition() {
        val obj1 = Circle(Vector2(5f, 5f), 1f)
        val obj2 = Circle(Vector2(5f, 5f), 1f)
        QuadTree.insert(obj1)
        QuadTree.insert(obj2)
        val result1 = QuadTree.query(Vector2(5f, 5f))
        assertNotNull(result1)
        assertEquals(obj1, result1)
    }

    @Test
    fun testInsertMultipleObjectsSamePositionRemoveOne() {
        val obj1 = Circle(Vector2(5f, 5f), 1f)
        val obj2 = Circle(Vector2(5f, 5f), 1f)
        QuadTree.insert(obj1)
        QuadTree.insert(obj2)
        QuadTree.remove(obj1)
        QuadTree.insertPendingAdditions()
        val result1 = QuadTree.query(Vector2(5f, 5f))
        assertNotNull(result1)
        assertEquals(obj2, result1)
    }

    @Test
    fun testInsertMultipleObjectsSamePositionRemoveAll() {
        val obj1 = Circle(Vector2(5f, 5f), 1f)
        val obj2 = Circle(Vector2(5f, 5f), 1f)
        QuadTree.insert(obj1)
        QuadTree.insert(obj2)
        QuadTree.remove(obj1)
        QuadTree.remove(obj2)
        QuadTree.insertPendingAdditions()
        val result1 = QuadTree.query(Vector2(5f, 5f))
        assertNull(result1)
    }
}