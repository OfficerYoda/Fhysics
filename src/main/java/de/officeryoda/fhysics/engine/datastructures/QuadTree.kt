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

    // TODO group the main methods and the called methods in them so that main methods are at the top
    // TODO make methods shorter and more readable

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
                if (child.boundingBox.contains(pos)) {
                    node = child
                    break
                }
            }
        }
        return node
    }

    // Returns the index of the object or -1
    private fun queryLeafObjects(node: QuadTreeNode, pos: Vector2): FhysicsObject? {
        if (node.count <= 0) return null

        // Traverse the element linked list
        var nodeElement = QTNodeElement(-1, node.firstIdx) // Dummy element
        while (nodeElement.next != -1) {
            // Get the next element
            nodeElement = elements[nodeElement.next]

            // Get the object from the list
            val obj: FhysicsObject = objects[nodeElement.index]
            // Check if the object is at the position
            if (obj.contains(pos)) {
                return obj
            }
        }

        return null
    }

    fun insert(obj: FhysicsObject) {
        // Add the object to the list
        val objIdx: Int = objects.add(obj)

        // A collection of nodes to process
        val queue: ArrayDeque<QuadTreeNode> = ArrayDeque()
        queue.add(root)

        while (!queue.isEmpty()) {
            val node: QuadTreeNode = queue.removeFirst()

            // If the node is a leaf, insert the object
            if (node.isLeaf()) {
                insertIntoLeaf(node, objIdx)
                // Continue because objects can overlap multiple nodes
                continue
            }

            // Find the child nodes that overlap with the object
            val index: Int = node.firstIdx
            for (i: Int in 0 until 4) {
                val child: QuadTreeNode = nodes[index + i]
                // If the child node overlaps with the object, add it to the process queue
                if (child.boundingBox.overlaps(obj.boundingBox)) {
                    queue.add(child)
                }
            }
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

        // Traverse the element linked list until the end
        var nodeElement: QTNodeElement = elements[node.firstIdx]
        while (nodeElement.next != -1) {
            nodeElement = elements[nodeElement.next]
        }

        // Append the element to the end of the list
        nodeElement.next = elements.add(element)
        node.count++

        // Split the node if it's full
        if (node.count > capacity) {
            splitNode(node)
        }
    }

    private fun splitNode(node: QuadTreeNode) {
        val childNodes: Array<QuadTreeNode> = createChildNodes(node)
        moveElementsToChildren(node, childNodes)
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

    private fun addChildNodesToNodesList(parent: QuadTreeNode, children: Array<QuadTreeNode>) {
        // Add the child nodes to the list
        parent.firstIdx = nodes.add(children[0])
        nodes.add(children[1])
        nodes.add(children[2])
        nodes.add(children[3])
    }

    private fun moveElementsToChildren(parent: QuadTreeNode, children: Array<QuadTreeNode>) {
        // Traverse the element linked list
        var current = QTNodeElement(-1, parent.firstIdx) // Dummy element
        while (current.next != -1) {
            // Get the next element
            current = elements[current.next]
            // Get the object from the list
            val obj: FhysicsObject = objects[current.index]
            // Insert the object into the child nodes
            children.forEach { insertIntoNewChild(it, obj, current.index) }
        }

        convertToBranch(parent)
    }

    private fun insertIntoNewChild(child: QuadTreeNode, obj: FhysicsObject, objIdx: Int) {
        if (child.boundingBox.overlaps(obj.boundingBox)) {
            insertIntoLeaf(child, objIdx)
        }
    }

    private fun convertToBranch(parent: QuadTreeNode) {
        // Set the count to -1 to indicate that the node is a branch
        parent.count = -1

        // Remove the elements from the parent node
        for (i: Int in 3 downTo 0) {
            // Remove elements in reverse order to maintain correct indices
            elements.remove(parent.firstIdx + i)
        }
    }

    fun remove(obj: FhysicsObject) {
        // Add the object to the list
        val objIdx: Int = objects.indexOf(obj)

        // A collection of nodes to process
        val queue: ArrayDeque<QuadTreeNode> = ArrayDeque()
        queue.add(root)

        while (!queue.isEmpty()) {
            val node: QuadTreeNode = queue.removeFirst()

            // If the node is a leaf, remove the object
            if (node.isLeaf()) {
                removeFromLeaf(node, objIdx)
                // Continue because objects can overlap multiple nodes
                continue
            }

            // Find the child nodes that overlap with the object
            val index: Int = node.firstIdx
            for (i: Int in 0 until 4) {
                val child: QuadTreeNode = nodes[index + i]
                if (obj.boundingBox.overlaps(child.boundingBox)) {
                    queue.add(child)
                }
            }
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

    /// region
    fun insertPendingAdditions() {
        pendingAdditions.forEach { insert(it) }
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