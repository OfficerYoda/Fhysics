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
    // Capacity of the tree
    var capacity: Int = 8
        set(value) {
            field = value.coerceAtLeast(1)
        }

    // The max depth of the QuadTree
    // The formula calculates the amount of divisions before the size falls under 0
    private val MAX_DEPTH = log2(min(FhysicsCore.BORDER.width, FhysicsCore.BORDER.height)).floorToInt()

    // The root node of the tree
    var root: QTNode = QTNode(FhysicsCore.BORDER)
        private set

    private var rootData: QTNodeData = QTNodeData(0, boundingBoxToIntArray(FhysicsCore.BORDER), 0)

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
        val overlappingLeaves: MutableList<QTNodeData> = findOverlappingLeaves(obj.boundingBox)
        for (leave: QTNodeData in overlappingLeaves) {
            insertIntoLeaf(objIdx, leave)
        }
    }

    private fun findOverlappingLeaves(bbox: BoundingBox): MutableList<QTNodeData> {
        val leaves: MutableList<QTNodeData> = mutableListOf() // List to store leaf nodes
        val toProcess: ArrayDeque<QTNodeData> = ArrayDeque() // Stack to manage nodes to process
        toProcess.add(rootData) // Add the root node to the stack

        // Bounding box edges
        val bl: Int = bbox.x.floorToInt() // Left edge
        val bb: Int = bbox.y.floorToInt() // Bottom edge
        val br: Int = (bbox.x + bbox.width).ceilToInt() // Right edge
        val bt: Int = (bbox.y + bbox.height).ceilToInt() // Top edge

        while (toProcess.isNotEmpty()) {
            // Pop the last node from the stack
            val node: QTNodeData = toProcess.removeFirst() // Node data

            // If this node is a leaf, add it to the leaves list
            if (nodes[node.index].isLeaf) {
                leaves.add(node)
                continue
            }

            // Extract the current node's center and half-size
            val cx: Int = node.crect[0] // Center X
            val cy: Int = node.crect[1] // Center Y
            val hw: Int = node.crect[2] / 2 // Half width
            val hh: Int = node.crect[3] / 2 // Half height

            // Get the index of the first child
            val childIndex: Int = nodes[node.index].firstIdx

            // Check which children overlap the bounding box
            if (bl <= cx) { // Left edge intersects or is left of the center
                if (bt >= cy) { // Top edge intersects or is above the center
                    toProcess.add(QTNodeData(cx - hw, cy + 0, hw, hh, childIndex + 0, node.depth + 1)) // Top-left
                }
                if (bb <= cy) { // Bottom edge intersects or is below the center
                    toProcess.add(QTNodeData(cx - hw, cy - hh, hw, hh, childIndex + 2, node.depth + 1)) // Bottom-left
                }
            }
            if (br >= cx) { // Right edge intersects or is right of the center
                if (bt >= cy) { // Top edge intersects or is above the center
                    toProcess.add(QTNodeData(cx + 0, cy + 0, hw, hh, childIndex + 1, node.depth + 1)) // Top-right
                }
                if (bb <= cy) { // Bottom edge intersects or is below the center
                    toProcess.add(QTNodeData(cx + 0, cy - hh, hw, hh, childIndex + 4, node.depth + 1)) // Bottom-right
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

    private fun insertIntoLeaf(objIdx: Int, nodeData: QTNodeData) {
        // Get the node
        val node: QTNode = nodes[nodeData.index]
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

    private fun getLastElement(node: QTNode): QTNodeElement {
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
        val queue: ArrayDeque<QTNode> = ArrayDeque()
        queue.add(root)

        removeIterative(queue, objIdx, obj)
    }

    private fun removeIterative(
        queue: ArrayDeque<QTNode>,
        objIdx: Int,
        obj: FhysicsObject,
    ) {
        while (queue.isNotEmpty()) {
            val node: QTNode = queue.removeFirst()

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

    private fun removeFromLeaf(node: QTNode, objIdx: Int) {
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
        val leaf: QTNode = getLeafNode(pos)
        val objectHit: FhysicsObject? = queryLeafObjects(leaf, pos)
        return objectHit
    }

    private fun getLeafNode(pos: Vector2): QTNode {
        // Traverse the tree until the leaf node containing the position is found
        var nodeData: QTNodeData = rootData
        while (true) {
            val node: QTNode = nodes[nodeData.index]
            if (node.isLeaf) return node

            val cx: Int = nodeData.crect[0] // Center X
            val cy: Int = nodeData.crect[1] // Center Y
            val hw: Int = nodeData.crect[2] / 2 // Half width
            val hh: Int = nodeData.crect[3] / 2 // Half height

            // Check which child node contains the position (favouring the top-left node)
            nodeData =
                if (pos.x <= cx) { // Left side
                    if (pos.y >= cy) { // Top side
                        QTNodeData(cx - hw, cy + 0, hw, hh, node.firstIdx + 0, nodeData.depth + 1) // Top-left
                    } else {
                        QTNodeData(cx - hw, cy - hh, hw, hh, node.firstIdx + 2, nodeData.depth + 1) // Bottom-left
                    }
                } else { // Right side
                    if (pos.y >= cy) { // Top side
                        QTNodeData(cx + 0, cy + 0, hw, hh, node.firstIdx + 1, nodeData.depth + 1) // Top-right
                    } else {
                        QTNodeData(cx + 0, cy - hh, hw, hh, node.firstIdx + 3, nodeData.depth + 1) // Bottom-right
                    }
                }
        }
    }

    private fun queryLeafObjects(
        node: QTNode,
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
        val parent: QTNode = nodes[nodeData.index]
        val firstElementIndex: Int = parent.firstIdx
        val childNodes: Array<QTNodeData> = createChildNodes(nodeData)
        moveElementsToChildren(firstElementIndex, childNodes)
        convertToBranch(parent, childNodes.first())
    }

    private fun createChildNodes(parent: QTNodeData): Array<QTNodeData> {
        val cx: Int = parent.crect[0] // Center X
        val cy: Int = parent.crect[1] // Center Y
        val hw: Int = parent.crect[2] / 2 // Half width
        val hh: Int = parent.crect[3] / 2 // Half height

        // Calculate the boundaries of the parents' bounds
        val pl: Int = cx - hw // Parent left
        val pb: Int = cy - hh // Parent bottom

        val qtNodes: MutableList<QTNode> = mutableListOf()
        qtNodes.add(QTNode(BoundingBox(pl.toFloat(), cy.toFloat(), hw.toFloat(), hh.toFloat()))) // Top left
        qtNodes.add(QTNode(BoundingBox(cx.toFloat(), cy.toFloat(), hw.toFloat(), hh.toFloat()))) // Top right
        qtNodes.add(QTNode(BoundingBox(pl.toFloat(), pb.toFloat(), hw.toFloat(), hh.toFloat()))) // Bottom left
        qtNodes.add(QTNode(BoundingBox(cx.toFloat(), pb.toFloat(), hw.toFloat(), hh.toFloat()))) // Bottom right


        val firstNodeIndex: Int = nodes.add(qtNodes[0])
        nodes.add(qtNodes[1])
        nodes.add(qtNodes[2])
        nodes.add(qtNodes[3])

        val qtNodeData: MutableList<QTNodeData> = mutableListOf()
        qtNodeData.add(QTNodeData(pl, cy, hw, hh, firstNodeIndex + 0, parent.depth + 1)) // Top-left child
        qtNodeData.add(QTNodeData(cx, cy, hw, hh, firstNodeIndex + 1, parent.depth + 1)) // Top-right child
        qtNodeData.add(QTNodeData(pl, pb, hw, hh, firstNodeIndex + 2, parent.depth + 1)) // Bottom-left child
        qtNodeData.add(QTNodeData(cx, pb, hw, hh, firstNodeIndex + 3, parent.depth + 1)) // Bottom-right child

        return qtNodeData.toTypedArray()
    }

    private fun moveElementsToChildren(
        firstElementIndex: Int,
        children: Array<QTNodeData>,
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
            val node: QTNode = nodes[it.index]
            if (node.boundingBox.overlaps(obj.boundingBox)) {
                insertIntoLeaf(objIdx, it) // TODO
            }
        }
    }

    private fun convertToBranch(
        node: QTNode,
        firstChildData: QTNodeData,
    ) {
        // Set the count to -1 to indicate that the node is a branch
        node.count = -1
        // Set the first index to the first child node
        node.firstIdx = firstChildData.index

        // Remove the elements from the parent node
        for (i: Int in 3 downTo 0) {
            // Remove elements in reverse order to maintain correct indices
            elements.remove(node.firstIdx + i)
        }
    }
    /// endregion

    private fun addOverlappingNodesToQueue(
        node: QTNode,
        obj: FhysicsObject,
        queue: ArrayDeque<QTNode>,
    ) {
        // Find the child nodes that overlap with the object...
        val index: Int = node.firstIdx
        for (i: Int in 0 until 4) {
            val child: QTNode = nodes[index + i]
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
        root = QTNode(FhysicsCore.BORDER)
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
