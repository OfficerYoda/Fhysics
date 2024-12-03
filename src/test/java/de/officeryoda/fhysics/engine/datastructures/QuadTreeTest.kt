package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.datastructures.spatial.CenterRect
import de.officeryoda.fhysics.engine.datastructures.spatial.QuadTree
import de.officeryoda.fhysics.engine.datastructures.spatial.QuadTree.QTDebugHelper
import de.officeryoda.fhysics.engine.datastructures.spatial.QuadTree.QTNode
import de.officeryoda.fhysics.engine.datastructures.spatial.QuadTree.QTNodeData
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.FhysicsObject
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
        val obj1 = Circle(Vector2(5f, 5f), 1f)
        val obj2 = Circle(Vector2(10f, 10f), 1f)
        insertObjects(obj1, obj2)
        QuadTree.clear()
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
        QuadTree.remove(obj)
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
        QuadTree.remove(obj1)
        val result1 = QuadTree.query(Vector2(5f, 5f))
        assertNotNull(result1)
        assertEquals(obj2, result1)
    }

    @Test
    fun testInsertionMultipleObjectsSamePositionRemoveAll() {
        val obj1 = Circle(Vector2(5f, 5f), 1f)
        val obj2 = Circle(Vector2(5f, 5f), 1f)
        insertObjects(obj1, obj2)
        QuadTree.remove(obj1)
        QuadTree.remove(obj2)
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

        val objects = listOf(obj1, obj2, obj3, obj4, obj5)
        objects.forEach { it.updateBoundingBox() }

        insertObjects(objects)

        QTDebugHelper.printTree()

        val childrenGen1: Array<QTNode> = QTDebugHelper.getChildren(QTDebugHelper.root)
        val childrenBl: Array<QTNode> = QTDebugHelper.getChildren(childrenGen1[2])

        assertEquals(1, childrenBl[0].count)
        assertEquals(3, childrenBl[1].count)
        assertEquals(3, childrenBl[2].count)
        assertEquals(1, childrenBl[3].count)

        assertEquals(5, QuadTree.getObjectCount())
        assertEquals(2, QTDebugHelper.getCurrentDepth())
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

        val objects = listOf(obj1, obj2, obj3, obj4, obj5)
        objects.forEach { it.updateBoundingBox() }

        insertObjects(objects)

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

        val objects = listOf(obj1, obj2, obj3, obj4, obj5)
        objects.forEach { it.updateBoundingBox() }

        insertObjects(objects)
        objects.forEach { QuadTree.remove(it) }

//        assertTrue(QuadTree.root.isLeaf()) // TODO: Will work when deferred cleanup is implemented
        assertEquals(0, QuadTree.getObjectCount())
    }

    @Test
    fun testAddChildNodeDataToCollection1() {
        val parentNodeData = QTNodeData(
            x = 0, y = 0,
            width = 100, height = 100,
            index = 0, depth = 0
        )
        val collection: MutableList<QTNodeData> = mutableListOf()

        QuadTree.addChildNodeDataToCollection(parentNodeData, collection, 1)

        assertEquals(4, collection.size)
        assertEquals(QTNodeData(CenterRect(25, 75, 50, 50), index = 1, depth = 1), collection[0]) // Top-left
        assertEquals(QTNodeData(CenterRect(75, 75, 50, 50), index = 2, depth = 1), collection[1]) // Top-right
        assertEquals(
            QTNodeData(CenterRect(25, 25, 50, 50), index = 3, depth = 1),
            collection[2]
        ) // Bottom-left
        assertEquals(
            QTNodeData(CenterRect(75, 25, 50, 50), index = 4, depth = 1),
            collection[3]
        ) // Bottom-right
    }

    @Test
    fun testAddChildNodeDataToCollection2() {
        val parentNodeData = QTNodeData(
            x = 50, y = 50,
            width = 25, height = 25,
            index = 0, depth = 0
        )
        val collection: MutableList<QTNodeData> = mutableListOf()

        QuadTree.addChildNodeDataToCollection(parentNodeData, collection, 1)

        assertEquals(4, collection.size)
        assertEquals(QTNodeData(CenterRect(56, 68, 12, 13), index = 1, depth = 1), collection[0]) // Top-left
        assertEquals(QTNodeData(CenterRect(68, 68, 13, 13), index = 2, depth = 1), collection[1]) // Top-right
        assertEquals(
            QTNodeData(CenterRect(56, 56, 12, 12), index = 3, depth = 1),
            collection[2]
        ) // Bottom-left
        assertEquals(
            QTNodeData(CenterRect(68, 56, 13, 12), index = 4, depth = 1),
            collection[3]
        ) // Bottom-right
    }

    @Test
    fun testAddChildNodeDataToCollection3() {
        val parentNodeData = QTNodeData(
            x = 0, y = 0,
            width = 5, height = 5,
            index = 0, depth = 0
        )
        val collection: MutableList<QTNodeData> = mutableListOf()

        QuadTree.addChildNodeDataToCollection(parentNodeData, collection, 1)

        assertEquals(4, collection.size)
        assertEquals(QTNodeData(CenterRect(1, 3, 2, 3), index = 1, depth = 1), collection[0]) // Top-left
        assertEquals(QTNodeData(CenterRect(3, 3, 3, 3), index = 2, depth = 1), collection[1]) // Top-right
        assertEquals(QTNodeData(CenterRect(1, 1, 2, 2), index = 3, depth = 1), collection[2]) // Bottom-left
        assertEquals(QTNodeData(CenterRect(3, 1, 3, 2), index = 4, depth = 1), collection[3]) // Bottom-right
    }

    @Test
    fun testAddChildNodeDataToCollection4() {
        val parentNodeData = QTNodeData(
            CenterRect(
                centerX = 68, centerY = 30,
                width = 13, height = 13
            ),
            index = 0, depth = 0
        )
        val collection: MutableList<QTNodeData> = mutableListOf()

        QuadTree.addChildNodeDataToCollection(parentNodeData, collection, 1)

        assertEquals(4, collection.size)
        assertEquals(QTNodeData(CenterRect(65, 33, 6, 7), index = 1, depth = 1), collection[0]) // Top-left
        assertEquals(QTNodeData(CenterRect(71, 33, 7, 7), index = 2, depth = 1), collection[1]) // Top-right
        assertEquals(QTNodeData(CenterRect(65, 27, 6, 6), index = 3, depth = 1), collection[2]) // Bottom-left
        assertEquals(QTNodeData(CenterRect(71, 27, 7, 6), index = 4, depth = 1), collection[3]) // Bottom-right
    }

    @Test
    fun testAddChildNodeDataToCollection5() {
        val parentNodeData =
            QTNodeData(
                CenterRect(
                    centerX = 48, centerY = 7,
                    width = 4, height = 3
                ),
                index = 52, depth = 0
            )
        val collection: MutableList<QTNodeData> = mutableListOf()

        QuadTree.addChildNodeDataToCollection(parentNodeData, collection, 1)

        assertEquals(4, collection.size)
        assertEquals(QTNodeData(CenterRect(47, 8, 2, 2), index = 1, depth = 1), collection[0]) // Top-left
        assertEquals(QTNodeData(CenterRect(49, 8, 2, 2), index = 2, depth = 1), collection[1]) // Top-right
        assertEquals(QTNodeData(CenterRect(47, 6, 2, 1), index = 3, depth = 1), collection[2]) // Bottom-left
        assertEquals(QTNodeData(CenterRect(49, 6, 2, 1), index = 4, depth = 1), collection[3]) // Bottom-right
    }

    private fun insertObjects(vararg objects: FhysicsObject) {
        objects.forEach { QuadTree.queueInsertion(it) }
        QuadTree.insertPendingAdditions()
    }

    private fun insertObjects(objects: List<FhysicsObject>) {
        objects.forEach { QuadTree.queueInsertion(it) }
        QuadTree.insertPendingAdditions()
    }
}