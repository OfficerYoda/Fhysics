package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer

object QuadTree {
    private class QTNodeElement(
        /**
         * The index of the object in [QuadTree.objects]
         */
        val index: Int,
        /**
         * The index of the element in the node or -1 if it's the last element
         */
        var next: Int,
    )

    // Capacity of the tree
    var capacity: Int = 8
        set(value) {
            field = value.coerceAtLeast(1)
        }

    // The root node of the tree
    private var root: QuadTreeNode = QuadTreeNode(FhysicsCore.BORDER, null)

    // List of all nodes in the tree
    private val nodes = IndexedFreeList<QuadTreeNode>()

    // List of all elements in the tree (elements store the index of the object in QuadTree.objects)
    // This is done, so that objects are only stored once in the tree and can be referenced multiple times by different elements
    private val elements = IndexedFreeList<QTNodeElement>()

    // List of all objects in the tree
    private val objects = IndexedFreeList<FhysicsObject>()

    // List of objects to add, used to queue up insertions and prevent concurrent modification
    private val pendingAdditions: MutableList<FhysicsObject> = ArrayList()

    // Set of objects to remove, used to mark objects for deletion safely
    private val pendingRemovals: MutableSet<FhysicsObject> = HashSet()

    /// region =====Basic QuadTree Operations=====
    fun query(pos: Vector2): FhysicsObject? {
        val leaf: QuadTreeNode = getLeafNode(pos)
        val objectHit: FhysicsObject? = queryLeafObjects(leaf, pos)
        return objectHit
    }

    private fun getLeafNode(pos: Vector2): QuadTreeNode {
        var node: QuadTreeNode = root
        // Traverse the tree until a leaf node is found
        while (!node.isLeaf()) {
            val index: Int = node.firstIdx
            for (i: Int in 0 until 4) {
                val child: QuadTreeNode = nodes[index + i]
                if (child.boundary.contains(pos)) {
                    node = child
                    break
                }
            }
        }
        return node
    }

    // Returns the index of the object or -1
    private fun queryLeafObjects(
        node: QuadTreeNode,
        pos: Vector2
    ): FhysicsObject? {
        if (node.count <= 0) return null

        // Traverse the element linked list
        var element: QTNodeElement = elements[node.firstIdx]
        while (true) {
            // Get the object from the list
            val obj: FhysicsObject = objects[element.index]
            // Check if the object is at the position
            if (obj.contains(pos)) {
                return obj
            }

            // No more elements in the leaf
            if (element.next == -1) {
                break
            }

            // Get the next element
            element = elements[element.next]
        }

        return null
    }

    fun insert(obj: FhysicsObject) {
        // A collection of nodes to process
        val queue: ArrayDeque<QuadTreeNode> = ArrayDeque()
        queue.add(root)

        while (!queue.isEmpty()) {
            val node: QuadTreeNode = queue.removeFirst()

            // If the node is a leaf, insert the object
            if (node.isLeaf()) {
                insertIntoLeaf(node, obj)
                return
            }

            // Find the child nodes that overlap with the object
            val index: Int = node.firstIdx
            for (i: Int in 0 until 4) {
                val child: QuadTreeNode = nodes[index + i]
                if (child.boundary.overlaps(obj.boundingBox)) {
                    queue.add(child)
                }
            }
        }
    }

    private fun insertIntoLeaf(node: QuadTreeNode, obj: FhysicsObject) {
        // Create a new element
        val element = QTNodeElement(objects.add(obj), -1)

        // If the node is empty, add the element as the first element
        if (node.count == 0) {
            node.firstIdx = elements.add(element)
            node.count = 1
            return
        }

        // Traverse the element linked list
        var current: QTNodeElement = elements[node.firstIdx]
        while (true) {
            // If the next element is the last element, add the new element
            if (current.next == -1) {
                current.next = elements.add(element)
                node.count++
                return
            }

            // Get the next element
            current = elements[current.next]
        }
    }

    fun remove(obj: FhysicsObject) {
        // A collection of nodes to process
        val queue: ArrayDeque<QuadTreeNode> = ArrayDeque()
        queue.add(root)

        while (!queue.isEmpty()) {
            val node: QuadTreeNode = queue.removeFirst()

            // If the node is a leaf, insert the object
            if (node.isLeaf()) {
                removeFromLeaf(node, obj)
                return
            }

            // Find the child nodes that overlap with the object
            val index: Int = node.firstIdx
            for (i: Int in 0 until 4) {
                val child: QuadTreeNode = nodes[index + i]
                if (child.boundary.overlaps(obj.boundingBox)) {
                    queue.add(child)
                }
            }
        }
    }

    private fun removeFromLeaf(node: QuadTreeNode, obj: FhysicsObject) {
        // Traverse the element linked list
        var current: QTNodeElement = elements[node.firstIdx]
        var previous: QTNodeElement? = null
        while (true) {
            // Get the object from the list
            val currentObj: FhysicsObject = objects[current.index]
            // Check if the object is the one to remove
            if (currentObj == obj) {
                // If the object is the first element, update the first element
                if (previous == null) {
                    elements.remove(node.firstIdx)
                    node.firstIdx = current.next
                } else {
                    elements.remove(previous.index)
                    previous.next = current.next
                }

                // Remove the element
                objects.remove(current.index)
                node.count--
                return
            }

            // No more elements in the leaf
            if (current.next == -1) {
                break
            }

            // Get the next element
            previous = current
            current = elements[current.next]
        }
    }
    /// endregion

    fun insertPendingAdditions() {
        pendingAdditions.forEach { insert(it) }
        pendingAdditions.clear()
    }

    fun rebuild() {
        // TODO
    }

    fun clear() {
        root = QuadTreeNode(FhysicsCore.BORDER, null)
        nodes.clear()
        elements.clear()
        objects.clear()
    }

    fun updateFhysicsObjects() {
        // TODO
    }

    fun handleCollisions() {
        // TODO
    }

    fun drawObjects(drawer: FhysicsObjectDrawer) {
        // TODO
    }

    fun drawNodes() {
        // TODO
    }

    fun updateNodeSizes() {
        // TODO
    }

    fun getObjectCount(): Int {
        return objects.capacity()
    }

    fun getPendingRemovals(): MutableSet<FhysicsObject> {
        return pendingRemovals
    }

    fun shutdownThreadPool() {
        println("Shutting down thread pool")
//        threadPool.shutdownNow() // TODO
    }

    override fun toString(): String {
        return root.toString()
    }
}