package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.math.BoundingBox
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.extensions.ceilToInt
import de.officeryoda.fhysics.extensions.floorToInt
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import kotlin.math.log2
import kotlin.math.min

object QuadTree {
    /// region =====Data classes=====
    private data class QTNodeElement(
        /** The index of the object in [QuadTree.objects] */
        val index: Int,
        /** The index of the element in the node or -1 if it's the last element */
        var next: Int,
    )

    data class QTNodeData(
        /** Index of the node in the [nodes] list. */
        val index: Int,
        /** Array representing the node's bounding box: [centerX, centerY, width, height]. */
        val crect: IntArray,
        /** Depth of the node in the Quadtree. */
        val depth: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as QTNodeData

            if (index != other.index) return false
            if (!crect.contentEquals(other.crect)) return false
            if (depth != other.depth) return false

            return true
        }

        override fun hashCode(): Int {
            var result = index
            result = 31 * result + crect.contentHashCode()
            result = 31 * result + depth
            return result
        }
    }
    /// endregion

    // Capacity of the tree
    var capacity: Int = 8
        set(value) {
            field = value.coerceAtLeast(1)
        }

    // The max depth of the QuadTree
    // The formula calculates the amount of divisions before the size falls under 0
    private val MAX_DEPTH = log2(min(FhysicsCore.BORDER.width, FhysicsCore.BORDER.height)).floorToInt()

    // The root node of the tree
    var root: QuadTreeNode = QuadTreeNode(FhysicsCore.BORDER)
        private set

    // List of all nodes in the tree (root node is always at index 0)
    private val nodes = IndexedFreeList(root)

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
//        val queue: ArrayDeque<QuadTreeNode> = ArrayDeque()
//        queue.add(root)
//
//        insertIterative(queue, objIdx, obj)

        val overlappingLeaves: MutableList<QTNodeData> = findOverlappingLeaves(obj.boundingBox)
        for (leave: QTNodeData in overlappingLeaves) {
            insertIntoLeaf(objIdx, leave)
        }
    }

    private fun findOverlappingLeaves(bbox: BoundingBox): MutableList<QTNodeData> {
        val leaves: MutableList<QTNodeData> = mutableListOf() // List to store leaf nodes.
        val toProcess: ArrayDeque<QTNodeData> = ArrayDeque() // Stack to manage nodes to process.
        toProcess.add(QTNodeData(0, boundingBoxToIntArray(FhysicsCore.BORDER), 0)) // Add the root node to the stack.

        // Bounding box edges
        val bl: Int = bbox.x.floorToInt() // Left edge
        val bb: Int = bbox.y.floorToInt() // Bottom edge
        val br: Int = (bbox.x + bbox.width).ceilToInt() // Right edge
        val bt: Int = (bbox.y + bbox.height).ceilToInt() // Top edge

        while (toProcess.isNotEmpty()) {
            // Pop the last node from the stack.
            val nd: QTNodeData = toProcess.removeFirst() // Node data

            // If this node is a leaf, add it to the leaves list.
            if (nodes[nd.index].isLeaf) {
                leaves.add(nd)
                continue
            }

            // Extract the current node's center and half-size.
            val cx: Int = nd.crect[0] // Center X
            val cy: Int = nd.crect[1] // Center Y
            val hw: Int = nd.crect[2] / 2 // Half width
            val hh: Int = nd.crect[3] / 2 // Half height

            // Get the index of the first child.
            val childIndex: Int = nodes[nd.index].firstIdx

            // Calculate the boundaries of this node's region.
            val l: Int = cx - hw
            val t: Int = cy - hh
            val r: Int = cx + hw
            val b: Int = cy + hh

            // Check which children intersect the bounding box.
            if (bt >= cy) { // Top edge intersects or is above the center.
                if (bl <= cx) { // Left edge intersects or is left of the center.
                    toProcess.add(toQTNodeData(l, t, hw, hh, childIndex + 0, nd.depth + 1)) // Top-left child.
                }
                if (br > cx) {// Right of rectangle is right of the center.
                    toProcess.add(toQTNodeData(r, t, hw, hh, childIndex + 1, nd.depth + 1)) // Top-right child.
                }
            }
            if (bb < cy) { // Bottom of rectangle intersects or is below the center.
                if (bl <= cx) {// Left of rectangle intersects or is left of the center.
                    toProcess.add(toQTNodeData(l, b, hw, hh, childIndex + 2, nd.depth + 1)) // Bottom-left child.
                }
                if (br > cx) { // Right of rectangle is right of the center.
                    toProcess.add(toQTNodeData(r, b, hw, hh, childIndex + 3, nd.depth + 1)) // Bottom-right child.
                }
            }
        }

        return leaves
    }

    private fun boundingBoxToIntArray(boundingBox: BoundingBox): IntArray {
        val centerX: Float = boundingBox.x + boundingBox.width / 2
        val centerY: Float = boundingBox.y + boundingBox.height / 2
        return intArrayOf(
            centerX.floorToInt(),
            centerY.floorToInt(),
            boundingBox.width.ceilToInt(),
            boundingBox.height.ceilToInt(),
        )
    }

    private fun toQTNodeData(l: Int, t: Int, hw: Int, hh: Int, nodeIndex: Int, depth: Int): QTNodeData {
        return QTNodeData(nodeIndex, intArrayOf(l, t, hw * 2, hh * 2), depth)
    }

    private fun insertIntoLeaf(objIdx: Int, nodeData: QTNodeData) {
        // Get the node
        val node: QuadTreeNode = nodes[nodeData.index]
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
        if (node.count > capacity && nodeData.depth <= MAX_DEPTH) {
            splitNode(nodeData)
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
        while (queue.isNotEmpty()) {
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
    private fun splitNode(nodeData: QTNodeData) {
        TODO()
//        val node: QuadTreeNode = nodes[nodeData.index]
//        val childNodes: Array<QuadTreeNode> = createChildNodes(node)
//        moveElementsToChildren(node.firstIdx, childNodes)
//        convertToBranch(node)
//        addChildNodesToNodesList(node, childNodes)
    }

    private fun createChildNodes(parent: QuadTreeNode): Array<QuadTreeNode> {
        // Create the child bounding boxes
        val x: Float = parent.boundingBox.x
        val y: Float = parent.boundingBox.y
        val hw: Float = parent.boundingBox.width / 2
        val hh: Float = parent.boundingBox.height / 2

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
        firstElementIndex: Int,
        children: Array<QTNodeData>
    ) {
        // Traverse the element linked list
        var current = QTNodeElement(-1, firstElementIndex) // Dummy element
        while (current.next != -1) {
            // Get the next element
            current = elements[current.next]
            // Insert the object into the child nodes
            insertIntoChildren(current.index, children)
        }
    }

    private fun insertIntoChildren(
        objIdx: Int,
        children: Array<QTNodeData>,
    ) {
        // Get the object from the list
        val obj: FhysicsObject = objects[objIdx]
        // Insert the object into the child nodes
        for (it: QTNodeData in children) {
            val node: QuadTreeNode = nodes[it.index]
            if (node.boundingBox.overlaps(obj.boundingBox)) {
                insertIntoLeaf(objIdx, it) // TODO
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
        nodes.add(root)
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
