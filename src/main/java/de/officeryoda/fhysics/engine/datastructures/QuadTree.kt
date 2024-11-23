package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.math.BoundingBox
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
    var root: QuadTreeNode = QuadTreeNode(FhysicsCore.BORDER)
        private set

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
    private val pendingRemovals: MutableList<FhysicsObject> = ArrayList()

    /// region =====Basic QuadTree Operations=====
    /// region =====Insertion=====
    fun insert(obj: FhysicsObject) {
        val objIdx: Int = objects.add(obj)

        // A collection of nodes to process
        val queue: ArrayDeque<QuadTreeNode> = ArrayDeque()
        queue.add(root)

        insertIterative(queue, objIdx, obj)
    }

    private fun insertIterative(
        queue: ArrayDeque<QuadTreeNode>,
        objIdx: Int,
        obj: FhysicsObject,
    ) {
        while (!queue.isEmpty()) {
            // Get the next node to process
            val node: QuadTreeNode = queue.removeFirst()

            // If the node is a leaf, insert the object
            if (node.isLeaf) {
                insertIntoLeaf(node, objIdx)
                // Continue because objects can overlap multiple nodes
                continue
            }

            // Find the child nodes that overlap with the object
            addOverlappingNodesToQueue(node, obj, queue)
        }
    }

    private fun insertIntoLeaf(node: QuadTreeNode, objIdx: Int) {
        // Create a new element
        val element = QTNodeElement(objIdx, -1)

        // If the node is empty, add the element as the first element
        if (node.count == 0) {
            node.firstIdx = elements.add(element)
            node.count = 1
            return
        }

        val nodeElement: QTNodeElement = getLastElement(node)

        // Append the element to the end of the list
        nodeElement.next = elements.add(element)
        node.count++

        // Split the node if it's full
        if (node.count > capacity) {
            splitNode(node)
        }
    }

    private fun getLastElement(node: QuadTreeNode): QTNodeElement {
        // Traverse the element linked list until the last element
        var nodeElement: QTNodeElement = elements[node.firstIdx]
        while (nodeElement.next != -1) {
            nodeElement = elements[nodeElement.next]
        }
        return nodeElement
    }
    /// endregion

    /// region =====Removal=====
    fun remove(obj: FhysicsObject) {
        // Get the index of the object
        val objIdx: Int = objects.indexOf(obj)

        // A collection of nodes to process
        val queue: ArrayDeque<QuadTreeNode> = ArrayDeque()
        queue.add(root)

        removeIterative(queue, objIdx, obj)
    }

    private fun removeIterative(
        queue: ArrayDeque<QuadTreeNode>,
        objIdx: Int,
        obj: FhysicsObject,
    ) {
        while (!queue.isEmpty()) {
            val node: QuadTreeNode = queue.removeFirst()

            // If the node is a leaf, remove the object
            if (node.isLeaf) {
                removeFromLeaf(node, objIdx)
                // Continue because objects can overlap multiple nodes
                continue
            }

            // Add nodes that overlap with the object to the processing queue
            addOverlappingNodesToQueue(node, obj, queue)
        }
    }

    private fun removeFromLeaf(node: QuadTreeNode, objIdx: Int) {
        // Traverse the element linked list
        var current = QTNodeElement(-1, node.firstIdx) // Dummy element
        var previous: QTNodeElement?
        while (current.next != -1) {
            // Get the next element
            previous = current
            current = elements[current.next]

            // Check if the object is the one to remove
            if (current.index == objIdx) {
                // If the object is the first element, update the first element
                if (previous.index == -1) {
                    elements.remove(node.firstIdx)
                    node.firstIdx = current.next
                } else {
                    elements.remove(previous.next)
                    previous.next = current.next
                }

                // Remove the element
                objects.remove(current.index)
                node.count--
                return
            }
        }
    }
    /// endregion

    /// region =====Query=====
    fun query(pos: Vector2): FhysicsObject? {
        val leaf: QuadTreeNode = getLeafNode(pos)
        val objectHit: FhysicsObject? = queryLeafObjects(leaf, pos)
        return objectHit
    }

    private fun getLeafNode(pos: Vector2): QuadTreeNode {
        // Traverse the tree until the leaf node containing the position is found
        var node: QuadTreeNode = root
        while (!node.isLeaf) {
            val index: Int = node.firstIdx
            for (i: Int in 0 until 4) {
                val child: QuadTreeNode = nodes[index + i]
                if (child.boundingBox.contains(pos)) {
                    node = child
                    continue
                }
            }
        }

        return node
    }

    private fun queryLeafObjects(
        node: QuadTreeNode,
        pos: Vector2,
    ): FhysicsObject? {
        if (node.count <= 0) return null

        // Traverse the element linked list
        var nodeElement = QTNodeElement(-1, node.firstIdx) // Dummy element
        while (nodeElement.next != -1) {
            // Get the next element
            nodeElement = elements[nodeElement.next]

            // Get the object from the list
            val obj: FhysicsObject = objects[nodeElement.index]
            // Check if the object contains the position
            if (obj.contains(pos)) {
                return obj
            }
        }

        return null
    }
    /// endregion

    /// region =====Splitting=====
    private fun splitNode(node: QuadTreeNode) {
        val childNodes: Array<QuadTreeNode> = createChildNodes(node)
        moveElementsToChildren(node.firstIdx, childNodes)
        convertToBranch(node)
        addChildNodesToNodesList(node, childNodes)
    }

    private fun createChildNodes(node: QuadTreeNode): Array<QuadTreeNode> {
        // Create the child bounding boxes
        val x: Float = node.boundingBox.x
        val y: Float = node.boundingBox.y
        val hw: Float = node.boundingBox.width / 2
        val hh: Float = node.boundingBox.height / 2

        val topLeft = BoundingBox(x, y, hw, hh)
        val topRight = BoundingBox(x + hw, y, hw, hh)
        val bottomLeft = BoundingBox(x, y + hh, hw, hh)
        val bottomRight = BoundingBox(x + hw, y + hh, hw, hh)

        // Create the child nodes
        val topLeftNode = QuadTreeNode(topLeft)
        val topRightNode = QuadTreeNode(topRight)
        val bottomLeftNode = QuadTreeNode(bottomLeft)
        val bottomRightNode = QuadTreeNode(bottomRight)

        return arrayOf(topLeftNode, topRightNode, bottomLeftNode, bottomRightNode)
    }

    private fun moveElementsToChildren(
        firstIndex: Int,
        children: Array<QuadTreeNode>
    ) {
        // Traverse the element linked list
        var current = QTNodeElement(-1, firstIndex) // Dummy element
        while (current.next != -1) {
            // Get the next element
            current = elements[current.next]
            // Insert the object into the child nodes
            insertIntoChildren(children, current.index)
        }
    }

    private fun insertIntoChildren(
        children: Array<QuadTreeNode>,
        objIdx: Int,
    ) {
        // Get the object from the list
        val obj: FhysicsObject = objects[objIdx]
        // Insert the object into the child nodes
        for (it: QuadTreeNode in children) {
            if (it.boundingBox.overlaps(obj.boundingBox)) {
                insertIntoLeaf(it, objIdx)
            }
        }
    }

    private fun addChildNodesToNodesList(
        parent: QuadTreeNode,
        children: Array<QuadTreeNode>
    ) {
        // Add the child nodes to the list
        parent.firstIdx = nodes.add(children[0])
        nodes.add(children[1])
        nodes.add(children[2])
        nodes.add(children[3])
    }

    private fun convertToBranch(node: QuadTreeNode) {
        // Set the count to -1 to indicate that the node is a branch
        node.count = -1

        // Remove the elements from the parent node
        for (i: Int in 3 downTo 0) {
            // Remove elements in reverse order to maintain correct indices
            elements.remove(node.firstIdx + i)
        }
    }
    /// endregion

    private fun addOverlappingNodesToQueue(
        node: QuadTreeNode,
        obj: FhysicsObject,
        queue: ArrayDeque<QuadTreeNode>,
    ) {
        // Find the child nodes that overlap with the object...
        val index: Int = node.firstIdx
        for (i: Int in 0 until 4) {
            val child: QuadTreeNode = nodes[index + i]
            if (child.boundingBox.overlaps(obj.boundingBox)) {
                // ...and add them to the processing queue
                queue.add(child)
            }
        }
    }
    /// endregion

    /// region
    fun insertPendingAdditions() {
        for (it: FhysicsObject in pendingAdditions) {
            insert(it)
        }
        pendingAdditions.clear()
    }

    fun rebuild() {
        // TODO
    }

    fun clear() {
        root = QuadTreeNode(FhysicsCore.BORDER)
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
        return objects.size()
    }

    fun getPendingRemovals(): MutableList<FhysicsObject> {
        return pendingRemovals
    }

    fun shutdownThreadPool() {
        println("Shutting down thread pool")
//        threadPool.shutdownNow() // TODO
    }

    override fun toString(): String {
        return root.toString()
    }
    /// endregion
}