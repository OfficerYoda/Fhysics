package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.datastructures.QuadTree.QTDebugHelper
import de.officeryoda.fhysics.engine.datastructures.QuadTree.QTNode
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class QuadTreeTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initAll() {
            // Make sure BORDER is initialized correctly
            FhysicsCore.border = BoundingBox(0f, 0f, 100f, 100f)
            assert(FhysicsCore.border == BoundingBox(0f, 0f, 100f, 100f))
        }
    }

    @BeforeEach
    fun setUp() {
        QuadTree.clearFlag = true
        QuadTree.processPendingOperations()
    }

    @Test
    fun testClear() {
        val obj1 = Circle(Vector2(5f, 5f), 1f)
        val obj2 = Circle(Vector2(10f, 10f), 1f)
        insertObjects(obj1, obj2)

        QuadTree.clearFlag = true
        QuadTree.processPendingOperations()

        assertEquals(0, QuadTree.getObjectCount())
    }

    @Test
    fun testInsertAndQuery() {
        val obj = Circle(Vector2(5f, 5f), 1f)

        insertObjects(obj)

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

        insertObjects(obj)
        removeObjects(obj)

        val result = QuadTree.query(Vector2(5f, 5f))
        assertNull(result)
    }

    @Test
    fun testInsertionMultipleObjects() {
        val obj1 = Circle(Vector2(5f, 5f), 1f)
        val obj2 = Circle(Vector2(10f, 10f), 1f)

        insertObjects(obj1, obj2)

        val result1 = QuadTree.query(Vector2(5f, 5f))
        val result2 = QuadTree.query(Vector2(10f, 10f))
        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(obj1, result1)
        assertEquals(obj2, result2)
    }

    @Test
    fun testInsertionMultipleObjectsSamePosition() {
        val obj1 = Circle(Vector2(5f, 5f), 1f)
        val obj2 = Circle(Vector2(5f, 5f), 1f)

        insertObjects(obj1, obj2)

        val result1 = QuadTree.query(Vector2(5f, 5f))
        assertNotNull(result1)
        assertEquals(obj1, result1)
    }

    @Test
    fun testInsertionMultipleObjectsSamePositionRemoveOne() {
        val obj1 = Circle(Vector2(5f, 5f), 1f)
        val obj2 = Circle(Vector2(5f, 5f), 1f)

        insertObjects(obj1, obj2)
        removeObjects(obj1)

        val result1 = QuadTree.query(Vector2(5f, 5f))
        assertNotNull(result1)
        assertEquals(obj2, result1)
    }

    @Test
    fun testInsertionMultipleObjectsSamePositionRemoveAll() {
        val obj1 = Circle(Vector2(5f, 5f), 1f)
        val obj2 = Circle(Vector2(5f, 5f), 1f)

        insertObjects(obj1, obj2)
        removeObjects(obj1, obj2)

        val result1 = QuadTree.query(Vector2(5f, 5f))
        assertNull(result1)
    }

    @Test
    fun testSplitNode() {
        QuadTree.capacity = 4
        val obj1 = Circle(Vector2(5f, 5f), 1f)
        val obj2 = Circle(Vector2(15f, 15f), 1f)
        val obj3 = Circle(Vector2(25f, 25f), 1f)
        val obj4 = Circle(Vector2(35f, 35f), 1f)
        val obj5 = Circle(Vector2(45f, 45f), 1f)

        insertObjects(obj1, obj2, obj3, obj4, obj5)

        QTDebugHelper.printTree()
        val childrenGen1: Array<QTNode> = QTDebugHelper.getChildren(QTDebugHelper.root)
        val childrenBl: Array<QTNode> = QTDebugHelper.getChildren(childrenGen1[2])
        assertEquals(1, childrenBl[0].objects.size)
        assertEquals(3, childrenBl[1].objects.size)
        assertEquals(3, childrenBl[2].objects.size)
        assertEquals(1, childrenBl[3].objects.size)
        assertEquals(5, QuadTree.getObjectCount())
        assertTrue(!QTDebugHelper.root.isLeaf)
    }

    @Test
    fun testSplitNodeQuery() {
        QuadTree.capacity = 4
        val obj1 = Circle(Vector2(5f, 5f), 1f)
        val obj2 = Circle(Vector2(15f, 15f), 1f)
        val obj3 = Circle(Vector2(25f, 25f), 1f)
        val obj4 = Circle(Vector2(35f, 35f), 1f)
        val obj5 = Circle(Vector2(45f, 45f), 1f)

        insertObjects(obj1, obj2, obj3, obj4, obj5)

        val result1 = QuadTree.query(Vector2(5f, 5f))
        val result2 = QuadTree.query(Vector2(15f, 15f))
        val result3 = QuadTree.query(Vector2(25f, 25f))
        val result4 = QuadTree.query(Vector2(35f, 35f))
        val result5 = QuadTree.query(Vector2(45f, 45f))
        assertNotNull(result1)
        assertNotNull(result2)
        assertNotNull(result3)
        assertNotNull(result4)
        assertNotNull(result5)
        assertEquals(obj1, result1)
        assertEquals(obj2, result2)
        assertEquals(obj3, result3)
        assertEquals(obj4, result4)
        assertEquals(obj5, result5)
    }

    @Test
    fun testSplitNodeRemove() {
        QuadTree.capacity = 4
        val obj1 = Circle(Vector2(5f, 5f), 1f)
        val obj2 = Circle(Vector2(15f, 15f), 1f)
        val obj3 = Circle(Vector2(25f, 25f), 1f)
        val obj4 = Circle(Vector2(35f, 35f), 1f)
        val obj5 = Circle(Vector2(45f, 45f), 1f)

        insertObjects(obj1, obj2, obj3, obj4, obj5)
        removeObjects(obj1, obj2, obj3, obj4, obj5)
        QuadTree.rebuild()

        assertTrue(QTDebugHelper.root.isLeaf)
        assertEquals(0, QuadTree.getObjectCount())
    }

    @Test
    fun testCleanup() {
        QuadTree.capacity = 4
        val obj1 = Circle(Vector2(15f, 15f), 1f)
        val obj2 = Circle(Vector2(25f, 25f), 1f)
        val obj3 = Circle(Vector2(35f, 35f), 1f)
        val obj4 = Circle(Vector2(45f, 45f), 1f)
        val obj5 = Circle(Vector2(55f, 55f), 1f)

        insertObjects(obj1, obj2, obj3, obj4, obj5)
        removeObjects(obj1, obj2, obj3, obj4, obj5)
        QuadTree.rebuild()

        val rootNode = QTDebugHelper.root
        assertTrue(rootNode.isLeaf)
        assertEquals(0, rootNode.objects.size)
    }

    private fun insertObjects(vararg objects: FhysicsObject) {
        insertObjects(objects.toList())
    }

    private fun insertObjects(objects: List<FhysicsObject>) {
        objects.forEach {
            it.updateBoundingBox()
            QuadTree.insert(it)
        }
        QuadTree.processPendingOperations()
    }

    private fun removeObjects(vararg objects: FhysicsObject) {
        removeObjects(objects.toList())
    }

    private fun removeObjects(objects: List<FhysicsObject>) {
        objects.forEach { QuadTree.remove(it) }
        QuadTree.processPendingOperations()
    }
}